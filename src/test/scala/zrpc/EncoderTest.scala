package zrpc

import utest._
import io.circe.generic.auto._, io.circe.syntax._

class EncoderTest extends TestSuite {
  override def tests: Tests = Tests {
    test("encode") {
      val enc = ToyData.n.asJson
      println(enc)
    }
  }
}
