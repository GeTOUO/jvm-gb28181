package com.getouo.gb.scl.rtp

import java.net.{DatagramPacket, DatagramSocket, InetAddress}
import java.util
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}
import java.util.concurrent.{ArrayBlockingQueue, ConcurrentHashMap}

import com.getouo.gb.util.BytesPrinter

class H264FileAccessor(fileName: String) extends Runnable {
  private val workStatus = new AtomicBoolean(false)

  private val observers: ConcurrentHashMap[String, Int] = new ConcurrentHashMap[String, Int]()

  private val ioQueue: ArrayBlockingQueue[H264NALU] = new ArrayBlockingQueue[H264NALU](10000)

  def popPacket(): RtpPacket = {
    //    fis.re
    null
  }

  private def startReadStream(): Unit = {
    if (workStatus.compareAndSet(false, true)) {
      new Thread(new H264FileReader(fileName, ioQueue)).start()
    }
  }

  private val sendSeq: AtomicInteger = new AtomicInteger(0)
  private var timestamp: Int = 0
  private val framerate: Float = 25
  private val timestampIncrement: Int = (90000 / framerate).intValue() //+0.5
  override def run(): Unit = {
    startReadStream()
    while (true) {
      try {
        val hnalu = ioQueue.take()
        timestamp += timestampIncrement
        if (hnalu.nalu.length <= H264NALU.DEFAULT_MTU_LEN) {
          val rtpHeader = RtpHeader.from(RtpHeader.VERSION, false, false, 0,
            false, RtpHeader.RTP_PAYLOAD_TYPE_H264.byteValue(), sendSeq.incrementAndGet().shortValue(),
            timestamp, 0x88923423)
          //          RtpPacket(rtpHeader, hnalu.nalu)
//          System.err.println(s"rtpHeader.bytes.len = ${rtpHeader.bytes.length}; hnalu.nalu.length= ${hnalu.nalu.length}")
          val toSend = rtpHeader.bytes ++ hnalu.nalu
          send(toSend)
        } else {
          val noTypeNalus = hnalu.nalu.tail
          val groupByMTU = noTypeNalus.grouped(H264NALU.DEFAULT_MTU_LEN).toSeq
          (0 until groupByMTU.length).map(index => {
            val headerBytes = RtpHeader.from(RtpHeader.VERSION, false, false, 0,
              marker = if (index == (groupByMTU.length - 1)) true else false,
              RtpHeader.RTP_PAYLOAD_TYPE_H264.byteValue(), sendSeq.incrementAndGet().shortValue(),
              timestamp, 0x88923423).bytes
            //            val fui = (0 | (hnalu.forbiddenBit << 7).byteValue() | (hnalu.nalReferenceIdc << 5).byteValue() | hnalu.nalUnitType).byteValue()
            val fui = (0 | (hnalu.forbiddenBit << 7).byteValue() | (hnalu.nalReferenceIdc << 5).byteValue() | 28.byteValue()).byteValue()

            val fuh = if (index == 0) {
              (0 & 0xBF & 0xDF | 0x80 | hnalu.nalUnitType).byteValue() // //E=0, //R=0,//S=1
            } else if (index == (groupByMTU.length - 1)) {
              (0 & 0xDF & 0x7F | 0x40 | hnalu.nalUnitType).byteValue() // //R=0, //S=0,//E=1
            } else {
              (0 & 0xDF & 0x7F & 0xBF | hnalu.nalUnitType).byteValue() // //R=0, //S=0,//E=0
            }
            val toSend: Array[Byte] = headerBytes ++ Array[Byte](fui, fuh) ++ groupByMTU(index)
            send(toSend)

          })
        }
//        val packet = packets.take()
//        observers.values().forEach(s => {
//          //TODO 推流
//        })
      } catch {
        case e: Throwable => e.printStackTrace()
      }
    }
  }

  val client: DatagramSocket = new DatagramSocket
//  private val inetAddress: InetAddress = InetAddress.getByName("192.168.199.237")
  private val inetAddress: InetAddress = InetAddress.getByName("192.168.2.19")

  private def send(byte: Array[Byte]): Unit = {
    System.err.println(BytesPrinter.toStr(byte))
    observers.values().forEach(port => {
      //TODO 推流
      System.err.println("tuiiliu : " + byte.length)
      val sendPacket = new DatagramPacket(byte, byte.length, inetAddress, port)
      client.send(sendPacket)
    })
  }

  def subscribe(actor: String, port: Int): Unit = observers.put(actor, port)

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
  }
}
