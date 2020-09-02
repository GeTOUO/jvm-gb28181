package com.getouo.gb.scl.server

import java.net.{InetAddress, InetSocketAddress}
import java.util.concurrent.TimeUnit

import com.getouo.gb.HttpRequestHandler
import com.getouo.gb.scl.util.LogSupport
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.{NioServerSocketChannel, NioSocketChannel}
import io.netty.channel.{Channel, ChannelFuture, ChannelInitializer, ChannelOption}
import io.netty.handler.codec.http.{HttpObjectAggregator, HttpServerCodec}
import io.netty.handler.logging.{LogLevel, LoggingHandler}
import io.netty.handler.stream.ChunkedWriteHandler
import io.netty.handler.timeout.IdleStateHandler
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class WebsocketServer() extends LogSupport with CommandLineRunner {

  val readIdeaTime: Int = 60 * 1000 * 1000
  private val bossGroup: NioEventLoopGroup = new NioEventLoopGroup
  private val workerGroup: NioEventLoopGroup = new NioEventLoopGroup
  private var channel: Channel = _

  @throws[Exception]
  def start(port: Int): ChannelFuture = { // ServerBootstrap负责初始化netty服务器，并且开始监听端口的socket请求
    val bootstrap: ServerBootstrap = new ServerBootstrap
    val localAddress: InetSocketAddress = new InetSocketAddress(port)
    bootstrap.option[Integer](ChannelOption.SO_BACKLOG, 1024 * 2 * 2 * 2) // 连接队列上限
      .childOption[java.lang.Boolean](ChannelOption.TCP_NODELAY, true) // ture - 关闭Nagle算法
      .group(bossGroup, workerGroup)
      .channel(classOf[NioServerSocketChannel])
      .localAddress(localAddress)
      .childHandler(new ChannelInitializer[NioSocketChannel]() {
      @throws[Exception]
      override protected def initChannel(nsc: NioSocketChannel): Unit = {
        val pipeline = nsc.pipeline
        // 为监听客户端read/write事件的Channel添加用户自定义的ChannelHandler
        pipeline.addLast("heart-beat-handler", new IdleStateHandler(readIdeaTime + 500, 0, 0, TimeUnit.MILLISECONDS)) //用于心跳检测
        pipeline.addLast("websocket-log", new LoggingHandler("websocket-log", LogLevel.DEBUG)) //设置log监听器，并且日志级别为debug，方便观察运行流程
        pipeline.addLast("http-codec", new HttpServerCodec) //设置解码器
        pipeline.addLast("aggregator", new HttpObjectAggregator(65536)) //聚合器，使用websocket会用到
        pipeline.addLast("chunked", new ChunkedWriteHandler) //用于大数据的分区传输

        // 编码
        pipeline.addLast("req", new HttpRequestHandler) //用

      }
    })
    val f = bootstrap.bind.sync
    channel = f.channel
    logger.info("======WebSocketServer启动成功: ws://{}:{}/ =========", InetAddress.getLocalHost.getHostAddress, localAddress.getPort)
    f
  }

  /**
   * 停止服务
   */
  def destroy(): Unit = {
    logger.info(" WebSocket Server 正在关闭...")
    if (channel != null) channel.close
    workerGroup.shutdownGracefully()
    bossGroup.shutdownGracefully()
    logger.info("成功关闭 WebSocket Server!")
  }

  override def run(args: String*): Unit = {
    start(8551)
  }
}
