package com.getouo.gb.scl.server

import com.getouo.gb.configuration.PlatConfiguration
import com.getouo.gb.scl.server.handler.SIPServerHandler
import io.netty.bootstrap.Bootstrap
import javax.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import io.netty.channel.{ChannelFuture, ChannelOption, EventLoopGroup}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioDatagramChannel

@Component
class SIPUDPServer(configuration: PlatConfiguration, serverHandler: SIPServerHandler) extends Runnable {

  @PostConstruct
  private def init(): Unit = {
    // 异步启动netty 不使用异步会阻塞程序
    val thread: Thread = new Thread(this)
    thread.setDaemon(true)
    thread.setName("sip udp server netty thread")
    thread.start()
  }

  override def run(): Unit = {
    val bossGroup: EventLoopGroup = new NioEventLoopGroup
    try {
      //通过NioDatagramChannel创建Channel，并设置Socket参数支持广播
      //UDP相对于TCP不需要在客户端和服务端建立实际的连接，因此不需要为连接（ChannelPipeline）设置handler
      val b: Bootstrap = new Bootstrap
      b.group(bossGroup).channel(classOf[NioDatagramChannel]).option[java.lang.Boolean](ChannelOption.SO_BROADCAST, true)
        .handler(serverHandler)

      val channelFuture: ChannelFuture = b.bind(configuration.getPort).sync()
      serverHandler.setChannelFuture(channelFuture)

      println("sip udp server started")
      channelFuture.channel.closeFuture.sync
    } catch {
      case e: Exception => e.printStackTrace()
      case e =>
    } finally {
      bossGroup.shutdownGracefully
    }
  }

}
