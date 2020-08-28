package com.getouo.gb.scl.server.handler

import com.getouo.gb.scl.model.RtspOptionsRequest
import com.getouo.gb.scl.server.{GBStreamPublisher, UdpPusher}
import com.getouo.gb.scl.stream.GB28181PlayStream
import com.getouo.gb.scl.util.{ChannelUtil, SpringContextUtil}
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.util.concurrent.Future

/**
 * OPTION方法
 *
 * 方法格式如下所示，OPTION方法由客户端发起，格式为:
 *
 * OPTION URL RTSP版本号
 * CSeq: CSeq号(每发一条方法加一)
 * User-Agent:
 *
 * -----------------------------
 *
 * 服务端给反馈，格式为:
 *
 * RTSP版本号 状态码 状态字段
 * CSeq: 服务端发的CSeq号
 * Date: 时间 时区
 * Public: 可用方法
 */
class RtspOptionsHandler extends SimpleChannelInboundHandler[RtspOptionsRequest] {
  override def channelRead0(ctx: ChannelHandlerContext, i: RtspOptionsRequest): Unit = {
    val response = i.defaultResponse()
    System.err.println(
      s"""
         |回复:
         |${response.stringMessage()}
         |-----------------------------------
         |""".stripMargin)
    ctx.writeAndFlush(response).addListener((f: Future[_]) => println(
      s"""
         |fasong option、：
         |${f.isSuccess}
         |""".stripMargin))
  }
}

