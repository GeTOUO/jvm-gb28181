package com.getouo.gb.scl.server

import java.io.{File, RandomAccessFile}
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger

import com.getouo.gb.HttpRequestHandler
import com.getouo.gb.scl.data.{H264NaluData, PESFrame, PSH264Audio, PSH264Data}
import com.getouo.gb.scl.stream.{ConsumptionPipeline, SourceConsumer}
import com.getouo.gb.scl.util.ChannelUtil
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel._
import io.netty.channel.group.{ChannelGroup, DefaultChannelGroup}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.{DatagramPacket, SocketChannel}
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import io.netty.util.ReferenceCountUtil
import io.netty.util.concurrent.{Future, GlobalEventExecutor}

import scala.util.{Failure, Success, Try}

@Sharable
class GBStreamPublisher extends ChannelInboundHandlerAdapter with Runnable with SourceConsumer[PSH264Data] {

  val thisHandler: GBStreamPublisher = this
  val actors: ChannelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE)

  val websocketActors: ChannelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE)

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    //    actors.add(ctx.channel())
    logger.info(s"GBStreamPublisher channelActive ${ctx.channel().remoteAddress()}")
    tcpJoin(ctx.channel())
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

  def tcpSend(lo: Long, data: H264NaluData): Unit = {
    //    timestamp = timestamp + timestampIncrement
    timestamp = timestamp + 3600
    //    timestamp = lo.toInt
    val packets = data.rtpPacket(sendSeq, timestamp)

    val nulHeaderTag =  Array[Byte](0x00,0x40,0x00,0x00)


    websocketActors.writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer(nulHeaderTag ++ data.nalu)))
    packets.foreach(p => {
      val tcpHeader = Array[Byte]('$', 0x00, ((p.length & 0xFF00) >> 8).byteValue(), (p.length & 0xFF).byteValue())
      //      tcpSubscriber.writeAndFlush(Unpooled.copiedBuffer(tcpHeader ++ p))
      //      websocketActors.writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer(p)))
      actors.writeAndFlush(Unpooled.copiedBuffer(tcpHeader ++ p))
    })
    packets.map(Unpooled.copiedBuffer).foreach(bf => {
      udpSubscriber.foreach { case (channel, subscribers) =>
        subscribers.map(info => {
          bf.retain()
          new DatagramPacket(bf, new InetSocketAddress(info._1, info._2))
        }).foreach(addr => channel.writeAndFlush(addr))
      }
      ReferenceCountUtil.release(bf)
    })
  }

  val file: RandomAccessFile = new RandomAccessFile(new File("/gb28181.264"), "rws")
  //  private val fileChannel: FileChannel = file.getChannel
  var count = 0
  var writeable = true

  override def onNext(pipeline: ConsumptionPipeline[_, PSH264Data], data: PSH264Data): Unit = {
    count += 1

    //    if (writeable && data.isInstanceOf[PESFrame]) {
    //      file.write(data.asInstanceOf[PESFrame].getNalus.map(n => {
    //        new Array[Byte](n.startCodeLen) ++ n.nalu
    //      }).reduce((a, b) => a ++ b))
    //    } else {
    //      Try(file.close()) match {
    //        case Failure(exception) => exception.printStackTrace()
    //        case Success(value) =>
    //      }
    //    }
    data match {
      case frame: PESFrame =>
        frame.splitNALU.foreach {
          n =>
            if (n.nalUnitType == 7) {
              if (writeable && count > 1000) writeable = false
              tcpSubscriber.forEach(c => actors.add(c))
              tcpSubscriber.clear()

              HttpRequestHandler.actors.forEach(c => websocketActors.add(c))
              HttpRequestHandler.actors.clear()
            }
            tcpSend(0, n)

        }

      //        frame.getNalus.foreach(n => {
      //          if (n.nalUnitType == 7) {
      //            if (count > 1000) writeable = false
      //            System.err.println(tcpSubscriber.size())
      //            tcpSubscriber.forEach(c => actors.add(c))
      //            tcpSubscriber.clear()
      //          }
      //          tcpSend(n)
      //        })
      case PSH264Audio() =>
      case _ =>
    }
  }

  override def onError(pipeline: ConsumptionPipeline[_, PSH264Data], thr: Throwable): Unit = {
    logger.error(s"on error $thr")
  }

  override def onComplete(pipeline: ConsumptionPipeline[_, PSH264Data]): Unit = {
    logger.info(s"onComplete")
  }
}
