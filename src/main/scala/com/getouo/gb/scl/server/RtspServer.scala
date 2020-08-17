package com.getouo.gb.scl.server

import com.getouo.gb.scl.server.handler._
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.{ChannelFuture, ChannelInitializer, ChannelOption, EventLoopGroup}
import io.netty.handler.codec.rtsp.RtspDecoder
import io.netty.handler.codec.string.{StringDecoder, StringEncoder}
import org.springframework.stereotype.Component

/**
 * 负责处理服务器与客户端之间的请求与响应
 */
@Component
class RtspServer extends RunnableServer {

  override def run(): Unit = {
    val bossGroup: EventLoopGroup = new NioEventLoopGroup
    val workerGroup: EventLoopGroup = new NioEventLoopGroup
    try {
      val b: ServerBootstrap = new ServerBootstrap() // (2)
      b.group(bossGroup, workerGroup).channel(classOf[NioServerSocketChannel]).childHandler(
        new ChannelInitializer[SocketChannel]() { // (4)
          @throws[Exception]
          override def initChannel(ch: SocketChannel): Unit = {
            ch.pipeline
              .addLast(new RtspDecoder) // 添加netty自带的rtsp消息解析器
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
        }).option[Integer](ChannelOption.SO_BACKLOG, 128)
        .childOption[java.lang.Boolean](ChannelOption.SO_KEEPALIVE, true)

      val f: ChannelFuture = b.bind(8550).sync()
      logger.info("start rtsp server success")
      f.channel.closeFuture.sync()
    } catch {
      case ex: Exception =>
        logger.error("start netty failed, ", ex)
    } finally {
      workerGroup.shutdownGracefully
      bossGroup.shutdownGracefully
    }
  }
}
