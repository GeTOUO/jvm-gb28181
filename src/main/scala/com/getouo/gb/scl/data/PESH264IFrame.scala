package com.getouo.gb.scl.data

import java.nio.ByteBuffer

import com.getouo.gb.scl.util.H264NALUFramer

import scala.collection.mutable.ArrayBuffer

//class PESFrame extends PSH264Data with H264NALUFramer {
class PESFrame extends PSH264Data {

//  private var payload: ArrayBuffer[Byte] = ArrayBuffer.empty
  private var payload: ArrayBuffer[(Long, H264NaluData)] = ArrayBuffer.empty

//  def addBytes(arr: Array[Byte]): Unit = this.payload.addAll(arr)
  def addBytes(arr: (Long, H264NaluData)): Unit = this.payload.addOne(arr)

  def addLastBytes(arr: Array[Byte]): Unit = {
    val last = this.payload.last
    this.payload.update(this.payload.length - 1, last.copy(_2 = last._2.copy(nalu = last._2.nalu ++ arr)))
  }

  def getNalus: Array[(Long, H264NaluData)] = payload.toArray

//  def getArray: Array[Byte] = this.payload.toArray
//  def getArray: Array[Byte] = this.payload.ma.toArray

  private var readableBuffer: ByteBuffer = ByteBuffer.allocate(0)

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

case class PSH264Audio() extends PSH264Data {}
