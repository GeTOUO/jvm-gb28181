package com.getouo.gb.scl.rtp

case class H264NALU(startCode: Int, nalu: Array[Byte]) {
  private val head: Byte = nalu.head
//  val forbiddenBit: Int = head >> 7 // 一般为0，为 1 时表示此单元出现错误，解码器会丢弃该数据，按丢包处理。
  val forbiddenBit: Int = head & 0x80 //1 bit 一般为0，为 1 时表示此单元出现错误，解码器会丢弃该数据，按丢包处理。
  val nalReferenceIdc: Int = (head << 1).byteValue() >> 6 // 由编码器决定该 NAL 单元的优先级，值越高，优先级越高。00 时表示该帧不用于在重建参考图像的内部预测，可以丢弃。
//  val nalReferenceIdc: Int = head & 0x60; // 2 bit ,由编码器决定该 NAL 单元的优先级，值越高，优先级越高。00 时表示该帧不用于在重建参考图像的内部预测，可以丢弃。
  /**
   * 在网络中如何传输这些 NAL 单元，有不同的方式，常见的就是通过 RFC3984/RFC6184 定义的通过 RTP 打包的传输方式。
   * 一般情况下，在传输之前，需要事先协商好如何传输，传输什么内容。
   * 这种协商的描述方式采用 SDP 来完成。所以在会话协议中如 SIP, RTSP 等都会携带 SDP 的文本描述，使得双方完成意见交换，达成一致。
   */
  val nalUnitType: Int = (head << 3).byteValue() >> 3 //
//  val nalUnitType: Int = head & 0x1f;// 5 bit

//  def rtpPacket(): Seq[RtpPacket] = {
//    if (nalu.length > H264NALU.DEFAULT_MTU_LEN) {
//      Seq()
//    } else {
//      Seq(singlePacket())
//    }
//  }

//  def singlePacket(): RtpPacket = {
//
//  }
//
//  def singlePacket(): RtpPacket = {
//
//  }

//  private def calcFUIndicator(): Byte = {
//
//  }
//
//  private def calcFUHeader(): Byte = {
//
//  }
}

object H264NALU {
  val DEFAULT_MTU_LEN = 1400
  val FUIndicatorType = 28
}
