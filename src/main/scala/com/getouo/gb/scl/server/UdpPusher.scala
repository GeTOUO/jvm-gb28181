package com.getouo.gb.scl.server

import io.netty.bootstrap.Bootstrap
import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.DatagramPacket
import io.netty.channel.socket.nio.NioDatagramChannel

/**
 * udp 推送服务器
 */
class UdpPusher(port: Int) extends RunnableServer {

  private val bossGroup: EventLoopGroup = new NioEventLoopGroup
  val channel: Channel = initCfg()
  override def run(): Unit = {
    try {
      channel.closeFuture.sync()
    } catch {
      case ex: Exception =>
        logger.error(s"start udp[$port] server failed, ", ex)
    } finally {
      bossGroup.shutdownGracefully
    }
  }

  @throws[Exception]
  def initCfg(): Channel = {
    val b: Bootstrap = new Bootstrap()
    b.group(bossGroup).channel(classOf[NioDatagramChannel])
      .option[java.lang.Boolean](ChannelOption.SO_BROADCAST, true)
      .handler(new SimpleChannelInboundHandler[DatagramPacket]() {
        override def channelRead0(channelHandlerContext: ChannelHandlerContext, i: DatagramPacket): Unit = {
          println(s"udp server[$port] 收到:" + i.content().isReadable)
        }
      })
    logger.info(s"ready start udp[$port] server")
    val channelFuture: ChannelFuture = b.bind(port).sync()
    logger.info(s"start udp[$port] server success")
    channelFuture.channel()
  }
}
