package com.getouo.gb.scl.server.handler

import java.net.InetAddress

import com.getouo.gb.scl.model.{RtspDescribeRequest, RtspDescribeResponse, SDPInfo, SDPSessionInfo}
import com.getouo.gb.scl.util.LogSupport
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import org.apache.commons.net.ntp.NTPUDPClient
import sun.util.calendar.LocalGregorianCalendar.Date

class RtspDescribeHandler extends SimpleChannelInboundHandler[RtspDescribeRequest] with LogSupport {
  override def channelRead0(channelHandlerContext: ChannelHandlerContext, i: RtspDescribeRequest): Unit = {
    logger.info(s"准备资源: ${i.url} 的 describe")
//    i.url
//    RtspDescribeResponse(i.CSeq)
//    i.defaultResponse()
  }

  private def loadSdp(path: String): SDPInfo = {


//    new Date()
//    new SDPInfo(new SDPSessionInfo(),)
    null
  }
}

object Tim {
  def main(args: Array[String]): Unit = {
    val timeClient: NTPUDPClient  = new NTPUDPClient()
    val timeInfo = timeClient.getTime(InetAddress.getByName("ntp.baijinshan.cn"))
    val timeStamp = timeInfo.getMessage.getTransmitTimeStamp
    println(timeStamp)
    println(timeStamp.getTime)
    println(timeStamp.getDate)
    println(timeStamp.getDate.getTime)
    println(System.currentTimeMillis())
  }
}
