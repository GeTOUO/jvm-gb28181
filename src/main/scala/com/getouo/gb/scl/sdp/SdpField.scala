package com.getouo.gb.scl.sdp

case class SdpFieldName(name: Char) extends AnyVal

object SdpFieldName {
  val VERSION = SdpFieldName('v')
  val ORIGIN = SdpFieldName('o')
  val SESSION_NAME = SdpFieldName('s')
  val CONNECTION = SdpFieldName('c')
  val BANDWIDTH = SdpFieldName('b')
  val TIMES = SdpFieldName('t')

  val ATTR = SdpFieldName('a')

  val M_MEDIA = SdpFieldName('m')

}

trait SdpField[V <: Any] {
  val key: String
  val value: String
  val valueReader: V
}

object SdpField {
  def strArrAccessor(arr: Array[String], index: Int): Option[String] =
    if (index >= arr.length) None else Some(arr(index).trim)
}
