package com.getouo.gb.scl.server

import com.getouo.gb.scl.server.handler.RtspHandler
import javax.annotation.PostConstruct
import org.springframework.stereotype.Component
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.rtsp.RtspDecoder
import io.netty.handler.codec.string.{StringDecoder, StringEncoder}
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.util.internal.logging.{InternalLogger, InternalLoggerFactory}

@Component
class RtspServer extends Runnable {

  protected val logger: InternalLogger = InternalLoggerFactory.getInstance(this.getClass)

  @PostConstruct
  private def init(): Unit = {
    val thread: Thread = new Thread(this)
    thread.setDaemon(true)
    thread.setName("sip udp server netty thread")
    thread.start()
  }

  override def run(): Unit = {

    val bossGroup: EventLoopGroup = new NioEventLoopGroup
    val workerGroup: EventLoopGroup = new NioEventLoopGroup
    try {
      val b: ServerBootstrap = new ServerBootstrap() // (2)
      b.group(bossGroup, workerGroup).channel(classOf[NioServerSocketChannel]).childHandler(
        new ChannelInitializer[SocketChannel]() { // (4)
          @throws[Exception]
          override def initChannel(ch: SocketChannel): Unit = {
            ch.pipeline.addLast(new RtspDecoder) // 添加netty自带的rtsp消息解析器
              .addLast(new RtspHandler) // 上一步将消息解析完成之后, 再交给自定义的处理器
              .addLast(new StringDecoder()) // 上一步将消息解析完成之后, 再交给自定义的处理器
              .addLast(new StringEncoder()) // 上一步将消息解析完成之后, 再交给自定义的处理器
              .addLast(new ReadTimeoutHandler(30)) // idle超时处理
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
