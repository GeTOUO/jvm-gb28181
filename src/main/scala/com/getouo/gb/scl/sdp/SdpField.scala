package com.getouo.gb.scl.sdp

trait SdpField[V <: Any] {
  val key: String
  val value: String
  val valueReader: V
}

object SdpField {
  def strArrAccessor(arr: Array[String], index: Int): Option[String] =
    if (index >= arr.length) None else Some(arr(index).trim)
}
