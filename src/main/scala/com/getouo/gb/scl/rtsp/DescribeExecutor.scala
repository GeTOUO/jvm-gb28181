package com.getouo.gb.scl.rtsp

import java.io.File
import java.util.Base64

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.PooledByteBufAllocator
import com.getouo.gb.scl.model.{SDPInfo, SDPMediaInfo, SDPSessionInfo}
import com.getouo.gb.scl.service.DeviceService
import com.getouo.gb.scl.stream.{FileSourceId, GB28181PlayStream, GBSourceId, SourceId}
import com.getouo.gb.scl.util.{ChannelUtil, ConstVal, LogSupport, RtpSession, RtpUrlUtil, Session, SpringContextUtil}
import io.netty.channel.Channel
import io.netty.handler.codec.http.{FullHttpRequest, FullHttpResponse}
import io.netty.handler.codec.rtsp.{RtspHeaderNames, RtspResponseStatuses}

class DescribeExecutor(val channel: Channel, val request: FullHttpRequest) extends RtspExecuteable[FullHttpResponse] with LogSupport {
  override def call(): FullHttpResponse = {

    val uri = request.uri()
    val uriAccessor = new RtspUriAccessor(uri)
    uriAccessor
    val sourceName = RtpUrlUtil.getSourceName(uri)


    buildSourceId(sourceName) match {
      case None =>
        logger.error(s"未找到资源 $sourceName 暂未处理")
        toResponse(RtspResponseStatuses.NOT_FOUND)
      case Some(sourceId) =>
        installSession(sourceId, channel)
        val sdpMessage = loadSdp(sourceId, uri, channel)
        val payload: ByteBuf = ByteBufUtil.writeAscii(PooledByteBufAllocator.DEFAULT, sdpMessage.text())
        val response = payloadResponse(payload = payload)
        response.headers().add(RtspHeaderNames.CONTENT_TYPE, "application/sdp")
        response
    }
  }

  private def loadSdp(sessionId: SourceId, path: String, channel: Channel): SDPInfo = {

    val localIp: String = ChannelUtil.localIp(channel)
    val targetIp: String = ChannelUtil.remoteIp(channel)
    //    val aGroup: Seq[(Char, String)] = Seq(('a', "rtpmap:96 H264/90000"), ('a', "framerate:25"), ('a', "control:track0"))
    val aGroup: Seq[(Char, String)] = Seq(('a', "rtpmap:96 H264/90000"), ('a', "framerate:25"), ('a', "control:trackID=0"))

    val mediaInfo = SDPMediaInfo(mediaFormat = 96, aGroup = aGroup)
    //    val mediaInfo = SDPMediaInfo(rtpTransType = ConstVal.RtpOverTCP(), mediaFormat = 96, aGroup = aGroup)
    //    SDPInfo(SDPSessionInfo(sessionIdIsNTPTimestamp = TimeUtil.currentMicTime(), serverIpAddress = localIp), Seq(mediaInfo))
    SDPInfo(SDPSessionInfo(sessionIdIsNTPTimestamp = sessionId.idHash(), serverIpAddress = localIp), Seq(mediaInfo))
  }

  def installSession(sourceId: SourceId, channel: Channel): Unit = {
    sourceId match {
      case fid@FileSourceId(file, setupTime) =>
        Session.updateChannelSession(channel, opt => Some({
          opt match {
            case Some(value: RtpSession) => value.copy(id = fid)
            case None => RtpSession(fid, pt = ConstVal.UnknownTransport, None, None)
          }
        }))
      case gid@GBSourceId(deviceId, channelId, setupTime) =>
        Session.updateChannelSession(channel, opt => Some({
          opt match {
            case Some(value: RtpSession) => value.copy(id = gid)
            case None => RtpSession(gid, pt = ConstVal.UnknownTransport, None, None)
          }
        }))
      case _ =>
    }
  }

  private def buildSourceId(sourceName: String): Option[SourceId] = {
    val file = new File(s"/$sourceName")
    val upCaseName = sourceName.toUpperCase
    if ((upCaseName.endsWith(".H264") || upCaseName.endsWith(".264")) && file.exists()) {
      Some(FileSourceId(file.getCanonicalPath, System.currentTimeMillis()))
    } else {
      SpringContextUtil.getBean[DeviceService](clazz = classOf[DeviceService]) match {
        case None => throw new Exception(s"无法获取 DeviceService")
        case Some(service) => service.findDevice(sourceName).map(device => GBSourceId(device.id, device.id))
      }
    }
  }
}
