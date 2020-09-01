package com.getouo.gb.scl.model

import com.getouo.gb.scl.util.ConstVal

object RtspConstVal {

  val version = "RTSP/1.0"

  object RtspMethod {
    val OPTIONS: String = "OPTIONS"
    val DESCRIBE: String = "DESCRIBE"
    val SETUP: String = "SETUP"
    val PLAY: String = "PLAY"
    val TEARDOWN: String = "TEARDOWN"
  }

}

trait RtspMessage {
  val version: String = RtspConstVal.version
  val CSeq: Int

  def stringMessage(): String
}

/**
 * method url vesion\r\n
 * CSeq: x\r\n
 * xxx\r\n
 * ...
 * \r\n
 */
trait RtspRequest extends RtspMessage {
  val method: String
  val url: String
}

/**
 * vesion 200 OK\r\n
 * CSeq: x\r\n
 * xxx\r\n
 * ...
 * \r\n
 */
trait RtspResponse extends RtspMessage {
  //  val method: String
}

case class RtspTeardownRequest(url: String, CSeq: Int, session: Long) extends RtspRequest {
  override val method: String = RtspConstVal.RtspMethod.TEARDOWN

  override def stringMessage(): String =
    s"""$method $url $version
       |CSeq: $CSeq
       |Session: $session
       |
       |""".stripMargin
}

case class RtspTeardownResponse(CSeq: Int) extends RtspResponse {
  override def stringMessage(): String =
    s"""$version 200 OK
       |CSeq: $CSeq
       |
       |""".stripMargin
}

case class RtspPlayRequest(url: String, CSeq: Int, session: Long, range: String) extends RtspRequest {
  override val method: String = RtspConstVal.RtspMethod.PLAY

  def defaultResponse(): RtspPlayResponse = RtspPlayResponse(CSeq, session, range)

  override def stringMessage(): String =
    s"""$method $url $version
       |CSeq: $CSeq
       |Session: $session
       |Range: $range
       |
       |""".stripMargin
}

case class RtspPlayResponse(CSeq: Int, session: Long, range: String, timeout: Int = 60) extends RtspResponse {
  override def stringMessage(): String =
    s"""$version 200 OK
       |CSeq: $CSeq
       |Range: $range
       |Session: $session; timeout=$timeout
       |RTP-Info: url=trackID=0;seq=1;rtptime=3600
       |
       |""".stripMargin
}

case class RtspSetupRequest(url: String, CSeq: Int, rtpTransType: ConstVal.RtpTransport) extends RtspRequest {
  override val method: String = RtspConstVal.RtspMethod.SETUP

  override def stringMessage(): String =
    s"""$method $url $version
       |CSeq: $CSeq
       |Transport: ${rtpTransType.transportValue()}
       |
       |""".stripMargin
}

case class RtspSetupResponse(CSeq: Int, rtpTransType: ConstVal.RtpTransport, session: Long) extends RtspResponse {

  override def stringMessage(): String =
  //    s"""$version 200 OK
  //       |CSeq: $CSeq
  //       |Transport: ${rtpTransType.transportValue()}
  //       |Session: $session;timeout=600
  //       |Expires: Fri, 28 Aug 2020 08:51:43 UTC
  //       |Date: Fri, 28 Aug 2020 08:51:43 UTC
  //       |Cache-Control: no-cache
  //       |
  //       |""".stripMargin

    s"""RTSP/1.0 200 OK
       |CSeq: $CSeq
       |Server: Wowza Streaming Engine 4.7.5.01 build21752
       |Cache-Control: no-cache
       |Expires: Fri, 28 Aug 2020 08:51:43 UTC
       |Transport: RTP/AVP/TCP;unicast;interleaved=0-1
       |Date: Fri, 28 Aug 2020 08:51:43 UTC
       |Session: $session;timeout=60
       |
       |""".stripMargin
}

case class RtspDescribeRequest(url: String, CSeq: Int, userAgent: String, accept: String) extends RtspRequest {
  override val method: String = RtspConstVal.RtspMethod.DESCRIBE

  def defaultResponse(sdp: SDPInfo): RtspDescribeResponse = new RtspDescribeResponse(CSeq, sdp, accept)

  def defaultResponse(sdp: String): RtspDescribeResponse = RtspDescribeResponse(CSeq, sdp, accept)

  def resp404(message: String): String =
    s"""$version 404 $message
       |CSeq: $CSeq
       |
       |""".stripMargin

  override def stringMessage(): String =
    s"""$method $url $version
       |CSeq: $CSeq
       |User-Agent: $userAgent
       |Accept: $accept
       |
       |""".stripMargin
}

case class RtspDescribeResponse(CSeq: Int, sdpStr: String, contentType: String) extends RtspResponse {
  def this(CSeq: Int, sdp: SDPInfo, contentType: String) {
    this(CSeq, sdp.text(), contentType)
  }

  //  private val sdpStr: String = sdp
  override def stringMessage(): String =
    s"""$version 200 OK
       |CSeq: $CSeq
       |Content-length: ${sdpStr.length}
       |Content-Type: $contentType
       |
       |$sdpStr
       |""".stripMargin
}

case class RtspOptionsRequest(url: String, CSeq: Int) extends RtspRequest {
  override val method: String = RtspConstVal.RtspMethod.OPTIONS

  def defaultResponse(publicMethods: Seq[String] = Seq("OPTIONS", "DESCRIBE", "SETUP", "TEARDOWN", "PLAY")): RtspOptionsResponse =
    RtspOptionsResponse(CSeq, publicMethods)

  override def stringMessage(): String =
    s"""$method $url $version
       |CSeq: $CSeq
       |
       |""".stripMargin
}

case class RtspOptionsResponse(CSeq: Int, publicMethods: Seq[String] = Seq("OPTIONS", "DESCRIBE", "SETUP", "TEARDOWN", "PLAY"))
  extends RtspResponse {
  override def stringMessage(): String =
    s"""$version 200 OK
       |CSeq: $CSeq
       |Public: ${if (publicMethods.isEmpty) "" else publicMethods.reduce((a, b) => a + ", " + b)}
       |
       |""".stripMargin
}