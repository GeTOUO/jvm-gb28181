package com.getouo.gb.scl.sdp

case class SessionDescription(origin: SdpOriginField) {
  val version: SdpVersionField = SdpVersionField()
}
