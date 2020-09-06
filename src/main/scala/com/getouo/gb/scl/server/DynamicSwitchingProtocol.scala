package com.getouo.gb.scl.server

import java.util
import java.util.concurrent.TimeUnit

import com.getouo.gb.HttpRequestHandler
import com.getouo.gb.scl.exception.{ProtocolParseException, ProtocolUnSupportException}
import com.getouo.gb.scl.server.handler.{ProxyHandler, RtspDescribeHandler, RtspMethodParser, RtspOptionsHandler, RtspPlayHandler, RtspResponseEncoder, RtspSetupHandler, RtspTailHandler}
import com.getouo.gb.scl.util.LogSupport
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.http.{HttpObjectAggregator, HttpServerCodec, HttpVersion}
import io.netty.handler.codec.rtsp.{RtspDecoder, RtspEncoder, RtspVersions}
import io.netty.handler.codec.string.{StringDecoder, StringEncoder}
import io.netty.handler.logging.{LogLevel, LoggingHandler}
import io.netty.handler.stream.ChunkedWriteHandler
import io.netty.handler.timeout.IdleStateHandler
import io.pkts.buffer.{Buffer, Buffers}
import io.sipstack.netty.codec.sip.{SipMessageEncoder, SipMessageStreamDecoder}


object SipVersions {
  val SIP_2_0: HttpVersion = HttpVersion.valueOf("SIP/2.0")

  def valueOf(text: String): HttpVersion = {
    val str = text.trim().toUpperCase()
    if ("SIP/2.0" == text) return SIP_2_0
    HttpVersion.valueOf(str)
  }
}

class DynamicSwitchingProtocol extends ByteToMessageDecoder with LogSupport {

  override def decode(channelHandlerContext: ChannelHandlerContext, byteBuf: ByteBuf, list: util.List[AnyRef]): Unit = {
    byteBuf.markReaderIndex()
    val bytes: Array[Byte] = new Array[Byte](byteBuf.readableBytes())
    byteBuf.readBytes(bytes)
    byteBuf.resetReaderIndex()
    val buffer: Buffer = Buffers.wrap(bytes)
    val rawInitialLineUnits = buffer.readLine().toString.split(DynamicSwitchingProtocol.SP).map(_.trim)
    if (!buffer.hasReadableBytes) {
      return
    }
    if (rawInitialLineUnits.length != 3) {
      channelHandlerContext.close()
      throw new ProtocolParseException("读出的协议头有异常 expected space")
    }

    val protocol = HttpVersion.valueOf(rawInitialLineUnits(2).toUpperCase)
    protocol match {
      case HttpVersion.HTTP_1_0 | HttpVersion.HTTP_1_1 =>
        switchToHttpAndWebsocket(channelHandlerContext)
      case RtspVersions.RTSP_1_0 => switchToRtsp(channelHandlerContext)
      case SipVersions.SIP_2_0 => switchToSip(channelHandlerContext)
      case unSupportProtocol =>
        channelHandlerContext.close()
        throw new ProtocolUnSupportException(unSupportProtocol.text())
    }
    logger.info(s"switch protocol to ${protocol.protocolName()}")

    list.add(byteBuf.retain())
    channelHandlerContext.pipeline.remove(this)
  }

  def switchToSip(ctx: ChannelHandlerContext): Unit = {
    val pipeline = ctx.pipeline()
    pipeline.addLast("decoder", new SipMessageStreamDecoder)
    pipeline.addLast("encoder", new SipMessageEncoder)
    pipeline.addLast("sip dispatcher", new ProxyHandler)
  }

  def switchToRtsp(ctx: ChannelHandlerContext): Unit = {
    val pipeline = ctx.pipeline()
    pipeline
      .addLast(new RtspDecoder) // 添加netty自带的rtsp消息解析器
      .addLast(new RtspEncoder) // 添加netty自带的rtsp消息解析器
      .addLast("aggregator", new HttpObjectAggregator(1048576))
      .addLast(new StringEncoder()) // 支持直接发送字符串
      .addLast(new RtspResponseEncoder()) // 支持直接发送RtspResponse
      .addLast(new StringDecoder()) // 支持收string

      .addLast(new RtspMethodParser) // 上一步将消息解析完成之后, 再交给自定义的处理器

      .addLast(new RtspOptionsHandler) // 上一步将消息解析完成之后, 再交给自定义的处理器
      .addLast(new RtspDescribeHandler) // 上一步将消息解析完成之后, 再交给自定义的处理器
      .addLast(new RtspSetupHandler) // 上一步将消息解析完成之后, 再交给自定义的处理器
      .addLast(new RtspPlayHandler) // 上一步将消息解析完成之后, 再交给自定义的处理器
      .addLast(new RtspTailHandler) // 上一步将消息解析完成之后, 再交给自定义的处理器
    //              .addLast(new ReadTimeoutHandler(30)) // idle超时处理

  }

  def switchToHttpAndWebsocket(ctx: ChannelHandlerContext): Unit = {
    val readIdeaTime: Int = 60 * 1000 * 1000
    val pipeline = ctx.pipeline()

    pipeline.addLast("heart-beat-handler", new IdleStateHandler(readIdeaTime + 500, 0, 0, TimeUnit.MILLISECONDS)) //用于心跳检测
    pipeline.addLast("websocket-log", new LoggingHandler("websocket-log", LogLevel.DEBUG)) //设置log监听器，并且日志级别为debug，方便观察运行流程
    pipeline.addLast("http-codec", new HttpServerCodec) //设置解码器
    pipeline.addLast("aggregator", new HttpObjectAggregator(65536)) //聚合器，使用websocket会用到
    pipeline.addLast("chunked", new ChunkedWriteHandler) //用于大数据的分区传输

    // 编码
    pipeline.addLast("req", new HttpRequestHandler) //用
  }

}


object DynamicSwitchingProtocol {
  val MIN_LENGTH = 5
  val SP = ' '
}
