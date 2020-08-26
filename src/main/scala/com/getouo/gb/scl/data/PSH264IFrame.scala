package com.getouo.gb.scl.data

import scala.collection.mutable

case class PSH264IFrame(val pts: Long, private val arr: Array[Byte] = Array.empty) extends PSH264Data {
  private var bytes: mutable.ArrayBuffer[Array[Byte]] = new mutable.ArrayBuffer[Array[Byte]]()
  def addBytes(arr: Array[Byte]): PSH264IFrame = {
    this.bytes.addOne(arr)
    this
  }

  def addLast(arr: Array[Byte]): Unit = {
    bytes.last ++ arr
    bytes.update(bytes.length - 1, bytes.last ++ arr)
  }

  def getArray: Seq[Array[Byte]] = bytes.toSeq

}

case class PSH264PFrame(val pts: Long, private val arr: Array[Byte] = Array.empty) extends PSH264Data {
  private var bytes: mutable.ArrayBuffer[Array[Byte]] = new mutable.ArrayBuffer[Array[Byte]]()
  def addBytes(arr: Array[Byte]): PSH264PFrame = {
    this.bytes.addOne(arr)
    this
  }

  def addLast(arr: Array[Byte]): Unit = {
    bytes.last ++ arr
    bytes.update(bytes.length - 1, bytes.last ++ arr)
  }

  def getArray: Seq[Array[Byte]] = bytes.toSeq
}

case class PSH264Audio() extends PSH264Data {

}

object PSH264IFrame {

}