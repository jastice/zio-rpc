package zrpc

import ZIORPC._
import Pickle._
import scala.util.Random
import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._


object ToyData {

  val n: Notification = Notification("2.0", "boop", Some(23.asJson))
  val r: Request = Request("2.0", "feep", Some(74.asJson), "1")
  val sr: SuccessResponse = SuccessResponse("2.0", 99.asJson, "901")
  val er: ErrorResponse = ErrorResponse("2.0", ErrorObject(ErrorCode.ParseError, "you're bad!", None), "901")
  val serialized: List[Json] = List[Message](n,r,sr,er).map(_.asJson)
  val conc: String = serialized.mkString
  def simpleNotification(message: String): Notification = Notification("2.0", "notify", Some(message.asJson))
  def echoRequest(message: String): Request = Request("2.0", "echo", Some(message.asJson), Random.nextLong.toString)
  def echoResponse(message: String, id: String): SuccessResponse = SuccessResponse("2.0", message.asJson, id)
}
