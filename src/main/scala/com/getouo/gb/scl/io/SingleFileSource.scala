package com.getouo.gb.scl.io

import java.io.{FileInputStream, FileNotFoundException}

import scala.util.{Failure, Success, Try}

@throws[FileNotFoundException]
class SingleFileSource(fileName: String) extends UnActiveSource[ISourceData] {

  private var fis: FileInputStream = _

  override final def produce(): ISourceData = read()



  private def read(buf: Array[Byte] = new Array[Byte](1024 * 8)): ISourceData = {
    Try(fis.read(buf)) match {
      case Failure(exception) => Try(fis.close()); ErrorSourceData(exception)
      case Success(readLen) => readLen match {
        case -1 => Try(fis.close()); EndSymbol
        case _ => ByteSourceData(buf.take(readLen))
      }
    }
  }

  @throws[FileNotFoundException]
  def reOpen(): Unit = {
    Try(fis.close())
    fis = new FileInputStream(fileName)
  }

  override def load(): Unit = fis = new FileInputStream(fileName)
}
