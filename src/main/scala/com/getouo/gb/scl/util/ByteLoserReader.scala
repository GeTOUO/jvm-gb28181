package com.getouo.gb.scl.util

case class ByteLoserReader(bytes: Array[Byte]) extends Seq[Byte] {

  private var arr = bytes

  def apply(index: Int): Byte = arr(index)

  override def length: Int = arr.length

  override def iterator: Iterator[Byte] = arr.iterator

  override def take(n: Int): Seq[Byte] = {
    val value = arr.take(n)
    arr = arr.drop(n)
    value
  }

  override def indexOfSlice[B >: Byte](that: collection.Seq[B]): Int = arr.indexOfSlice(that)

  override def drop(n: Int): Seq[Byte] = {
    arr = arr.drop(n)
    this
  }

  def matcher(startIndex: Int, target: Array[Byte]): Boolean = {
    arr.indexOfSlice(target) == startIndex
  }
}
