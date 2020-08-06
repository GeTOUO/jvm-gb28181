package com.getouo.gb.scl.rtp

import java.io.FileInputStream
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{ArrayBlockingQueue, ConcurrentHashMap}

import scala.io.Source

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
        private val buffer: ByteBuffer = ByteBuffer.allocateDirect(1024 * 32)
        var tagCount = 0
        var readCount = 0
        val buf = new Array[Byte](1024 * 32)
        override def run(): Unit = {
          while (true) {
            val byte = new Array[Byte](1024 * 8)

            var readLen = fis.read(byte)
            while (readLen != -1) {

              for 


            }

            while ((readLen = fis.read(byte)) != -1) {
              Source.fromFile(fileName).reader()
            }
            fis.

          }
          buffer.
        }
      }).start()
    }
  }

  override def run(): Unit = {
    while (true) {
      try {
        val packet = packets.take()
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

  def isStartTag(buf: Array[Byte]): Boolean = isStartTag4(buf) || isStartTag3(buf)

  def isStartTag4(buf: Array[Byte]): Boolean = buf.length >= 4 && buf(0) == 0 && buf(1) == 0 && buf(2) == 0 && buf(3) == 1

  def isStartTag3(buf: Array[Byte]): Boolean = buf.length >= 3 && buf(0) == 0 && buf(1) == 0 && buf(2) == 1
}
