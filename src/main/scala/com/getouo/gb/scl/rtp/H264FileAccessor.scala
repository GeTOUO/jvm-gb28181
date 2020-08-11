package com.getouo.gb.scl.rtp

import java.net.{DatagramPacket, DatagramSocket, InetAddress}
import java.util
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}

import io.netty.buffer.ByteBufAllocator
import io.netty.channel.Channel

class H264FileAccessor(fileName: String) extends Runnable {
  private val workStatus = new AtomicBoolean(false)

  private val observers: ConcurrentHashMap[String, Int] = new ConcurrentHashMap[String, Int]()
  private var channel: Channel = null

//  private val ioQueue: ArrayBlockingQueue[H264NALU] = new ArrayBlockingQueue[H264NALU](10000)

  private def startReadStream(): Unit = {
    if (workStatus.compareAndSet(false, true)) {
      new Thread(new H264FileReader(fileName, n => {
        timestamp += timestampIncrement
        n.rtpPacket(sendSeq, timestamp).foreach(send)
      })).start()
    }
  }

  private val sendSeq: AtomicInteger = new AtomicInteger(0)
  private var timestamp: Int = 0
  private val framerate: Float = 25
  private val timestampIncrement: Int = (90000 / framerate).intValue() //+0.5
  override def run(): Unit = {
    startReadStream()
//    while (true) {
//      try {
//        val hnalu = ioQueue.take()
//        timestamp += timestampIncrement
//        hnalu.rtpPacket(sendSeq, timestamp).foreach(send)
////        val packet = packets.take()
////        observers.values().forEach(s => {
////          //TODO 推流
////        })
//      } catch {
//        case e: Throwable => e.printStackTrace()
//      }
//    }
  }

  val client: DatagramSocket = new DatagramSocket
//  private val inetAddress: InetAddress = InetAddress.getByName("192.168.199.237")
  private val inetAddress: InetAddress = InetAddress.getByName("localhost")

  private def send(byte: Array[Byte]): Unit = {
//    System.err.println(BytesPrinter.toStr(byte))
//    3 + Math.random()
    Thread.sleep((3 + Math.random()).intValue())
    if (this.channel != null) {
      val tcpHeader = new Array[Byte](4)
      tcpHeader(0) = '$'
      tcpHeader(1) = 123
      tcpHeader(2) = ((byte.length >> 8) & 0x0f).byteValue()
      tcpHeader(3) = (byte.length & 0xff).byteValue()
      val tcpPacket = tcpHeader ++ byte
      val buf = ByteBufAllocator.DEFAULT.directBuffer(tcpPacket.length)
      buf.writeBytes(tcpPacket)
      this.channel.writeAndFlush(buf)
    }
    observers.values().forEach(port => {
      //TODO 推流
//      System.err.println("tuiiliu : " + byte.length)
      val sendPacket = new DatagramPacket(byte, byte.length, inetAddress, port)
      client.send(sendPacket)
    })
  }

  def subscribe(actor: String, port: Int): Unit = observers.put(actor, port)
  def subscribeTCP(channel: Channel): Unit = this.channel = channel

  def unSubscribe(actor: String): Unit = observers.remove(actor)
}

object H264FileAccessor {

  val START_TAG4: Array[Byte] = Array[Byte](0, 0, 0, 1)
  val START_TAG3: Array[Byte] = Array[Byte](0, 0, 1)

  def startTag4Index(buf: Array[Byte]): Int = buf.indexOfSlice(START_TAG4)

  def startTag3Index(buf: Array[Byte]): Int = buf.indexOfSlice(START_TAG3)

  def main(args: Array[String]): Unit = {
    //    val byte = new Array[Byte](10)
    var byte: Array[Byte] = Array[Byte](0, 2, 4)

    val byteStart: Array[Byte] = Array[Byte](1, 2, 3, 0, 0, 0, 1, 0)

    println(startTag4Index(byteStart))
    //    println(startTag3Index(byteStart))
    //
    //    byte ++= byteStart
    //    println(util.Arrays.toString(byte))
    //    println(util.Arrays.toString(byteStart))
    ////    byteStart(0) = 3
    //    println(util.Arrays.toString(byte))
    //    println(util.Arrays.toString(byteStart))
    println(util.Arrays.toString(byteStart.drop(3 + 4)))

    val sss = byte
    println(util.Arrays.toString(byte))
    println(util.Arrays.toString(sss))
    byte ++= Array[Byte](2)
    println(util.Arrays.toString(byte))
    println(util.Arrays.toString(sss))

    println("53824-53825".split("-")(0))

    println(2 >> 6)


    println("========================================")
    val framerate: Float = 25
    println((90000 / framerate).intValue())
    println(90000 % framerate)


  }
}
