package com.getouo.gb.scl.data

class PESHeader(bytes: Array[Byte]) {
  if (bytes.indexOfSlice(PSH264Data.PS_PES_HEADER) != 0) throw new IllegalArgumentException("PESHeader tag 0x000001 must index of 0, error: " + bytes.take(3).toSeq)
}
