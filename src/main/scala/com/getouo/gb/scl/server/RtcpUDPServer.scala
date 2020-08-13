package com.getouo.gb.scl.server

import io.netty.bootstrap.Bootstrap
import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.DatagramPacket
import io.netty.channel.socket.nio.NioDatagramChannel

/**
 * 负责处理服务器与客户端之间的请求与响应
 */
//@Component
class RtcpUDPServer(port: Int) extends RunnableServer {

  var channel: Channel = null
  override def run(): Unit = {
    val bossGroup: EventLoopGroup = new NioEventLoopGroup
    try {
      val b: Bootstrap = new Bootstrap()
      b.group(bossGroup).channel(classOf[NioDatagramChannel])
        .option[java.lang.Boolean](ChannelOption.SO_BROADCAST, true)
        .handler(new SimpleChannelInboundHandler[DatagramPacket]() {
          override def channelRead0(channelHandlerContext: ChannelHandlerContext, i: DatagramPacket): Unit = {
            println("rtcp server 收到:" + i.content().isReadable)
          }
        })

      logger.info("ready start rtcp server")
      val channelFuture: ChannelFuture = b.bind(port).sync()
      channel = channelFuture.channel()
      logger.info("start rtcp server success")
      channel.closeFuture.sync()
    } catch {
      case ex: Exception =>
        logger.error("start rtcp server failed, ", ex)
    } finally {
      bossGroup.shutdownGracefully
    }
  }
}
