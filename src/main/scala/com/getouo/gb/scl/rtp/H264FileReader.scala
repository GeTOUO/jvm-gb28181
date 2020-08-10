package com.getouo.gb.scl.rtp

import java.io.FileInputStream
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicReference

class H264FileReader(fileName: String, private val ioQueue: ArrayBlockingQueue[H264NALU]) extends Runnable {

//  private var fis = new FileInputStream(fileName)
  private var fis: FileInputStream = null
  private val startContainer: AtomicReference[Array[Byte]] = new AtomicReference[Array[Byte]](null)

  override def run(): Unit = {
    while (true) {
      fis = new FileInputStream(fileName)
      var splitBuf = new Array[Byte](0)
      val buf = new Array[Byte](1024 * 8)
      var readLen = fis.read(buf)
      while (readLen != -1) {
        splitBuf ++= buf.slice(0, readLen)
        var tag4Index = H264FileAccessor.startTag4Index(splitBuf)
        var tag3Index = H264FileAccessor.startTag3Index(splitBuf)
        while (tag4Index != -1 || tag3Index != -1) {
          val lastStart: Array[Byte] = startContainer.get()
          if (tag4Index != -1) {
            if (lastStart != null) addNalu(H264NALU(lastStart.length, splitBuf.slice(0, tag4Index)))
            splitBuf = splitBuf.drop(tag4Index + 4)
            startContainer.set(H264FileAccessor.START_TAG4)
          } else if (tag4Index == -1 && tag3Index != -1) {
            if (lastStart != null) addNalu(H264NALU(lastStart.length, splitBuf.slice(0, tag3Index)))
            splitBuf = splitBuf.drop(tag3Index + 3)
            startContainer.set(H264FileAccessor.START_TAG3)
          }
          tag4Index = H264FileAccessor.startTag4Index(splitBuf)
          tag3Index = H264FileAccessor.startTag3Index(splitBuf)
        }
        readLen = fis.read(buf)
      }
      fis.close()
      val last = startContainer.get()
      if (last != null && !splitBuf.isEmpty) addNalu(H264NALU(last.length, splitBuf))
      startContainer.set(null)
    }
  }

  private def addNalu(n: H264NALU): Unit = {
    try {
      ioQueue.add(n)
    } catch {
      case e: Throwable =>
    }
  }
}
