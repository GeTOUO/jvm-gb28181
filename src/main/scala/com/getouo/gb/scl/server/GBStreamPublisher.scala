package com.getouo.gb.scl.server

import com.getouo.gb.scl.data.PSH264IFrame
import com.getouo.gb.scl.stream.{ConsumptionPipeline, SourceConsumer}
import com.getouo.gb.scl.util.ChannelUtil
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel._
import io.netty.channel.group.{ChannelGroup, DefaultChannelGroup}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.util.ReferenceCountUtil
import io.netty.util.concurrent.GlobalEventExecutor

import scala.util.{Failure, Success, Try}

@Sharable
class GBStreamPublisher extends ChannelInboundHandlerAdapter with Runnable with SourceConsumer[PSH264IFrame]{

  val thisHandler: GBStreamPublisher = this
  val actors: ChannelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE)

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    actors.add(ctx.channel())
  }

  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
    try ReferenceCountUtil.release(msg)
    catch {
      case e: Exception =>
        e.printStackTrace()
    }
  }

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
        logger.info("start media server success on $port")
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
        ch.pipeline.addLast(thisHandler)
        //              .addLast(new ReadTimeoutHandler(30)) // idle超时处理
      }
    }).option[Integer](ChannelOption.SO_BACKLOG, 128)
    .childOption[java.lang.Boolean](ChannelOption.SO_KEEPALIVE, true)

  val channel: Channel = whileTryPort(b, 10000).channel()
  val localPort: Int = ChannelUtil.castSocketAddr(channel.localAddress()).getPort

  override def onNext(pipeline: ConsumptionPipeline[_, PSH264IFrame], data: PSH264IFrame): Unit = {


    tcpSubscriber.writeAndFlush(Unpooled.copiedBuffer(data.bytes))
//    tcpSubscriber.

//    actors.writeAndFlush(Unpooled.copiedBuffer(data.bytes))
  }

  override def onError(pipeline: ConsumptionPipeline[_, PSH264IFrame], thr: Throwable): Unit = {
    logger.error(s"on error $thr")
  }

  override def onComplete(pipeline: ConsumptionPipeline[_, PSH264IFrame]): Unit = {
    logger.info(s"onComplete")
  }
}
