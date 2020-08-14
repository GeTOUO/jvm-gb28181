package com.getouo.gb.scl.model

import com.getouo.gb.scl.util.ConstVal

/**
 *
 * @param v
 * @param sessionIdIsNTPTimestamp NTP时间戳
 * @param serverAddress
 */
case class SDPSessionInfo(v: Int, sessionIdIsNTPTimestamp: Long, serverAddress: String) {
  val username: String = "-"
  val sessionVersion = 1
  val netType = "IN"
  val ipType = "IP4"

  def test(): String = {
    s"v=$v\r\n" + s"o=$username $sessionIdIsNTPTimestamp $sessionVersion $netType $ipType $serverAddress\r\n" + s"t=0 0\r\n" + s"a=contol:*\r\n"
  }
}
case class SDPMediaInfo(mediaType: String, port: Int, rtpTransType: ConstVal.RtpTransType, mediaFormat: Int, aGroup: Seq[(Char, String)]) {

  def text(): String = {
    s"m=$mediaType $port ${rtpTransType.value} $mediaFormat\r\n" + (if (aGroup.isEmpty) "" else aGroup.map{case(t, v) => s"$t=$v\r\n"}.reduce((a, b) => a + b))
  }
}
case class SDPInfo(sessionInfo: SDPSessionInfo, mediaInfo: Seq[SDPMediaInfo]) {
  def text(): String = sessionInfo.test() + (if (mediaInfo.isEmpty) "" else mediaInfo.map(_.text()).reduce((a, b) => a + b))
}
