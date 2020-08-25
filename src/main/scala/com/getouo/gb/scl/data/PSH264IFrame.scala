package com.getouo.gb.scl.data

import io.pkts.buffer.Buffers

case class PSH264IFrame(bytes: Array[Byte]) extends PSH264Data {
  val version: Int = Buffers.wrap(bytes.take(2)).readUnsignedShort()

}

case class PSH264PFrame(bytes: Array[Byte]) extends PSH264Data {

}

case class PSH264Audio() extends PSH264Data {

}

object PSH264IFrame {

}