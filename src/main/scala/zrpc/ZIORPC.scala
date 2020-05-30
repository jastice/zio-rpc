package zrpc

import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.JsonCodec
import cats.syntax.functor._
import io.circe.generic.auto._
import io.circe.syntax._


object ZIORPC {

  sealed abstract class Message

//  @JsonCodec
  case class Notification(jsonrpc: String, method: String, params: Option[Json]) extends Message

//  @JsonCodec
  case class Request(jsonrpc: String, method: String, params: Option[Json], id: String) extends Message

//  @JsonCodec
  case class SuccessResponse(jsonrpc: String, result: Json, id: String) extends Message

//  @JsonCodec
  case class ErrorResponse(jsonrpc: String, error: ErrorObject, id: String) extends Message

//  @JsonCodec
  case class ErrorObject(code: Int, message: String, data: Option[Json])

  implicit val encodeMessage: Encoder[Message] = Encoder.instance {
    case request @ Request(_, _, _, _) => request.asJson
    case notification @ Notification(_,_,_) => notification.asJson
    case successResponse @ SuccessResponse(_,_,_) => successResponse.asJson
    case errorResponse @ ErrorResponse(_,_,_) => errorResponse.asJson
  }

  implicit val decodeMessage: Decoder[Message] = List[Decoder[Message]](
    Decoder[Request].widen,
    Decoder[Notification].widen,
    Decoder[SuccessResponse].widen,
    Decoder[ErrorResponse].widen
  ).reduceLeft(_ or _)

}



object ErrorCode {
  val ParseError: Int = -32700
  val InvalidRequest: Int = -32600
  val MethodNotFound: Int = -32601
  val InvalidParams: Int = -32602
  val InternalError: Int = -32603

  /** Implementation-defined error code. Must be in range [-32900, -32000] */
  def serverError(code: Int): Unit = {
    assert(code <= -32000 && code >= -32900)
  }
}
