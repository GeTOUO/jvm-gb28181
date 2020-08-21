package com.getouo.gb.scl.sip

import gov.nist.javax.sip.header.SIPHeaderNames
import io.pkts.buffer.Buffers
import io.pkts.packet.sip.address.SipURI
import io.pkts.packet.sip.header.{CSeqHeader, CallIdHeader, ContactHeader, FromHeader, ToHeader, ViaHeader}
import io.pkts.packet.sip.header.impl.{SipHeaderImpl, ViaHeaderImpl}
import io.pkts.packet.sip.impl.SipRequestImpl
import io.pkts.packet.sip.{SipMessage, SipRequest, SipResponse}

object SipMessageTemplate {

//  def inviteRequest(deviceId: String, deviceLocalIp: String, deviceLocalPort: Int, callId: String,
//                    serverId: String, serverIp: String, serverPort: Int, ssrc: String, contentLength: Int): String = {
//    s"""INVITE sip:${deviceId}@${serverId} SIP/2.0
//       |Call-ID: ${callId}
//       |CSeq: 101 INVITE
//       |From: <sip:${serverId}@${serverIp}:${serverPort}>;tag=live
//       |To: "${deviceId}" <sip:${deviceId}@${deviceLocalIp}:${deviceLocalPort}>
//       |Via: SIP/2.0/UDP ${serverIp}:${serverPort};branch=branchlive
//       |Max-Forwards: 70
//       |Content-Type: Application/sdp
//       |Contact: <sip:${serverId}@${serverIp}:${serverPort}>
//       |Supported: 100re1
//       |Subject: ${deviceId}:010000${ssrc},${serverId}:0
//       |User-Agent: fyl
//       |Content-Length: ${contentLength}
//       |
//       |""".stripMargin
//  }

  def inviteRequest(channel: String, deviceIp: String, devicePort: Int, sipServerId: String,
                    callId: String, sipIp: String, sPort: Int, fromTag: String, mediaServerIp: String, mediaServerPort: Int): SipRequest = {
    val tcpSsrc = s"0${sipServerId.substring(3, 8)}0000"
    val str =
      s"""INVITE sip:$channel@$deviceIp:$devicePort SIP/2.0
         |Call-ID: $callId
         |CSeq: 1 INVITE
         |From: <sip:$sipServerId@${sipServerId.take(10)}>;tag=$fromTag
         |To: <sip:$channel@${sipServerId.take(8)}>
         |Via: SIP/2.0/UDP $deviceIp:$devicePort;rport
         |Max-Forwards: 70
         |Contact: <sip:$sipServerId@$sipIp:$sPort>
         |Content-Type: Application/SDP
         |
         |v=0
         |o=$sipServerId 0 0 IN IP4 $mediaServerIp
         |s=Play
         |c=IN IP4 $mediaServerIp
         |t=0 0
         |m=video $mediaServerPort TCP/RTP/AVP 96 98 97
         |a=sendrecv
         |a=rtpmap:96 PS/90000
         |a=rtpmap:98 H264/90000
         |a=rtpmap:97 MPEG4/90000
         |a=setup:passive
         |a=connection:new
         |y=$tcpSsrc
         |
         |""".stripMargin

    val message = SipMessage.frame(str).toRequest
    val buffer = new SipHeaderImpl(Buffers.wrap(SIPHeaderNames.CONTENT_LENGTH), Buffers.wrap(message.getRawContent.getArray.length))
    message.addHeader(buffer)
    message
  }

  def generateAck(response: SipResponse): SipRequest = {
    val contact = response.getContactHeader
    val requestURI = contact.getAddress.getURI.asInstanceOf[SipURI]
    val to = response.getToHeader
    val from = response.getFromHeader
    // The contact of the response is where the remote party wishes
    // to receive future request. Since an ACK is a "future", or sub-sequent, request,
    // the request-uri of the ACK has to be whatever is in the
    // contact header of the response.

    // Since this is an ACK, the cseq should have the same cseq number as the response,
    // i.e., the same as the original INVITE that we are ACK:ing.
    val cseq = CSeqHeader.`with`.cseq(response.getCSeqHeader.getSeqNumber).method("ACK").build
    val callId = response.getCallIDHeader

    // If there are Record-Route headers in the response, they must be
    // copied over as well otherwise the ACK will not go the correct
    // path through the network.
    // TODO

    // we also have to create a new Via header and as always, when creating
    // via header we need to fill out which ip, port and transport we are
    // coming in over. In SIP, unlike many other protocols, we can use
    // any transport protocol and it can actually change from message
    // to message but in this simple example we will just use the
    // same last time so we will only have to generate a new branch id
//    val via: ViaHeader = response.getViaHeader.clone.asInstanceOf[ViaHeader]
    val via: ViaHeader = response.getViaHeader
    via.asInstanceOf[ViaHeaderImpl].clone.setBranch(ViaHeader.generateBranch)

    // now we have all the pieces so let's put it together
    val builder = SipRequest.ack(requestURI)
    builder.from(from)
    builder.to(to)
    builder.callId(callId)
    builder.cseq(cseq)
    builder.via(via)
    builder.build
  }

}
