package com.getouo.gb.scl.data

class PSHeader(bytes: Array[Byte]) {
  if (bytes.indexOfSlice(PSH264Data.PS_HEADER) != 0) throw new IllegalArgumentException("PSHeader tag 0x000001BA must index of 0, error: " + bytes.take(4).toSeq)
  if (bytes.length != 14) throw new IllegalArgumentException("PSHeader length must = 12, error: " + bytes.length)
  val stuffingLength: Int = bytes.last & 0x07
}
