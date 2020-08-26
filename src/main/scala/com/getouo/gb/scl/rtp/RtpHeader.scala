package com.getouo.gb.scl.rtp

import java.nio.ByteBuffer

import com.getouo.gb.util.{ByteBitAccessor, LongBitAccessor}
import io.pkts.buffer.{Buffer, Buffers}

/*
 *
 *    0                   1                   2                   3
 *    7 6 5 4 3 2 1 0|7 6 5 4 3 2 1 0|7 6 5 4 3 2 1 0|7 6 5 4 3 2 1 0
 *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *   |V=2|P|X|  CC   |M|     PT      |       sequence number         |
 *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *   |                           timestamp                           |
 *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *   |           synchronization source (SSRC) identifier            |
 *   +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
 *   |            contributing source (CSRC) identifiers             |
 *   :                             ....                              :
 *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *
 */
case class RtpHeader(bytes: Array[Byte]) {
  if (bytes.length != 12) throw new RuntimeException("RtpHeader header size must = 12, but error: " + bytes.length)
//  private val buffer: ByteBuffer = ByteBuffer.wrap(bytes)
  private val buffer: Buffer = Buffers.wrap(bytes)
  private val byte0: Byte = buffer.readByte() // csrcLen:4; extension:1; padding:1; version:2
  private val byte1: Byte = buffer.readByte() // payloadType:7; marker:1;

  val sequenceNumber: Int = buffer.readUnsignedShort()
  if (sequenceNumber < 0) throw new RuntimeException("sequenceNumber < 0 error")
  val timestamp: Long = buffer.readUnsignedInt()
  val ssrcIdentifier: Long = buffer.readUnsignedInt()

  val version: Int = byte0 >> 6
  val padding: Boolean = ByteBitAccessor.hasBit(byte0, RtpHeader.bitByte(5))
  val extension: Boolean = ByteBitAccessor.hasBit(byte0, RtpHeader.bitByte(4))
  val csrcLen: Int = ((byte0 << 4).byteValue() & 0xff) >> 4

  val marker: Boolean = ByteBitAccessor.hasBit(byte1, RtpHeader.bitByte(7))
  val payloadType: Int = (0xff & (byte1 << 1).byteValue()) >> 1
}

object RtpHeader {

  val VERSION = 2
  val RTP_PAYLOAD_TYPE_H264 = 96

  def from(version: Int, padding: Boolean, extension: Boolean,
            csrcLen: Int, marker: Boolean, payloadType: Byte,
            sequenceNumber: Short,
            timestamp: Int,
            ssrcIdentifier: Int): RtpHeader = {
    val buffer = ByteBuffer.allocateDirect(12)
    val byte0 = (version.byteValue() << 6) | ((if (padding) 1 else 0).byteValue() << 5) | ((if (extension) 1 else 0).byteValue() << 4) | (csrcLen.byteValue() << 4 >> 4)
    buffer.put(byte0.toByte)
    val byte1 = ((if (marker) 1 else 0).byteValue() << 7) | (payloadType << 1 >> 1)
    buffer.put(byte1.toByte)
    buffer.putShort(sequenceNumber)
    buffer.putInt(timestamp)
    buffer.putInt(ssrcIdentifier)
    buffer.flip()
    val bytes = new Array[Byte](12)
    buffer.get(bytes)
//    new RtpHeader(buffer.array())
    new RtpHeader(bytes)
  }

  val start0Byte: Byte = 1

  def bitByte(index: Int): Byte = {
    if (index < 0 || index > 7) 0
    else (start0Byte << index).byteValue()
  }

  def build(): Array[Byte] = {
    val buffer = ByteBuffer.allocateDirect(12)
    buffer.array()
  }
}
