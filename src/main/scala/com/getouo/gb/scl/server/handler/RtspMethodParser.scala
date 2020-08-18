package com.getouo.gb.scl.server.handler

import java.net.InetSocketAddress
import java.util

import com.getouo.gb.scl.model._
import com.getouo.gb.scl.util.{ChannelUtil, ConstVal, LogSupport}
import io.netty.channel.{Channel, ChannelHandlerContext}
import io.netty.handler.codec.MessageToMessageDecoder
import io.netty.handler.codec.http.{DefaultHttpRequest, HttpHeaders}
import io.netty.handler.codec.rtsp.{RtspHeaderNames, RtspMethods}

/**
 * RTSP交互流程
 *
 * C表示rtsp客户端,S表示rtsp服务端
 *
 * >>> C->S:OPTION request //询问S有哪些指令可用
 *
 * <<< S->C:OPTION response //S回应信息中包括提供的所有可用指令
 *
 * >>> C->S:DESCRIBE request //要求得到S提供的媒体初始化描述信息
 *
 * <<< S->C:DESCRIBE response //S回应媒体初始化描述信息，主要是sdp
 *
 * >>> C->S:SETUP request //设置会话的属性，以及传输模式，提醒S建立会话
 *
 * <<< S->C:SETUP response //S建立会话，返回会话标识符，以及会话相关信息
 *
 * >>> C->S:PLAY request //C请求播放
 *
 * <<< S->C:PLAY response //S回应该请求的信息
 *
 * <<< S->C:发送流媒体数据
 *
 * >>> C->S:TEARDOWN request //C请求关闭会话
 *
 * <<< S->C:TEARDOWN response //S回应该请求
 */
class RtspMethodParser extends MessageToMessageDecoder[DefaultHttpRequest] with LogSupport {

  override def decode(ctx: ChannelHandlerContext, i: DefaultHttpRequest, list: util.List[AnyRef]): Unit = {

    val headers = i.headers()
    logger.warn(
      s"""
         |+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         ||收到【${i.method()}】↓↓↓↓↓↓↓↓↓↓ 【 uri=${i.uri()} 】clientPort=${ChannelUtil.castSocketAddr(ctx.channel().remoteAddress()).getPort}
         ||headers: $headers
         ||+-+-+-+-+-+-+-+-+-+-+-+-+-+↑↑↑↑↑收到↑↑↑↑↑+-+-+-+-+-+-+-+-+-+-+-+-+-+
         |""".stripMargin)

    val CSeq: Int = headers.getInt(RtspHeaderNames.CSEQ)
    RtspHeaderNames.USER_AGENT
    i.method() match {
      case RtspMethods.OPTIONS => list.add(RtspOptionsRequest(i.uri(), CSeq))
      case RtspMethods.DESCRIBE => list.add(RtspDescribeRequest(i.uri(), CSeq, headers.get(RtspHeaderNames.USER_AGENT), headers.get(RtspHeaderNames.ACCEPT)))
      case RtspMethods.SETUP => list.add(RtspSetupRequest(i.uri(), CSeq, deSetupTrans(ctx.channel(), headers)))
      case RtspMethods.PLAY => list.add(RtspPlayRequest(i.uri(), CSeq, headers.get(RtspHeaderNames.SESSION).toLong, headers.get(RtspHeaderNames.RANGE)))
      case RtspMethods.TEARDOWN => list.add(RtspTeardownRequest(i.uri(), CSeq, headers.get(RtspHeaderNames.SESSION).toLong))
      case _ => logger.warn(s"收到未知的消息: $i")
    }
  }

  private def deSetupTrans(c: Channel, h: HttpHeaders): ConstVal.RtpTransType = {
    val address: InetSocketAddress = ChannelUtil.castSocketAddr(c.localAddress())
    val sIp = address.getAddress.getHostAddress
    val sPort = address.getPort

    val clientAddress = ChannelUtil.castSocketAddr(c.remoteAddress())
    // RTP/AVP;unicast;client_port=54492-54493\r\n
    val str = h.get(RtspHeaderNames.TRANSPORT)
    val r = ".*RTP/([^;]*);.*".r
    val r(tt) = str
    if (tt == "AVP") {
      val rp = ".*client_port=([0-9]+)-.*".r
      val rp(cPort) = str
      while (RequestHandler.server.rtpUDPServer.channel == null) {
        Thread.sleep(1)
      }
      ConstVal.RtpOverUDP(sIp, clientAddress.getAddress.getHostAddress, cPort.toInt).updateServerPort(
        ChannelUtil.castSocketAddr(RequestHandler.server.rtpUDPServer.channel.localAddress()).getPort
      )
    } else if (tt == "AVP/TCP") {
      ConstVal.RtpOverTCP(str)
    } else {
      ConstVal.UnknownTransType
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    cause.printStackTrace()
  }
}
