package com.getouo.gb.scl.server

import com.getouo.gb.scl.server.handler._
import io.netty.bootstrap.{Bootstrap, ServerBootstrap}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.{DatagramChannel, SocketChannel}
import io.netty.channel.socket.nio.{NioDatagramChannel, NioServerSocketChannel}
import io.netty.channel.{Channel, ChannelFuture, ChannelInitializer, ChannelOption, ChannelPipeline, EventLoopGroup}
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.rtsp.{RtspDecoder, RtspEncoder}
import io.netty.handler.codec.string.{StringDecoder, StringEncoder}
import io.sipstack.netty.codec.sip.{SipMessageDatagramDecoder, SipMessageEncoder}
import java.util.concurrent.{ConcurrentHashMap, CountDownLatch, TimeUnit}

import com.getouo.gb.scl.sip.{SipRequestDispatcher, SipResponseDispatcher}
import com.getouo.gb.scl.stream.SourceConsumer
import com.getouo.sip.{AbstractSipRequestEncoder, AbstractSipResponseEncoder, SipObjectAggregator, SipObjectTcpDecoder, SipObjectUdpDecoder}
import io.netty.util.concurrent.Future
import org.springframework.stereotype.Component

import scala.collection.concurrent

/**
 * 负责处理服务器与客户端之间的请求与响应
 */
@Component
class HybridProtocolInstructionServer extends RunnableServer {

  val port = 8550

  private val bossGroup: EventLoopGroup = new NioEventLoopGroup
  private val workerGroup: EventLoopGroup = new NioEventLoopGroup
  private val sipUdpGroup: EventLoopGroup = new NioEventLoopGroup

  private val tcpBoot: ServerBootstrap = new ServerBootstrap()
  private val udpBoot: Bootstrap = new Bootstrap

  private val channelInitializer = new ChannelInitializer[SocketChannel]() {
    @throws[Exception]
    override protected def initChannel(ch: SocketChannel): Unit =
      ch.pipeline.addLast(new DynamicSwitchingProtocol())
  }
  tcpBoot.group(bossGroup, workerGroup).channel(classOf[NioServerSocketChannel])
    .childHandler(channelInitializer)
    .option[Integer](ChannelOption.SO_BACKLOG, 128)
    .option[Integer](ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
    .childOption[java.lang.Boolean](ChannelOption.SO_KEEPALIVE, true)
    .childOption[java.lang.Boolean](ChannelOption.TCP_NODELAY, true)

  udpBoot.group(this.sipUdpGroup).channel(classOf[NioDatagramChannel]).handler(new ChannelInitializer[DatagramChannel]() {
    @throws[Exception]
    override protected def initChannel(ch: DatagramChannel): Unit = {
      val pipeline = ch.pipeline
      pipeline.addLast(new AbstractSipResponseEncoder())
      pipeline.addLast(new AbstractSipRequestEncoder())
      pipeline.addLast(new SipObjectUdpDecoder())
      pipeline.addLast(new SipObjectAggregator(8192 * 10))
      pipeline.addLast(new SipRequestDispatcher())
      pipeline.addLast(new SipResponseDispatcher())

//      pipeline.addLast("decoder", new SipMessageDatagramDecoder)
//      pipeline.addLast("encoder", new SipMessageEncoder)
//      pipeline.addLast(new ProxyHandler)
      //      pipeline.addLast("handler", handler)
    }
  })

  private def udpSipListening(): Unit = {

  }

  override def run(): Unit = {
    try {
      val channel = this.udpBoot.bind(port).sync.channel
      HybridProtocolInstructionServer.sipChannels.put(port, channel)
      if (port != 5060) {
        val syncFuture = this.udpBoot.bind(5060).sync
        syncFuture.addListener((f: Future[_]) => {
          if (f.isSuccess) {
            logger.info(s"sip udp listening 5060 success")
          } else {
            logger.info(s"sip udp listening 5060 failed!")
          }
        })
        HybridProtocolInstructionServer.sipChannels.put(5060, syncFuture.channel)
      }
      HybridProtocolInstructionServer.countDownLatch.countDown()
      val f: ChannelFuture = tcpBoot.bind(port).sync()
      logger.info(s"start server success: rtsp sip http websocket on $port")
      f.channel.closeFuture.sync()
      HybridProtocolInstructionServer.sipChannels.values.foreach(_.closeFuture().sync())
    } catch {
      case ex: Exception =>
        logger.error("start server failed, ", ex)
    } finally {
      sipUdpGroup.shutdownGracefully
      workerGroup.shutdownGracefully
      bossGroup.shutdownGracefully
    }
  }
}

import scala.collection.concurrent
import scala.jdk.CollectionConverters._

object HybridProtocolInstructionServer {

  private val sipChannels: concurrent.Map[Int, Channel] = new ConcurrentHashMap[Int, Channel]().asScala
  private val countDownLatch: CountDownLatch = new CountDownLatch(1)
  def getSipUdpSender(maybePort: Int): Channel = {
    countDownLatch.await()
    sipChannels.values.headOption.map(any => {
      sipChannels.getOrElse(maybePort, any)
    }).getOrElse(throw new RuntimeException(s"没有sip udp sender"))
  }
}