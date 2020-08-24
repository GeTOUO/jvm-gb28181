package com.getouo.gb.scl.server.handler

import java.io.File

import com.getouo.gb.scl.model.{RtspDescribeRequest, SDPInfo, SDPMediaInfo, SDPSessionInfo}
import com.getouo.gb.scl.sdp.{SdpConnectionValue, SdpOriginValue}
import com.getouo.gb.scl.service.DeviceService
import com.getouo.gb.scl.stream.{FileSourceId, GB28181PlayStream, GBSourceId, SourceId}
import com.getouo.gb.scl.util._
import io.netty.channel.{Channel, ChannelHandlerContext, SimpleChannelInboundHandler}
import io.pkts.buffer.Buffers

class RtspDescribeHandler extends SimpleChannelInboundHandler[RtspDescribeRequest] with LogSupport {

  override def channelRead0(ctx: ChannelHandlerContext, i: RtspDescribeRequest): Unit = {

    val sourceName = RtpUrlUtil.getSourceName(i.url)
    val upperCaseName = sourceName.toUpperCase
    val sourceIdOpt = buildSourceId(sourceName)

    sourceIdOpt match {
      case None =>
        logger.error(s"未找到资源 $sourceName 暂未处理")
        ctx.writeAndFlush(i.resp404(s"未找到资源 $sourceName 暂未处理"))
      case Some(sourceId) =>
        installSession(sourceId, ctx.channel())

        sourceId match {
          case FileSourceId(file, setupTime) =>
            val info = loadSdp(i.url, ctx.channel())
            ctx.writeAndFlush(i.defaultResponse(info))
          case gid@GBSourceId(deviceId, channelId, setupTime) =>
            GB28181PlayStream.byIdOpt(gid) match {
              case None =>
                logger.error(s"设备【$deviceId】未加载或未开始点播")
                ctx.writeAndFlush(i.resp404(s"设备【$deviceId】未加载或未开始点播"))
              case Some(playStream) =>
                val sdpStr = playStream.source.sdpInfo.get()
                val resp = i.defaultResponse(loadGBSdp(sdpStr, ctx.channel()))
                ctx.writeAndFlush(resp)
            }
          case _ =>
            logger.error(s"未知资源 $sourceName 暂未处理")
            ctx.writeAndFlush(i.resp404(s"未知资源 $sourceName 暂未处理"))
        }
    }
  }

  def installSession(sourceId: SourceId, channel: Channel): Unit = {
    sourceId match {
      case fid@FileSourceId(file, setupTime) =>
        Session.updateChannelSession(channel, opt => Some({
          opt match {
            case Some(value: RtpSession) => value.copy(id = fid)
            case None => RtpSession(fid, pt = ConstVal.UnknownTransType, None, None)
          }
        }))
      case gid@GBSourceId(deviceId, channelId, setupTime) =>
        Session.updateChannelSession(channel, opt => Some({
          opt match {
            case Some(value: RtpSession) => value.copy(id = gid)
            case None => RtpSession(gid, pt = ConstVal.UnknownTransType, None, None)
          }
        }))
      case _ =>
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

  private def loadGBSdp(sdpStr: String, channel: Channel): String = {
    val localIp: String = ChannelUtil.localIp(channel)
    val targetIp: String = ChannelUtil.remoteIp(channel)
    var buf = sdpStr
    val buffer = Buffers.wrap(sdpStr)
    while (buffer.hasReadableBytes) {
      val lineKV = buffer.readLine().toString.split("=")
      if (lineKV.length == 2) {
        val strKey = lineKV(0)
        val lineValue = lineKV(1)
        if (strKey.equalsIgnoreCase("o")) {
          buf = buf.replace(lineValue, SdpOriginValue.from(lineValue).copy(addr = localIp).toString)
        }
        if (strKey.equalsIgnoreCase("c")) {
          buf = buf.replace(lineValue, SdpConnectionValue.from(lineValue).copy(addr = localIp).toString)
        }
      }
    }
    buf
  }

  private def buildSourceId(sourceName: String): Option[SourceId] = {
    val file = new File(s"/$sourceName")
    val upCaseName = sourceName.toUpperCase
    if ((upCaseName.endsWith(".H264") || upCaseName.endsWith(".264")) && file.exists()) {
      Some(FileSourceId(file.getName, System.currentTimeMillis()))
    } else {
      SpringContextUtil.getBean[DeviceService](clazz = classOf[DeviceService]) match {
        case None => throw new Exception(s"无法获取 DeviceService")
        case Some(service) => service.findDevice(sourceName).map(device => GBSourceId(device.id, device.id))
      }
    }

  }
}