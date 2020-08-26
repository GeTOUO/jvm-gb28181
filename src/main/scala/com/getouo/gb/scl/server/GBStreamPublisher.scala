package com.getouo.gb.scl.server

import java.util.concurrent.atomic.AtomicInteger

import com.getouo.gb.scl.data.{H264NaluData, PSH264Audio, PSH264Data, PSH264IFrame, PSH264PFrame}
import com.getouo.gb.scl.stream.{ConsumptionPipeline, SourceConsumer}
import com.getouo.gb.scl.util.ChannelUtil
import com.getouo.gb.util.BytesPrinter
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
class GBStreamPublisher extends ChannelInboundHandlerAdapter with Runnable with SourceConsumer[PSH264Data]{

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


  private val sendSeq: AtomicInteger = new AtomicInteger(0)
  private var timestamp: Int = 0
  private val framerate: Float = 25
  private val timestampIncrement: Int = (90000 / framerate).intValue() //+0.5

  override def onNext(pipeline: ConsumptionPipeline[_, PSH264Data], data: PSH264Data): Unit = {

    data match {
      case iFrame@PSH264IFrame(pts, _) =>
        iFrame.getArray.foreach(da => {
          timestamp += timestampIncrement
          val packets = H264NaluData(4, da.drop(4)).rtpPacket(sendSeq, timestamp)

          packets.map(p => {
            val tcpHeader = new Array[Byte](4)
            tcpHeader(0) = '$'
            tcpHeader(1) = 0
            tcpHeader(2) = ((p.length & 0xFF00) >> 8).byteValue()
            tcpHeader(3) = (p.length & 0xFF).byteValue()
            val bytes = tcpHeader ++ p
            logger.info(
              s"""
                 |发送I帧: 前128:
                 |${BytesPrinter.toStr(bytes.take(128))}
                 |""".stripMargin)
            tcpSubscriber.writeAndFlush(Unpooled.copiedBuffer(bytes))
          })
        })
//        tcpSubscriber.writeAndFlush(Unpooled.copiedBuffer(data.bytes))
      case pFrame@PSH264PFrame(pts, _) =>
        pFrame.getArray.foreach(da => {
          timestamp += timestampIncrement
          val packets = H264NaluData(4, da.drop(4)).rtpPacket(sendSeq, timestamp)

          packets.map(p => {
            val tcpHeader = new Array[Byte](4)
            tcpHeader(0) = '$'
            tcpHeader(1) = 0
            tcpHeader(2) = ((p.length & 0xFF00) >> 8).byteValue()
            tcpHeader(3) = (p.length & 0xFF).byteValue()
            val bytes = tcpHeader ++ p
            logger.info(
              s"""
                 |发送P帧: 前128:
                 |${BytesPrinter.toStr(bytes.take(128))}
                 |""".stripMargin)
            tcpSubscriber.writeAndFlush(Unpooled.copiedBuffer(bytes))
          })
        })

      case PSH264Audio() =>
      case _ =>
    }

//    tcpSubscriber.writeAndFlush(Unpooled.copiedBuffer(data.bytes))
//    tcpSubscriber.

//    actors.writeAndFlush(Unpooled.copiedBuffer(data.bytes))
  }

  override def onError(pipeline: ConsumptionPipeline[_, PSH264Data], thr: Throwable): Unit = {
    logger.error(s"on error $thr")
  }

  override def onComplete(pipeline: ConsumptionPipeline[_, PSH264Data]): Unit = {
    logger.info(s"onComplete")
  }
}
