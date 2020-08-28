package com.getouo.gb.scl.util

import java.util.concurrent.atomic.AtomicBoolean

import com.getouo.gb.scl.data.{EndSymbol, H264NaluData, H264SourceData, UnsafeNaluData}
import com.getouo.gb.scl.util.H264NALUFramer.StepInfo

trait H264NALUFramer {

  private var readCompleted: Array[Byte] = Array.empty
  private val readFinished = new AtomicBoolean(false)
  private var lastStartTagLen: Int = -1

  private val sourceReadBuf: Array[Byte] = new Array[Byte](1024 * 8)

  protected def read(buf: Array[Byte]): Array[Byte]

  def clear(): Unit = {
    readCompleted = Array.empty
    readFinished.set(false)
    lastStartTagLen = -1
  }

  final def produceNULA(): H264SourceData = {
    if (readFinished.get()) EndSymbol
    else atLeastOne()
  }

  @scala.annotation.tailrec
  private def atLeastOne(): H264SourceData = {
    H264NALUFramer.nextUnit(readCompleted) match {
      case Some(value) =>
        onFindNALUAndGetOldStartLen(value) match {
          case Some(value) => value
          case None => atLeastOne()
        }
      case None =>
        read(sourceReadBuf) match {
          case arr if arr.nonEmpty => this.readCompleted ++= arr; atLeastOne()
          case _ => onEnd()
        }
    }
  }

  private def onFindNALUAndGetOldStartLen(value: StepInfo): Option[H264NaluData] = {
    val llBuf = this.lastStartTagLen
    this.readCompleted = value.leftover
    this.lastStartTagLen = value.nextStartTagLen
    value.data match {
      case data@H264NaluData(startLen, nalu) if llBuf != -1 => Some(data.copy(startCodeLen = llBuf))
      case _ => None
    }
  }

  private def onEnd(): H264SourceData = {
    this.readFinished.set(true)
    if (this.lastStartTagLen == -1) {
      EndSymbol
    } else {
      val res = H264NaluData(this.lastStartTagLen, this.readCompleted)
      lastStartTagLen = -1
      this.readCompleted = Array.empty
      res
    }
  }
}

object H264NALUFramer {

  val START_TAG4: Array[Byte] = Array[Byte](0, 0, 0, 1)
  val START_TAG3: Array[Byte] = Array[Byte](0, 0, 1)

  case class StepInfo(data: UnsafeNaluData, nextStartTagLen: Int, leftover: Array[Byte])

  def nextUnit(buf: Array[Byte]): Option[StepInfo] = {
    val i4i = buf.indexOfSlice(START_TAG4)
    if (i4i != -1) {
      Some(StepInfo(H264NaluData.of(0, buf.take(i4i)), 4, buf.drop(i4i + 4)))
    } else {
      val i3i = buf.indexOfSlice(START_TAG3)
      if (i3i != -1) {
        Some(StepInfo(H264NaluData.of(0, buf.take(i3i)), 3, buf.drop(i3i + 3)))
      } else None
    }
  }

  def nextUnit(buf: Array[Byte], tags: Array[Array[Byte]]): Option[StepInfo] = {
    tags.collectFirst{case tag if buf.indexOfSlice(tag) != -1 =>
      val tagIndex = buf.indexOfSlice(tag)
      StepInfo(H264NaluData.of(0, buf.take(tagIndex)), tag.length, buf.drop(tagIndex + tag.length))
    }
  }

}
