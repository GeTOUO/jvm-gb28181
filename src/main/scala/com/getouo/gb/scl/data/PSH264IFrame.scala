package com.getouo.gb.scl.data

case class PSH264IFrame(val pts: Long, private val arr: Array[Byte] = Array.empty) extends PSH264Data {
  private var bytes = arr
  def addBytes(arr: Array[Byte]): PSH264IFrame = {
    this.bytes ++= arr
    this
  }

  def getArray: Array[Byte] = bytes
}

case class PSH264PFrame(val pts: Long, private val arr: Array[Byte] = Array.empty) extends PSH264Data {
  private var bytes = arr
  def addBytes(arr: Array[Byte]): PSH264PFrame = {
    this.bytes ++= arr
    this
  }

  def getArray: Array[Byte] = bytes
}

case class PSH264Audio() extends PSH264Data {

}

object PSH264IFrame {

}