package com.getouo.gb.scl.rtp

class H264Accessor {

}

object H264Accessor {
  def start(buf: Array[Byte]): Boolean = {
    start3Pos(buf) ||
    if (buf.length < 3) false
    else if (buf.length < 4) {
      return buf()
    }
  }

  private def start3Pos(buf: Array[Byte], sl: Int = 4): Boolean = {
    if (buf.length < sl) return false
    
    buf(0) == 0 && buf(1) == 0 &&buf(2) == 0
  }
}
