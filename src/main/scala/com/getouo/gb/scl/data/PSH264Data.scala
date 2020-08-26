package com.getouo.gb.scl.data

import java.nio.ByteBuffer
import java.util.Optional

import io.pkts.buffer.Buffers

trait PSH264Data extends ISourceData

class PSHeaders
/**
 *
 * PS流传输格式:
 * 1、视频关键帧的封装 RTP(12) + PS header(4) + PS system header(4) + PS system Map(4) + PES header(4) +h264 data(<1400)
 * 2、视频非关键帧的封装 RTP +PS header + PES header + h264 data
 * 3、音频帧的封装: RTP + PES header + G711
 *
 */
object PSHeaders {
  val PS_HEADER: Array[Byte] = ByteBuffer.allocate(4).putInt(0x000001BA).array()
  val PS_SYSTEM_HEADER_I_FRAME: Array[Byte] = ByteBuffer.allocate(4).putInt(0x000001BB).array()
  val PS_SYSTEM_MAP: Array[Byte] = ByteBuffer.allocate(4).putInt(0x000001BC).array()
  val PS_PES_HEADER: Array[Byte] = ByteBuffer.wrap(Array[Byte](0x00, 0x00, 0x01)).array()
  val PS_VIDEO_PES_HEADER: Array[Byte] = ByteBuffer.allocate(4).putInt(0x000001E0).array()
//  val PS_P_FRAME_HEADER: Array[Byte] = ByteBuffer.allocate(4).putInt(0x000001BA).array()
  val PS_AUDIO_PES_HEADER: Array[Byte] = ByteBuffer.allocate(4).putInt(0x000001C0).array()

  def main(args: Array[String]): Unit = {
    println(

    PSHeaders.getClass
    )
  }
}