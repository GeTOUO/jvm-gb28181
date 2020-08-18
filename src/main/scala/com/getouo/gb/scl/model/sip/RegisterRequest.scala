package com.getouo.gb.scl.model.sip

trait SipMessage {
  val context: String
}
case class BadSipMessage(context: String) extends SipMessage

case class RegisterRequest(context: String) {
  val contextR =
    s"""
       |REGISTER sip:([0-9]+)@([0-9]+) ([a-zA-Z0-9/.]+)
       |Via: SIP/2.0/UDP 192.168.2.130:5070;rport;branch=z9hG4bK77333116
       |From: <sip:43050000981328000015@4305000098>;tag=2020271230
       |To: <sip:43050000981328000015@4305000098>
       |Call-ID: 1970987503
       |CSeq: 1 REGISTER
       |Contact: <sip:43050000981328000015@192.168.2.130:5070>
       |Max-Forwards: 70
       |User-Agent: IP Camera
       |Expires: 3600
       |Content-Length: 0
       |""".stripMargin.r

  val contextR(serverId, serverRegion, version) = context
}

object Gh {

  def main(args: Array[String]): Unit = {
    val num = s"aaa([a-zA-Z0-9/.]+)sss".r
    val num(ss) = "aaaSIP/2.0sss"
    println(ss)
  }

}
