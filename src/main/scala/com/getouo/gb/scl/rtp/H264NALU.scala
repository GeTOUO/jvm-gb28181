//package com.getouo.gb.scl.rtp
//
//import java.nio.ByteBuffer
//import java.util.concurrent.atomic.AtomicInteger
//
//case class H264NALU(startCodeLen: Int, nalu: Array[Byte]) {
//  private val head: Byte = nalu.head
//  //  val forbiddenBit: Int = head >> 7 // 一般为0，为 1 时表示此单元出现错误，解码器会丢弃该数据，按丢包处理。
//  val forbiddenBit: Int = head & 0x80 //1 bit 一般为0，为 1 时表示此单元出现错误，解码器会丢弃该数据，按丢包处理。
//  //  val nalReferenceIdc: Int = (head << 1).byteValue() >> 6 // 由编码器决定该 NAL 单元的优先级，值越高，优先级越高。00 时表示该帧不用于在重建参考图像的内部预测，可以丢弃。
//  val nalReferenceIdc: Int = head & 0x60; // 2 bit ,由编码器决定该 NAL 单元的优先级，值越高，优先级越高。00 时表示该帧不用于在重建参考图像的内部预测，可以丢弃。
//  /**
//   * 在网络中如何传输这些 NAL 单元，有不同的方式，常见的就是通过 RFC3984/RFC6184 定义的通过 RTP 打包的传输方式。
//   * 一般情况下，在传输之前，需要事先协商好如何传输，传输什么内容。
//   * 这种协商的描述方式采用 SDP 来完成。所以在会话协议中如 SIP, RTSP 等都会携带 SDP 的文本描述，使得双方完成意见交换，达成一致。
//   */
//  //  val nalUnitType: Int = (head << 3).byteValue() >> 3 //
//  val nalUnitType: Int = head & 0x1f; // 5 bit
//
//  def rtpPacket(seq: AtomicInteger, nextTime: Int): Seq[Array[Byte]] = {
//    if (nalu.length - 1 <= H264NALU.DEFAULT_MTU_LEN) {
//      Seq(singleRtpPacket(seq, nextTime))
//    } else {
//      multipleRtpPacket(seq, nextTime)
//    }
//  }
//
//  private def singleRtpPacket(seq: AtomicInteger, nextTime: Int): Array[Byte] = {
//    val rtpHeaderBytes = new Array[Byte](13)
//    val timeWrite = ByteBuffer.allocate(4)
//    timeWrite.putInt(nextTime)
//    timeWrite.position(0)
//    timeWrite.get(rtpHeaderBytes, 4, 4)
//
//    rtpHeaderBytes(0) = 0x80.toByte // 版本号,此版本固定为2
//    rtpHeaderBytes(1) = ((rtpHeaderBytes(1) | 96).toByte & 254).toByte // 负载类型号96 ; 标志位，由具体协议规定其值
//    rtpHeaderBytes(11) = 10 //随即指定10，并在本RTP回话中全局唯一,java默认采用网络字节序号 不用转换
//
//    rtpHeaderBytes(1) = (rtpHeaderBytes(1) | 0x80).toByte // 设置rtp M位为1
//    writeSeq(rtpHeaderBytes, seq.incrementAndGet())
//
//    rtpHeaderBytes(12) = (forbiddenBit.toByte << 7).toByte
//    rtpHeaderBytes(12) = (rtpHeaderBytes(12) | (nalReferenceIdc >> 5).toByte << 5).toByte
//    rtpHeaderBytes(12) = (rtpHeaderBytes(12) | nalUnitType.toByte).toByte
//
//    rtpHeaderBytes ++ nalu.tail
//  }
//
//  private def multipleRtpPacket(seq: AtomicInteger, nextTime: Int): Seq[Array[Byte]] = {
//
//    val groupedBytes = nalu.tail.grouped(1400).toSeq
//    groupedBytes.indices.collect { case i => (i, groupedBytes(i)) }.map { case (index, data) => {
//      val rtpHeaderBytes = new Array[Byte](14)
//      val timeWrite = ByteBuffer.allocate(4)
//      timeWrite.putInt(nextTime)
//      timeWrite.position(0)
//      timeWrite.get(rtpHeaderBytes, 4, 4)
//
//      rtpHeaderBytes(0) = 0x80.toByte // 版本号,此版本固定为2
//      rtpHeaderBytes(1) = ((rtpHeaderBytes(1) | 96).toByte & 254).toByte // 负载类型号96 ; 标志位，由具体协议规定其值
//      rtpHeaderBytes(11) = 10 //随即指定10，并在本RTP回话中全局唯一,java默认采用网络字节序号 不用转换
//      writeSeq(rtpHeaderBytes, seq.incrementAndGet)
//      if (index == 0) {
//        rtpHeaderBytes(1) = (rtpHeaderBytes(1) & 0x7F).toByte // 设置rtp M位为0
//        val rtpSendBodys = new Array[Byte](1400 + 14)
//
//        // 设置FU INDICATOR,并将这个HEADER填入sendbuf[12]
//        rtpHeaderBytes(12) = (rtpHeaderBytes(12) | forbiddenBit.toByte << 7).toByte
//        rtpHeaderBytes(12) = (rtpHeaderBytes(12) | (nalReferenceIdc >> 5).toByte << 5).toByte
//        rtpHeaderBytes(12) = (rtpHeaderBytes(12) | 28.toByte).toByte
//
//        // 设置FU HEADER,并将这个HEADER填入snedbuf[13]
//        rtpHeaderBytes(13) = (rtpHeaderBytes(13) & 0xBF).toByte //E=0
//        rtpHeaderBytes(13) = (rtpHeaderBytes(13) & 0xDF).toByte //R=0
//        rtpHeaderBytes(13) = (rtpHeaderBytes(13) | 0x80).toByte //S=1
//        rtpHeaderBytes(13) = (rtpHeaderBytes(13) | nalUnitType.toByte).toByte
//
//        rtpHeaderBytes ++ data
//      } else if (index == groupedBytes.length - 1) {
//        //  设置rtp M位,当前床书的是最后一个分片时该位置1
//        rtpHeaderBytes(1) = (rtpHeaderBytes(1) | 0x80).toByte
//
//        // 设置FU INDICATOR,并将这个HEADER填入sendbuf[12]
//        rtpHeaderBytes(12) = (rtpHeaderBytes(12) | forbiddenBit.toByte << 7).toByte
//        rtpHeaderBytes(12) = (rtpHeaderBytes(12) | (nalReferenceIdc >> 5).toByte << 5).toByte
//        rtpHeaderBytes(12) = (rtpHeaderBytes(12) | 28.toByte).toByte
//
//        //设置FU HEADER,并将这个HEADER填入sendbuf[13]
//        rtpHeaderBytes(13) = (rtpHeaderBytes(13) & 0xDF).toByte //R=0
//        rtpHeaderBytes(13) = (rtpHeaderBytes(13) & 0x7F).toByte //S=0
//        rtpHeaderBytes(13) = (rtpHeaderBytes(13) | 0x40).toByte //E=1
//        rtpHeaderBytes(13) = (rtpHeaderBytes(13) | nalUnitType.toByte).toByte
//
//        rtpHeaderBytes ++ data
//      } else {
//        rtpHeaderBytes(1) = (rtpHeaderBytes(1) & 0x7F).toByte // M=0
//
//        // 设置FU INDICATOR,并将这个HEADER填入sendbuf[12]
//        rtpHeaderBytes(12) = (rtpHeaderBytes(12) | forbiddenBit.toByte << 7).toByte
//        rtpHeaderBytes(12) = (rtpHeaderBytes(12) | (nalReferenceIdc >> 5).toByte << 5).toByte
//        rtpHeaderBytes(12) = (rtpHeaderBytes(12) | 28.toByte).toByte
//
//        //设置FU HEADER,并将这个HEADER填入sendbuf[13]
//        rtpHeaderBytes(13) = (rtpHeaderBytes(13) & 0xDF).toByte //R=0
//        rtpHeaderBytes(13) = (rtpHeaderBytes(13) & 0x7F).toByte //S=0
//        rtpHeaderBytes(13) = (rtpHeaderBytes(13) & 0xBF).toByte //E=0
//        rtpHeaderBytes(13) = (rtpHeaderBytes(13) | nalUnitType.toByte).toByte
//
//        rtpHeaderBytes ++ data
//      }
//    }
//    }
//  }
//
//  private def writeSeq(header: Array[Byte], seq: Int): Unit = {
//    H264NALU.seqIOBuffer.position(0)
//    H264NALU.seqIOBuffer.putInt(seq)
//    H264NALU.seqIOBuffer.position(2)
//    H264NALU.seqIOBuffer.get(header, 2, 2)
//  }
//
//  //  private def calcFUIndicator(): Byte = {
//  //
//  //  }
//  //
//  //  private def calcFUHeader(): Byte = {
//  //
//  //  }
//}
//
//object H264NALU {
//  val DEFAULT_MTU_LEN = 1400
//  val FUIndicatorType = 28
//
//  val seqIOBuffer: ByteBuffer = ByteBuffer.allocateDirect(4)
//
//  def main(args: Array[String]): Unit = {
//    val in: Integer = 60000
//    println(in.shortValue())
//    println(in.toShort)
//
//    //    var i: Integer = 1
//    //    while ( {
//    //      i < 65535
//    //    }) {
//    //      val x = i.shortValue
//    //      if (x <= 0) System.err.println(x)
//    //      i += 1
//    //    }
//  }
//}
