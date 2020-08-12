package com.getouo.gb.scl.io

import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger

trait ISourceData
trait H264SourceData extends ISourceData

trait EndSymbolData extends ISourceData

case class ErrorSourceData(thr: Throwable) extends ISourceData
case class ByteSourceData(array: Array[Byte]) extends ISourceData

trait UnsafeNaluData
case class EmptyNaluData(startLen: Int) extends UnsafeNaluData
case class H264NaluData(startCodeLen: Int, nalu: Array[Byte]) extends H264SourceData with UnsafeNaluData {
  private val nalu_head: Byte = nalu.head
  val forbiddenBit: Int = nalu_head & 0x80 //1 bit 一般为0，为 1 时表示此单元出现错误，解码器会丢弃该数据，按丢包处理。
  val nalReferenceIdc: Int = nalu_head & 0x60;
  val nalUnitType: Int = nalu_head & 0x1f; // 5 bit

  def rtpPacket(seq: AtomicInteger, nextTime: Int): Seq[Array[Byte]] = {
    if (nalu.length - 1 <= H264NaluData.DEFAULT_MTU_LEN) {
      Seq(singleRtpPacket(seq, nextTime))
    } else {
      multipleRtpPacket(seq, nextTime)
    }
  }

  private def singleRtpPacket(seq: AtomicInteger, nextTime: Int): Array[Byte] = {
    val rtpHeaderBytes = new Array[Byte](13)
    val timeWrite = ByteBuffer.allocate(4)
    timeWrite.putInt(nextTime)
    timeWrite.position(0)
    timeWrite.get(rtpHeaderBytes, 4, 4)

    rtpHeaderBytes(0) = 0x80.toByte // 版本号,此版本固定为2
    rtpHeaderBytes(1) = ((rtpHeaderBytes(1) | 96).toByte & 254).toByte // 负载类型号96 ; 标志位，由具体协议规定其值
    rtpHeaderBytes(11) = 10 //随即指定10，并在本RTP回话中全局唯一,java默认采用网络字节序号 不用转换

    rtpHeaderBytes(1) = (rtpHeaderBytes(1) | 0x80).toByte // 设置rtp M位为1
    writeSeq(rtpHeaderBytes, seq.incrementAndGet())

    rtpHeaderBytes(12) = (forbiddenBit.toByte << 7).toByte
    rtpHeaderBytes(12) = (rtpHeaderBytes(12) | (nalReferenceIdc >> 5).toByte << 5).toByte
    rtpHeaderBytes(12) = (rtpHeaderBytes(12) | nalUnitType.toByte).toByte

    rtpHeaderBytes ++ nalu.tail
  }

  private def multipleRtpPacket(seq: AtomicInteger, nextTime: Int): Seq[Array[Byte]] = {

    val groupedBytes = nalu.tail.grouped(1400).toSeq
    groupedBytes.indices.collect { case i => (i, groupedBytes(i)) }.map { case (index, data) => {
      val rtpHeaderBytes = new Array[Byte](14)
      val timeWrite = ByteBuffer.allocate(4)
      timeWrite.putInt(nextTime)
      timeWrite.position(0)
      timeWrite.get(rtpHeaderBytes, 4, 4)

      rtpHeaderBytes(0) = 0x80.toByte // 版本号,此版本固定为2
      rtpHeaderBytes(1) = ((rtpHeaderBytes(1) | 96).toByte & 254).toByte // 负载类型号96 ; 标志位，由具体协议规定其值
      rtpHeaderBytes(11) = 10 //随即指定10，并在本RTP回话中全局唯一,java默认采用网络字节序号 不用转换
      writeSeq(rtpHeaderBytes, seq.incrementAndGet)
      if (index == 0) {
        rtpHeaderBytes(1) = (rtpHeaderBytes(1) & 0x7F).toByte // 设置rtp M位为0
        val rtpSendBodys = new Array[Byte](1400 + 14)

        // 设置FU INDICATOR,并将这个HEADER填入sendbuf[12]
        rtpHeaderBytes(12) = (rtpHeaderBytes(12) | forbiddenBit.toByte << 7).toByte
        rtpHeaderBytes(12) = (rtpHeaderBytes(12) | (nalReferenceIdc >> 5).toByte << 5).toByte
        rtpHeaderBytes(12) = (rtpHeaderBytes(12) | 28.toByte).toByte

        // 设置FU HEADER,并将这个HEADER填入snedbuf[13]
        rtpHeaderBytes(13) = (rtpHeaderBytes(13) & 0xBF).toByte //E=0
        rtpHeaderBytes(13) = (rtpHeaderBytes(13) & 0xDF).toByte //R=0
        rtpHeaderBytes(13) = (rtpHeaderBytes(13) | 0x80).toByte //S=1
        rtpHeaderBytes(13) = (rtpHeaderBytes(13) | nalUnitType.toByte).toByte

        rtpHeaderBytes ++ data
      } else if (index == groupedBytes.length - 1) {
        //  设置rtp M位,当前床书的是最后一个分片时该位置1
        rtpHeaderBytes(1) = (rtpHeaderBytes(1) | 0x80).toByte

        // 设置FU INDICATOR,并将这个HEADER填入sendbuf[12]
        rtpHeaderBytes(12) = (rtpHeaderBytes(12) | forbiddenBit.toByte << 7).toByte
        rtpHeaderBytes(12) = (rtpHeaderBytes(12) | (nalReferenceIdc >> 5).toByte << 5).toByte
        rtpHeaderBytes(12) = (rtpHeaderBytes(12) | 28.toByte).toByte

        //设置FU HEADER,并将这个HEADER填入sendbuf[13]
        rtpHeaderBytes(13) = (rtpHeaderBytes(13) & 0xDF).toByte //R=0
        rtpHeaderBytes(13) = (rtpHeaderBytes(13) & 0x7F).toByte //S=0
        rtpHeaderBytes(13) = (rtpHeaderBytes(13) | 0x40).toByte //E=1
        rtpHeaderBytes(13) = (rtpHeaderBytes(13) | nalUnitType.toByte).toByte

        rtpHeaderBytes ++ data
      } else {
        rtpHeaderBytes(1) = (rtpHeaderBytes(1) & 0x7F).toByte // M=0

        // 设置FU INDICATOR,并将这个HEADER填入sendbuf[12]
        rtpHeaderBytes(12) = (rtpHeaderBytes(12) | forbiddenBit.toByte << 7).toByte
        rtpHeaderBytes(12) = (rtpHeaderBytes(12) | (nalReferenceIdc >> 5).toByte << 5).toByte
        rtpHeaderBytes(12) = (rtpHeaderBytes(12) | 28.toByte).toByte

        //设置FU HEADER,并将这个HEADER填入sendbuf[13]
        rtpHeaderBytes(13) = (rtpHeaderBytes(13) & 0xDF).toByte //R=0
        rtpHeaderBytes(13) = (rtpHeaderBytes(13) & 0x7F).toByte //S=0
        rtpHeaderBytes(13) = (rtpHeaderBytes(13) & 0xBF).toByte //E=0
        rtpHeaderBytes(13) = (rtpHeaderBytes(13) | nalUnitType.toByte).toByte

        rtpHeaderBytes ++ data
      }
    }
    }
  }

  private def writeSeq(header: Array[Byte], seq: Int): Unit = {
    H264NaluData.seqIOBuffer.position(0)
    H264NaluData.seqIOBuffer.putInt(seq)
    H264NaluData.seqIOBuffer.position(2)
    H264NaluData.seqIOBuffer.get(header, 2, 2)
  }
}
object H264NaluData {
  val DEFAULT_MTU_LEN = 1400
  val FUIndicatorType = 28

  val seqIOBuffer: ByteBuffer = ByteBuffer.allocateDirect(4)
  def of(startLen: Int, nalu: Array[Byte]): UnsafeNaluData = {
    if (nalu.length <= 0) {
      EmptyNaluData(startLen)
    } else H264NaluData(startLen, nalu)
  }
}

case class EndSymbol() extends EndSymbolData with H264SourceData
