package com.getouo.gb.scl.stream

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{ConcurrentHashMap, ExecutorService, Executors, Future}

import com.getouo.gb.scl.data.{EndSymbol, PSH264IFrame, PSH264Data, H264NaluData, H264SourceData, ISourceData}
import com.getouo.gb.scl.io._
import com.getouo.gb.scl.util.{LogSupport, Observer}
import io.netty.channel.Channel

import scala.collection.concurrent
import scala.jdk.CollectionConverters._

trait PlayStreamGroup[ID <: SourceId, PS <: PlayStream[ID, _, _, _]] {
  val groups: concurrent.Map[ID, PS] = new ConcurrentHashMap[ID, PS]().asScala

  def getOrElseSubmit(sourceId: ID, op: ID => PS): PS = {
    groups.getOrElseUpdate(sourceId, {
      val ps = op.apply(sourceId)
      ps.submit()
      System.err.println(s"trait PlayStreamGroup: $ps is submit")
      ps
    })
    //
    //    groups.getOrElseUpdate(fileName, {
    //      import scala.concurrent.ExecutionContext.Implicits.global
    //      new H264PlayStream(new H264FileSource(fileName), new H264ConsumptionPipeline())
    //    })
  }

  def byIdOpt(sourceId: ID): Option[PS] = groups.get(sourceId)
}

trait PlayStream[ID <: SourceId, S <: Source[IN], IN <: ISourceData, OUT <: ISourceData] extends Runnable with LogSupport {

  val id: ID

  val consumptionPipeline: ConsumptionPipeline[IN, OUT]

  def subscribe(consumer: SourceConsumer[OUT]): ConsumptionPipeline[IN, OUT] = consumptionPipeline.subscribe(consumer)

  def subscribe(consumerName: String, consumer: SourceConsumer[OUT]): ConsumptionPipeline[IN, OUT] = consumptionPipeline.subscribe(consumerName, consumer)

  def unsubscribe(consumer: SourceConsumer[OUT]): Boolean = consumptionPipeline.unsubscribe(consumer)

  def unsubscribe(consumerName: String): Boolean = consumptionPipeline.unsubscribe(consumerName)

  override final def run(): Unit = start()

  protected def start(): Unit

  protected def onStop(): Unit

  private val runStatus = new AtomicBoolean(false)

  final def submit(): Unit = {
    if (runStatus.compareAndSet(false, true)) {
      PlayStream.submit(this)
//      val value = PlayStream.submit(this).get()
//      println(s"submit : $value")
    }
  }

  def getOrElseAddConsumer[C <: SourceConsumer[OUT]](consumerClz: Class[C], op: => C): C = {
    consumptionPipeline.getConsumerOrElseUpdate(consumerClz, op)
  }

  // later: replace session join
  def tcpTryJoin[C <: SourceConsumer[_]](consumerClz: Class[C], channel: Channel): Unit = {
    consumptionPipeline.tcpTryJoin(consumerClz, channel)
  }

  def udpTryJoin[C <: SourceConsumer[_]](consumerClz: Class[C], sender: Channel, targetIpAndPort: (String, Int)): Unit = {
    consumptionPipeline.udpTryJoin(consumerClz, sender, targetIpAndPort)
  }
}

abstract class UnActivePlayStream[ID <: SourceId, S <: UnActiveSource[IN], IN <: ISourceData,
  OUT <: ISourceData](source: S, val consumptionPipeline: ConsumptionPipeline[IN, OUT]) extends PlayStream[ID, S, IN, OUT] {

  var counter = 0

  override protected def start(): Unit = {
    source.load()
    var in = source.produce()
    counter += 1
    while (in != EndSymbol) {
      consumptionPipeline.onNext(in)
            Thread.sleep((30 + Math.random()).intValue())
      in = source.produce()
      counter += 1
    }
    logger.info(s"${getClass.getSimpleName} is completed on ${in}; counter=$counter")
    //    Thread.sleep(100 * 1000)
    consumptionPipeline.onComplete()
    onStop()
  }

}

abstract class ActivePlayStream[ID <: SourceId, S <: ActiveSource[IN], IN <: ISourceData,
  OUT <: ISourceData](source: S, val consumptionPipeline: ConsumptionPipeline[IN, OUT]) extends PlayStream[ID, S, IN, OUT] with Observer[IN] {

  override protected def start(): Unit = {
    source.registerObserver(this)
  }

  override def onNext(data: IN): Unit = {
    if (data != EndSymbol)
      consumptionPipeline.onNext(data)
    else {
      consumptionPipeline.onComplete()
      onStop()
    }
  }
}


class GB28181PlayStream(val id: GBSourceId, val source: GB28181RealtimeTCPSource, pipeline: GB28181ConsumptionPipeline)
  extends ActivePlayStream[GBSourceId, GB28181RealtimeTCPSource, PSH264Data, PSH264IFrame](source, pipeline) {
  override protected def onStop(): Unit = GB28181PlayStream.groups.remove(this.id)
}

object GB28181PlayStream extends PlayStreamGroup[GBSourceId, GB28181PlayStream]


class H264PlayStream(val id: FileSourceId, source: H264FileSource, pipeline: H264ConsumptionPipeline)
  extends UnActivePlayStream[FileSourceId, H264FileSource, H264SourceData, H264NaluData](source, pipeline) {
  override protected def onStop(): Unit = H264PlayStream.groups.remove(this.id)
}

object H264PlayStream extends PlayStreamGroup[FileSourceId, H264PlayStream]

object PlayStream {

  private def processors = Runtime.getRuntime.availableProcessors

  private val MAX_THREAD_NUM = processors * 2 + 1
  var executor: ExecutorService = Executors.newFixedThreadPool(MAX_THREAD_NUM)

  protected def submit[_](ps: PlayStream[_, _, _, _]): Future[_] = executor.submit(ps)
}