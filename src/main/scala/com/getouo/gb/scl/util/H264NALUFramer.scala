package com.getouo.gb.scl.util

import com.getouo.gb.scl.io.{H264NaluData, UnsafeNaluData}

object H264NALUFramer {

  val START_TAG4: Array[Byte] = Array[Byte](0, 0, 0, 1)
  val START_TAG3: Array[Byte] = Array[Byte](0, 0, 1)

  case class StepInfo(data: UnsafeNaluData, nextStartLen: Int, leftover: Array[Byte])
  def nextUnit(buf: Array[Byte]): Option[StepInfo] = {
    val i4i = buf.indexOfSlice(START_TAG4)
    if (i4i != -1) {
      Some(StepInfo(H264NaluData.of(0, buf.take(i4i)), 4, buf.drop(i4i + 4)))
    } else {
      val i3i = buf.indexOfSlice(START_TAG3)
      if (i3i != -1) {
        Some(StepInfo(H264NaluData.of(0, buf.take(i3i)), 3, buf.drop(i3i + 3)))
      } else None
    }
  }
}
