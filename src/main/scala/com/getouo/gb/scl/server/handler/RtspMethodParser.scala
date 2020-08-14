package com.getouo.gb.scl.server.handler

import java.net.InetSocketAddress
import java.util

import com.getouo.gb.scl.model._
import com.getouo.gb.scl.util.{ConstVal, LogSupport}
import io.netty.channel.{Channel, ChannelHandlerContext}
import io.netty.handler.codec.MessageToMessageDecoder
import io.netty.handler.codec.http.{DefaultHttpRequest, HttpHeaders}

class RtspMethodParser extends MessageToMessageDecoder[DefaultHttpRequest] with LogSupport {

  override def decode(ctx: ChannelHandlerContext, i: DefaultHttpRequest, list: util.List[AnyRef]): Unit = {

    val headers = i.headers()
    val CSeq: Int = headers.getInt("CSeq")
    i.method().name().trim.toUpperCase match {
      case "OPTIONS" => list.add(RtspOptionsRequest(i.uri(), CSeq))
      case "DESCRIBE" => list.add(RtspDescribeRequest(i.uri(), CSeq, headers.get("User-Agent"), headers.get("Accept")))
      case "SETUP" => list.add(RtspSetupRequest(i.uri(), CSeq, deSetupTrans(ctx.channel(), headers)))
      case "PLAY" => list.add(RtspPlayRequest(i.uri(), CSeq, headers.getInt("Session").longValue(), headers.get("Range")))
      case "TEARDOWN" => list.add(RtspTeardownRequest(i.uri(), CSeq, headers.getInt("Session").longValue()))
      case _ => logger.warn(s"收到未知的消息: $i")
    }
  }

  private def deSetupTrans(c: Channel, h: HttpHeaders): ConstVal.RtpTransType = {
    val address: InetSocketAddress = c.localAddress().asInstanceOf[InetSocketAddress]
    val sIp = address.getAddress.getHostAddress
    val sPort = address.getPort

    val clientAddress = c.remoteAddress().asInstanceOf[InetSocketAddress]
    // RTP/AVP;unicast;client_port=54492-54493\r\n
    val str = h.get("Transport")
    val r = ".*RTP/(.*);.*".r
    val r(tt) = str
    if (tt == "AVP") {
      val rp = ".*client_port=([0-9]+)-.*".r
      val rp(cPort) = str
      ConstVal.RtpOverUDP(sIp, 0, clientAddress.getAddress.getHostAddress, cPort.toInt)
    } else if (tt == "AVP/TCP") {
      ConstVal.RtpOverTCP()
    } else {
      ConstVal.UnknownTransType
    }
  }
}
