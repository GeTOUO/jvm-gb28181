package com.getouo.gb.scl.server

class RtpAndRtcpServerGroup(val rtpPort: Int, isUDP: Boolean = true) {

  System.err.println("rtp server port:" + rtpPort)
  private val rtpUDPServer = new RtpUDPServer(rtpPort).init()
  private val rtcpUDPServer = new RtcpUDPServer(rtpPort + 1).init()
}
