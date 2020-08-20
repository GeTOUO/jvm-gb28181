package com.getouo.gb.scl.sip

object SipMessageTemplate {

  def inviteRequest(deviceId: String, deviceLocalIp: String, deviceLocalPort: Int, callId: String,
                    serverId: String, serverIp: String, serverPort: Int, ssrc: String, contentLength: Int): String = {
    s"""INVITE sip:${deviceId}@${serverId} SIP/2.0
       |Call-ID: ${callId}
       |CSeq: 101 INVITE
       |From: <sip:${serverId}@${serverIp}:${serverPort}>;tag=live
       |To: "${deviceId}" <sip:${deviceId}@${deviceLocalIp}:${deviceLocalPort}>
       |Via: SIP/2.0/UDP ${serverIp}:${serverPort};branch=branchlive
       |Max-Forwards: 70
       |Content-Type: Application/sdp
       |Contact: <sip:${serverId}@${serverIp}:${serverPort}>
       |Supported: 100re1
       |Subject: ${deviceId}:010000${ssrc},${serverId}:0
       |User-Agent: fyl
       |Content-Length: ${contentLength}
       |
       |""".stripMargin
  }
}
