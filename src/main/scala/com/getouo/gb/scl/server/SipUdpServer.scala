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

  override def run(): Unit = {
    val bossGroup: EventLoopGroup = new NioEventLoopGroup
    try {
      //通过NioDatagramChannel创建Channel，并设置Socket参数支持广播
      //UDP相对于TCP不需要在客户端和服务端建立实际的连接，因此不需要为连接（ChannelPipeline）设置handler
      val b: Bootstrap = new Bootstrap
      b.group(bossGroup).channel(classOf[NioDatagramChannel])
        .option[java.lang.Boolean](ChannelOption.SO_BROADCAST, true)
//        .handler(serverHandler)
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

      logger.info(s"sip udp server started on ${configuration.getPort}")
      channelFuture.channel.closeFuture.sync
    } catch {
      case e: Exception => e.printStackTrace()
      case e =>
    } finally {
      bossGroup.shutdownGracefully
    }
  }

}
