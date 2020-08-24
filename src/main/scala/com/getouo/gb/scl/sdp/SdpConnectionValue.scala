package com.getouo.gb.scl.sdp

case class SdpConnectionValue(netType: String, addrType: String, addr: String) {
  override def toString: String = s"$netType $addrType $addr"
}

object SdpConnectionValue {
  def from(value: String): SdpConnectionValue = {
    val strings = value.split(" ")
    SdpConnectionValue(
      SdpField.strArrAccessor(strings, 0).getOrElse("IN"),
      SdpField.strArrAccessor(strings, 1).getOrElse("IP4"),
      SdpField.strArrAccessor(strings, 2).getOrElse("0.0.0.0")
    )
  }
}
case class SdpConnectionField(value: String) extends SdpField[SdpConnectionValue] {
  override val key: String = "c"
  override val valueReader: SdpConnectionValue = SdpConnectionValue.from(value)

  override def toString: String = s"$key=${valueReader.toString}"
}

