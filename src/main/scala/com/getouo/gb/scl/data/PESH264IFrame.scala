package com.getouo.gb.scl.data

import java.nio.ByteBuffer

import com.getouo.gb.scl.util.H264NALUFramer

import scala.collection.mutable.ArrayBuffer

//class PESFrame extends PSH264Data with H264NALUFramer {
class PESFrame extends PSH264Data {

  private val h264Payload: ArrayBuffer[Byte] = ArrayBuffer.empty

  def addBytes(arr: Array[Byte]): Unit = this.h264Payload.addAll(arr)

  def splitNALU: Array[H264NaluData] = {
    // 用于收集
    val collector: ArrayBuffer[H264NaluData] = ArrayBuffer.empty
    var remainingData = this.h264Payload.toArray
    var thePreviousTagLen = -1
    var hasRemaining = true
    while (hasRemaining) {
//      H264NALUFramer.nextUnit(remainingData, Array[Array[Byte]](H264NALUFramer.START_TAG4, H264NALUFramer.START_TAG3)) match {
      H264NALUFramer.nextUnit(remainingData, false) match {
        case Some(step) =>
          if (thePreviousTagLen != -1) {
            step.data match {
              case d@H264NaluData(_, arr) if arr.length > 0 =>
                collector.addOne(d.copy(startCodeLen = thePreviousTagLen))
              case _ =>
            }
          }
          remainingData = step.leftover
          thePreviousTagLen = step.nextStartTagLen
        case None =>
          if (thePreviousTagLen == -1) {
            H264NALUFramer.nextUnit(remainingData) match {
              case None =>
              case Some(value) => collector.addOne(H264NaluData(value.nextStartTagLen, value.leftover))
            }
          }
          if (thePreviousTagLen != -1 && remainingData.nonEmpty)
            collector.addOne(H264NaluData(thePreviousTagLen, remainingData))
          hasRemaining = false
      }
    }
    collector.toArray
  }

  //  def getArray: Array[Byte] = this.payload.toArray
  //  def getArray: Array[Byte] = this.payload.ma.toArray

  //  def decodeNALU(): Array[H264NaluData] = {
  //    readableBuffer = ByteBuffer.wrap(payload.toArray)
  //
  //    val res: ArrayBuffer[H264NaluData] = ArrayBuffer.empty
  //    var w = true
  //    while (w) {
  //      produceNULA() match {
  //        case data@H264NaluData(startCodeLen, nalu) if (startCodeLen != -1 && nalu.length > 0) => res.addOne(data)
  //        case EndSymbol => w = false
  //        case _ => w = false
  //      }
  //    }
  //    res.toArray
  //  }

  //  override protected def read(buf: Array[Byte]): Array[Byte] = {
  //    if (readableBuffer.hasRemaining) {
  //      val i = readableBuffer.remaining()
  //      val re = if (i < buf.length) new Array[Byte](i) else buf
  //      readableBuffer.get(re)
  //      re
  //    } else {
  //      Array.empty
  //    }
  //  }
}

case class PESH264IFrame() extends PESFrame {}

case class PESH264PFrame() extends PESFrame {}

case class PSH264Audio() extends PESFrame {}
