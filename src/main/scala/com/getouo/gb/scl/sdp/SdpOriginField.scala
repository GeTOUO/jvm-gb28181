package com.getouo.gb.scl.sdp

import java.util

/**
 *
 * @param username 用户名
 * @param sessionId 会话id
 * @param sessionVersion 会话版本
 * @param netType 网络类型
 * @param addrType 地址类型
 * @param addr 服务器的地址
 */
case class SdpOriginValue(username: String = "-", sessionId: String, sessionVersion: Int, netType: String, addrType: String, addr: String) {
  override def toString: String = s"$username $sessionId $sessionVersion $netType $addrType $addr"
}
object SdpOriginValue {
  def from(value: String): SdpOriginValue = {
    val strings = value.split(" ")
    SdpOriginValue(
      username = SdpField.strArrAccessor(strings, 0).getOrElse("-"),
      sessionId = SdpField.strArrAccessor(strings, 1).getOrElse("0"),
      sessionVersion = SdpField.strArrAccessor(strings, 2).getOrElse("0").toInt,
      netType = SdpField.strArrAccessor(strings, 3).getOrElse("IN"),
      addrType = SdpField.strArrAccessor(strings, 4).getOrElse("IP4"),
      addr = SdpField.strArrAccessor(strings, 5).getOrElse("0.0.0.0")
    )
  }
}

case class SdpOriginField(value: String) extends SdpField[SdpOriginValue] {
  override val key: String = "o"
  override val valueReader: SdpOriginValue = SdpOriginValue.from(value)

  override def toString: String = s"$key=${valueReader.toString}"

}
