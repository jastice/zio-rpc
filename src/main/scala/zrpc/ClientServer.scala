package zrpc

import java.net.{ServerSocket, Socket}

import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import zio.blocking._
import zio.console._
import zio.{Chunk, ExitCode, RIO, Task, URIO, ZEnv, ZIO}
import zio.stream.{ZStream, ZTransducer}
import zrpc.Pickle._
import zrpc.ToyData._
import zrpc.Util._
import zrpc.ZIORPC._

import scala.util.matching.Regex



object Server extends zio.App {

  def serverStream(handler: Message => ZIO[ZEnv, Throwable, Message],
                   in: ZStream[ZEnv, Throwable, Byte],
                   out: Message => RIO[Blocking, Unit]): ZIO[ZEnv, Throwable, Unit] =
    in
      .aggregate(AgJsonObj)
      .map(bs => new String(bs.toArray).trim)
      .filter(_.nonEmpty)
      .mapM(m => putStrLn(m).as(m))
      .mapM(m => zio.UIO(decode[Message](m))
      .tap {
        case Left(err) => putStrLn("bad message: " + err.getMessage)
        case Right(msg) => ZIO.unit
      })
      .collect { case Right(msg) => msg }
      .mapM(handler)
      // response gets written
      .foreach(x => out(x))
      .tapError(err => putStrLn("error: " + err.getMessage))

  override def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] = {
    println("server started")

val resource = zio.ZManaged.make(
    Task {
      val ss = new ServerSocket(1043)
      val sock = ss.accept()
      println("client connected")
      ss -> sock
    }){case (ss, _) => Task(ss.close()).orDie}

    def in(sock: Socket) = ZStream.fromInputStream(sock.getInputStream)
    def out(msg: Message, sock: Socket) = {
      Task(sock.getOutputStream.write(msg.asJson.toString().getBytes))
    }
    val dummyHandler = (m: Message) => putStrLn(m.toString).as(m)
    
    resource.use { case (_, sock) =>
      serverStream(dummyHandler, in(sock), out(_, sock))
    }.exitCode
     
  }
  
}


object Client extends zio.App {

  override def run(args: List[String]): URIO[Blocking with Console, ExitCode] = {

    val resource = zio.ZManaged.make(Task( new Socket("localhost", 1043))
    .tap(sock => putStrLn("server connected: " + sock.isConnected)))(sock => 
    Task(sock.close).orDie)

    def serverStream(sock: Socket) = 
    ZStream.fromInputStream(sock.getInputStream)
      .aggregate(AgJsonObj)
      .filter(_.nonEmpty)
      .map(c => new String(c.toArray))
      .map(m => decode[Message](m))
      .map(_.toString())
      .foreach(msg => putStrLn(s"<-- $msg"))
    

    def userStream(sock: Socket) = ZStream
      .repeatEffect(putStr("> ").flatMap(_ => getStrLn))
      .map(toRequest)
      .collect { case Some(req) => req }
      .foreach { req =>
        for {
          _ <- putStrLn("--> " + req)
          _ <- Task {
            sock.getOutputStream.write(req.toString.getBytes)
            sock.getOutputStream.flush()
          }
        } yield ()
      }

    resource.use(sock => serverStream(sock) &> userStream(sock))
      .exitCode
  }

  val notifyR: Regex = "notify (.*)".r
  val echoR: Regex = "echo (.*)".r
  val junkR: Regex = "junk (.*)".r

  def toRequest(text: String): Option[Json] = text match {
    case notifyR(msg) => Option(simpleNotification(msg).asJson)
    case echoR(msg) => Option(echoRequest(msg).asJson)
    case junkR(msg) => Option(Map("junk" -> "true", "msg" -> msg).asJson)
    case _ => None
  }

}


object Util {
  val NoBytes: Chunk[Byte] = Chunk[Byte]()
  val AgJsonObj: ZTransducer[Any, Nothing, Byte, Chunk[Byte]] =
    ZTransducer.foldWeighted(NoBytes)((_,i: Byte) => bracecount(i), 0)(_+_)

  def bracecount(b: Byte): Int = b match {
    case '{' => -1
    case '}' => 1
    case _ => 0
  }
}