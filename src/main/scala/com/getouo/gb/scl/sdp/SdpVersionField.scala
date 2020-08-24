package com.getouo.gb.scl.sdp

case class SdpVersionField(value: String = "1") extends SdpField[Int] {
  val key: String = "v"
  override val valueReader: Int = value.toInt
}
