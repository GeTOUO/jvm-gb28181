package com.getouo.gb.scl.server.handler

import java.io.File

import com.getouo.gb.scl.model.{RtspDescribeRequest, SDPInfo, SDPMediaInfo, SDPSessionInfo}
import com.getouo.gb.scl.util.{ChannelUtil, ConstVal, LogSupport, RtpSession, RtpUrlUtil, Session, TimeUtil}
import io.netty.channel.{Channel, ChannelHandlerContext, SimpleChannelInboundHandler}

class RtspDescribeHandler extends SimpleChannelInboundHandler[RtspDescribeRequest] with LogSupport {
  override def channelRead0(ctx: ChannelHandlerContext, i: RtspDescribeRequest): Unit = {

    val sourceName = RtpUrlUtil.getSourceName(i.url)
    val upperCaseName = sourceName.toUpperCase
    if (upperCaseName.endsWith(".H264") || upperCaseName.endsWith(".264")) {
      if (new File(s"/$upperCaseName").exists()) {
        logger.info(s"准备资源: ${sourceName} 的 describe")
        Session.updateChannelSession(ctx.channel(), opt => {
          Some(opt match {
            case Some(value: RtpSession) => value.copy(id = s"/$upperCaseName")
            case None => RtpSession(s"/$upperCaseName", pt = ConstVal.UnknownTransType, None, None)
          })
        })
        Session.rtpSession(ctx.channel()) match {
          case Some(value) => logger.error(s"session install成功 on client_port=${ChannelUtil.castSocketAddr(ctx.channel().remoteAddress()).getPort}")
          case None => logger.error(s"session install失败！！！ on client_port=${ChannelUtil.castSocketAddr(ctx.channel().remoteAddress()).getPort}")
        }
        val info = loadSdp(i.url, ctx.channel())
        ctx.writeAndFlush(i.defaultResponse(info))
      } else {
        logger.error(s"资源 $sourceName 不存在")
        ctx.writeAndFlush(i.resp404(s"资源 $sourceName 不存在"))
      }
    } else {
      logger.error(s"在线资源 $sourceName 暂未处理")
      ctx.writeAndFlush(i.resp404(s"在线资源 $sourceName 暂未处理"))
    }
  }

  private def loadSdp(path: String, channel: Channel): SDPInfo = {
    val localIp: String = ChannelUtil.localIp(channel)
    val targetIp: String = ChannelUtil.remoteIp(channel)
    //    val aGroup: Seq[(Char, String)] = Seq(('a', "rtpmap:96 H264/90000"), ('a', "framerate:25"), ('a', "control:track0"))
    val aGroup: Seq[(Char, String)] = Seq(('a', "rtpmap:96 H264/90000"), ('a', "framerate:25"), ('a', "control:trackID=0"))

    val mediaInfo = SDPMediaInfo(rtpTransType = ConstVal.RtpOverUDP(localIp, targetIp, 0), mediaFormat = 96, aGroup = aGroup)
//    val mediaInfo = SDPMediaInfo(rtpTransType = ConstVal.RtpOverTCP(), mediaFormat = 96, aGroup = aGroup)
    SDPInfo(SDPSessionInfo(sessionIdIsNTPTimestamp = TimeUtil.currentMicTime(), serverIpAddress = localIp), Seq(mediaInfo))
  }
}