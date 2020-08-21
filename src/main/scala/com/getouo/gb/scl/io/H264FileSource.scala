package com.getouo.gb.scl.io

import java.io.{FileInputStream, FileNotFoundException}
import java.util.concurrent.atomic.AtomicBoolean

import com.getouo.gb.scl.util.H264NALUFramer
import com.getouo.gb.scl.util.H264NALUFramer.StepInfo

import scala.util.Try

@throws[FileNotFoundException]
class H264FileSource(h264videoFileName: String, accFileName: Option[String] = None) extends UnActiveSource[H264SourceData] {

  private val fisSource: SingleFileSource = new SingleFileSource(h264videoFileName)
  private var accFis: Option[FileInputStream] = _

  //
  private var lastStartLen: Int = -1
  private var naluBuf: Array[Byte] = new Array[Byte](0)
  private val readFinished = new AtomicBoolean(false)

  @throws[FileNotFoundException]
  def reOpen(): Unit = {
    fisSource.reOpen()
    accFis.foreach(in => {
      Try(in.close())
      accFis = accFileName.map(new FileInputStream(_))
    })
  }

  override def produce(): H264SourceData = {
    if (readFinished.get()) EndSymbol
    else atLeastOne()
  }

  @scala.annotation.tailrec
  private def atLeastOne(): H264SourceData = {
    H264NALUFramer.nextUnit(naluBuf) match {
      case Some(value) =>
        onFindNALUAndGetOldStartLen(value) match {
          case Some(value) => value
          case None => atLeastOne()
        }
      case None => fisSource.produce() match {
        case ByteSourceData(array) => naluBuf ++= array; atLeastOne()
        case _: EndSymbolData => onEnd()
        case ErrorSourceData(thr) => onEnd()
        case data: H264SourceData => data
        case _ => onEnd()
      }
    }
  }

  private def onFindNALUAndGetOldStartLen(value: StepInfo): Option[H264SourceData] = {
    val llBuf = this.lastStartLen
    this.naluBuf = value.leftover
    this.lastStartLen = value.nextStartLen
    value.data match {
      case data@H264NaluData(startLen, nalu) if llBuf != -1 => Some(data.copy(startCodeLen = llBuf))
      case _ => None
    }

  }

  private def onEnd(): H264SourceData = {
    if (this.lastStartLen == -1) {
      this.readFinished.set(true);
      EndSymbol
    } else {
      readFinished.set(true)
      val res = H264NaluData(this.lastStartLen, naluBuf)
      lastStartLen = -1
      naluBuf = new Array[Byte](0)
      res
    }
  }

  override def load(): Unit = {
    fisSource.load()
    accFis = accFileName.map(new FileInputStream(_))
  }
}
