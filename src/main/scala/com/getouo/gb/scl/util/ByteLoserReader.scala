package com.getouo.gb.scl.util

case class ByteLoserReader(bytes: Array[Byte]) extends Seq[Byte] {

  private var arr = bytes

  def apply(index: Int): Byte = arr(index)

  override def length: Int = arr.length

  override def iterator: Iterator[Byte] = arr.iterator

  override def take(n: Int): Seq[Byte] = {
    val value = super.take(n)
    arr = arr.drop(n)
    value
  }

  def matcher(startIndex: Int, target: Array[Byte]): Boolean = {
    arr.indexOfSlice(target) == startIndex
  }
}

object SDF {
  def main(args: Array[String]): Unit = {
    val reader = ByteLoserReader(Array(1,2,3,4,5,6,7,8,9,10,11,12))
    println(reader.toArray.length)
    println(reader.take(10))
    println(reader.toArray.length)
    println(reader.toArray)
    println(0xba.toByte)
  }
}
