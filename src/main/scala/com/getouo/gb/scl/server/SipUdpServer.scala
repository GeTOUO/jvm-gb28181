package com.getouo.gb.scl.server

import com.getouo.gb.configuration.PlatConfiguration
import com.getouo.gb.scl.server.handler.{ProxyHandler, SipNonEmptyDatagramPacketFilter, SipRequestHandler, SipServerHandler}
import io.netty.bootstrap.Bootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.channel.{Channel, ChannelFuture, ChannelInitializer, ChannelOption, EventLoopGroup}
import io.netty.handler.codec.http.{HttpObjectAggregator, HttpServerCodec}
import io.netty.handler.codec.rtsp.RtspDecoder
import io.sipstack.netty.codec.sip.{SipMessageDatagramDecoder, SipMessageEncoder}
import org.springframework.stereotype.Component

@Component
class SipUdpServer(configuration: PlatConfiguration, serverHandler: SipServerHandler) extends RunnableServer {

  private val bossGroup: EventLoopGroup = new NioEventLoopGroup
  private val b: Bootstrap = new Bootstrap
  val sender: Channel = {
    b.group(bossGroup).channel(classOf[NioDatagramChannel])
      .option[java.lang.Boolean](ChannelOption.SO_BROADCAST, true)
      .handler(new ChannelInitializer[Channel]() {
        override def initChannel(ch: Channel): Unit = {
          ch.pipeline
            .addLast("sip-encoder", new SipMessageEncoder())
            .addLast("empty-filter", new SipNonEmptyDatagramPacketFilter())
            .addLast(new SipMessageDatagramDecoder)
            .addLast(new ProxyHandler)
            //              .addLast("http-codec", new HttpServerCodec())
            //              .addLast("aggregator", new HttpObjectAggregator(65536))
            //              .addLast("sr", new SipRequestHandler())
            .addLast(serverHandler)
        }
      })
    val channelFuture: ChannelFuture = b.bind(configuration.getPort).sync()
    serverHandler.setChannelFuture(channelFuture)
    channelFuture.channel()
  }

  override def run(): Unit = {
    try {
      logger.info(s"sip udp server started on ${configuration.getPort}")
      sender.closeFuture.sync
    } catch {
      case e: Exception => e.printStackTrace()
      case e =>
    } finally {
      bossGroup.shutdownGracefully
    }
  }

}
