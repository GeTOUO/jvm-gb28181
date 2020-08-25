package com.getouo.gb.scl.sdp

import io.pkts.buffer.Buffers

object SdpParser {
  def from(content: String): SessionDescription = {
    val buffer = Buffers.wrap(content)

    null
  }
}
