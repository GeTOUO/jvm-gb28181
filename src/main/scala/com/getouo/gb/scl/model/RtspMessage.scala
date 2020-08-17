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
    s"""
       |$method $url $version
       |CSeq: $CSeq
       |Session: $session
       |
       |""".stripMargin
}

case class RtspTeardownResponse(CSeq: Int) extends RtspResponse {
  override def stringMessage(): String =
    s"""
       |$version 200 OK
       |CSeq: $CSeq
       |
       |""".stripMargin
}

case class RtspPlayRequest(url: String, CSeq: Int, session: Long, range: String) extends RtspRequest {
  override val method: String = RtspConstVal.RtspMethod.PLAY

  def defaultResponse(): RtspPlayResponse = RtspPlayResponse(CSeq, session, range)

  override def stringMessage(): String =
    s"""
       |$method $url $version
       |CSeq: $CSeq
       |Session: $session
       |Range: $range
       |
       |""".stripMargin
}

case class RtspPlayResponse(CSeq: Int, session: Long, range: String, timeout: Int = 60) extends RtspResponse {
  override def stringMessage(): String =
    s"""
       |$version 200 OK
       |CSeq: $CSeq
       |Range: $range
       |Session: $session; timeout=$timeout
       |
       |""".stripMargin
}

case class RtspSetupRequest(url: String, CSeq: Int, rtpTransType: ConstVal.RtpTransType) extends RtspRequest {
  override val method: String = RtspConstVal.RtspMethod.SETUP
  override def stringMessage(): String =
    s"""
       |$method $url $version
       |CSeq: $CSeq
       |Transport: ${rtpTransType.transportValue()}
       |
       |""".stripMargin
}
case class RtspSetupResponse(CSeq: Int, rtpTransType: ConstVal.RtpTransType, session: Long) extends RtspResponse {

  override def stringMessage(): String =
    s"""
       |$version 200 OK
       |CSeq: $CSeq
       |Transport: ${rtpTransType.transportValue()}
       |Session: $session
       |
       |""".stripMargin
}

case class RtspDescribeRequest(url: String, CSeq: Int, userAgent: String, accept: String) extends RtspRequest {
  override val method: String = RtspConstVal.RtspMethod.DESCRIBE

  def defaultResponse(sdp: SDPInfo): RtspDescribeResponse = RtspDescribeResponse(CSeq, sdp, accept)

  override def stringMessage(): String =
    s"""
       |$method $url $version
       |CSeq: $CSeq
       |User-Agent: $userAgent
       |Accept: $accept
       |
       |""".stripMargin
}

case class RtspDescribeResponse(CSeq: Int, sdp: SDPInfo, contentType: String) extends RtspResponse {
  private val sdpStr: String = sdp.text()
  override def stringMessage(): String =
    s"""
       |$version 200 OK
       |CSeq: $CSeq
       |Content-length: ${sdpStr.length}
       |Content-Type: $contentType
       |
       |
       |$sdpStr
       |""".stripMargin
}

case class RtspOptionsRequest(url: String, CSeq: Int) extends RtspRequest {
  override val method: String = RtspConstVal.RtspMethod.OPTIONS

  def defaultResponse(publicMethods: Seq[String] = Seq("OPTIONS", "DESCRIBE", "SETUP", "TEARDOWN", "PLAY")): RtspOptionsResponse =
    RtspOptionsResponse(CSeq, publicMethods)

  override def stringMessage(): String =
    s"""
       |$method $url $version
       |CSeq: $CSeq
       |
       |""".stripMargin
}

case class RtspOptionsResponse(CSeq: Int, publicMethods: Seq[String] = Seq("OPTIONS", "DESCRIBE", "SETUP", "TEARDOWN", "PLAY"))
  extends RtspResponse {
  override def stringMessage(): String =
    s"""
       |$version 200 OK
       |CSeq: $CSeq
       |Public: ${if (publicMethods.isEmpty) "" else publicMethods.reduce((a, b) => a + ", " + b)}
       |
       |""".stripMargin
}