package com.getouo.gb.scl.server

import com.getouo.gb.scl.util.ChannelUtil
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel._
import io.netty.handler.codec.LengthFieldBasedFrameDecoder

import scala.util.{Failure, Success, Try}

//@Component
class RealtimeMediaStreamServer(handler: ChannelInboundHandlerAdapter) extends RunnableServer {

  override def run(): Unit = {
    try {
      channel.closeFuture.sync()
    } catch {
      case ex: Exception =>
        logger.error("start media server failed, ", ex)
    } finally {
      workerGroup.shutdownGracefully
      bossGroup.shutdownGracefully
    }
  }

  @throws[IllegalArgumentException]
  @scala.annotation.tailrec
  private def whileTryPort(b: ServerBootstrap, port: Int): ChannelFuture = {
    if (port <= 0 || port > 65535) {
      workerGroup.shutdownGracefully
      bossGroup.shutdownGracefully
      throw new IllegalArgumentException(s"不可用端口: $port")
    }
    Try(b.bind(port).sync()) match {
      case Failure(_) => whileTryPort(b, port + 1)
      case Success(value) =>
        logger.info(s"start media server success on $port")
        value
    }
  }

  private val bossGroup: EventLoopGroup = new NioEventLoopGroup
  private val workerGroup: EventLoopGroup = new NioEventLoopGroup
  private val b: ServerBootstrap = new ServerBootstrap()

  b.group(bossGroup, workerGroup).channel(classOf[NioServerSocketChannel]).childHandler(
    new ChannelInitializer[SocketChannel]() { // (4)
      @throws[Exception]
      override def initChannel(ch: SocketChannel): Unit = {
        ch.pipeline
          .addLast(new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 2))
          .addLast(handler)
        //              .addLast(new ReadTimeoutHandler(30)) // idle超时处理
      }
    }).option[Integer](ChannelOption.SO_BACKLOG, 128)
    .childOption[java.lang.Boolean](ChannelOption.SO_KEEPALIVE, true)

  val channel: Channel = whileTryPort(b, 10000).channel()
  val localPort: Int = ChannelUtil.castSocketAddr(channel.localAddress()).getPort
}
