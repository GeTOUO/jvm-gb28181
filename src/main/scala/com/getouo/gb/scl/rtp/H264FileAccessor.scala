package com.getouo.gb.scl.rtp

import java.io.FileInputStream
import java.net.SocketAddress
import java.util
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}
import java.util.concurrent.{ArrayBlockingQueue, ConcurrentHashMap}

class H264FileAccessor(fileName: String) extends Runnable {
  private val workStatus = new AtomicBoolean(false)
  private val fis = new FileInputStream(fileName)

  private val packets: ArrayBlockingQueue[RtpPacket] = new ArrayBlockingQueue[RtpPacket](10000)
  private val observers: ConcurrentHashMap[String, SocketAddress] = new ConcurrentHashMap[String, SocketAddress]()

  def popPacket(): RtpPacket = {
    //    fis.re
    null
  }

  private def startReadStream(): Unit = {
    if (workStatus.compareAndSet(false, true)) {

      new Thread(new Runnable {
        val startContainer: AtomicReference[Array[Byte]] = new AtomicReference[Array[Byte]](null)
        override def run(): Unit = {
          while (true) {
            var bigBuf = new Array[Byte](0)

            val buf = new Array[Byte](1024 * 8)
            var readLen = fis.read(buf)
            while (readLen != -1) {
              bigBuf ++= buf.slice(0, readLen)
              val tag4Index = H264FileAccessor.startTag4Index(bigBuf)
              val tag3Index = H264FileAccessor.startTag3Index(bigBuf)
              val lastStart = startContainer.get()
              if (tag4Index != -1) {
                if (lastStart != null) {
                  val bytes = bigBuf.slice(0, tag4Index)
                  H264NALU(lastStart.length, bytes)
                }
                bigBuf = bigBuf.drop(tag4Index + 4)
                startContainer.set(H264FileAccessor.START_TAG4)
              } else if (tag4Index == -1 && tag3Index != -1) {
                if (lastStart != -1) {
                  val bytes = bigBuf.slice(0, tag3Index)
                  H264NALU(lastStart.length, bytes)
                }
                bigBuf = bigBuf.drop(tag3Index + 3)
                startContainer.set(H264FileAccessor.START_TAG3)
              }
              readLen = fis.read(buf)
            }
            val last = startContainer.get()
            if (last != null && !bigBuf.isEmpty) {
              H264NALU(last.length, bigBuf)
            }
            startContainer.set(null)
          }
        }
      }).start()

    }
  }

  override def run(): Unit = {
    while (true) {
      try {
        val packet = packets.take()

        RtpHeader(RtpHeader.VERSION, 0, 0, 0, 0, RtpHeader.RTP_PAYLOAD_TYPE_H264, 0, 0, 0x88923423)


        observers.values().forEach(s => {
          //TODO 推流
        })
      } catch {
        case e => e.printStackTrace()
      }
    }
  }

  def subscribe(actor: String, adr: SocketAddress): Unit = observers.put(actor, adr)

  def unSubscribe(actor: String): Unit = observers.remove(actor)
}

object H264FileAccessor {

  val START_TAG4: Array[Byte] = Array[Byte](0, 0, 0, 1)
  val START_TAG3: Array[Byte] = Array[Byte](0, 0, 1)

  def startTag4Index(buf: Array[Byte]): Int = buf.lastIndexOfSlice(START_TAG4)

  def startTag3Index(buf: Array[Byte]): Int = buf.lastIndexOfSlice(START_TAG3)

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
