package com.getouo.gb.scl.rtsp

import java.text.ParseException

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}

class RtspUriAccessor(val uri: String) {

  private val pathItems: ArrayBuffer[String] = ArrayBuffer.empty
  private val params: mutable.Map[String, String] = mutable.Map.empty
  private val ps: LineParser = new LineParser(uri)
  ps.readIgnoreCase("rtsp://")
  val host: String = ps.readTo(':', '/')

  val port: Int = if (ps.ch == ':') {
    ps.next()
    ps.readInt
  } else 554

  while (ps.hasNext && (ps.ch == '/')) {
    ps.next()
    pathItems.addOne(ps.readTo('/', '?'))
  }

  if (ps.hasNext && (ps.ch == '?')) {
    ps.read('?')
    while (ps.hasNext) {
      val key = ps.readTo('=')
      ps.read('=')
      val value = ps.readTo('&')
      if (ps.hasNext) ps.read('&')
      params.put(key, value)
    }
  }

  def getPathItems: Array[String] = pathItems.toArray

  def pathItem(index: Int): Option[String] = Try(pathItems(index)) match {
    case Failure(exception) => None
    case Success(value) => Option.apply(value)
  }

  def pathItemsSize: Int = pathItems.size

  def getParam(key: String): Option[String] = params.get(key)

  def getParamOrDefault(key: String, defaultValue: String): String = params.getOrElse(key, defaultValue)

  def allParameters(): Map[String, String] = params.toMap

  class LineParser(line: String) {
    var pos = 0
    private val chars = line.toCharArray

    private[RtspUriAccessor] def ch: Char = chars(pos)

    def next(): Unit = pos += 1

    @throws[ParseException]
    private[RtspUriAccessor] def readIgnoreCase(word: String): Unit = {
      val wordChars = word.toCharArray
      for (wordChar <- wordChars) {
        readCharIgnoreCase(wordChar)
      }
    }

    @throws[ParseException]
    private def readCharIgnoreCase(c: Char): Unit = {
      if (Character.toLowerCase(ch) == Character.toLowerCase(c)) next()
      else throw new ParseException("expected: '" + c + "'", pos)
    }

    private[RtspUriAccessor] def readTo(chars: Char*) = {
      val sb = new StringBuilder
      while (hasNext && !contains(chars.toArray, ch)) {
        sb.append(ch)
        next()
      }
      sb.toString
    }

    private def contains(chars: Array[Char], ch: Char): Boolean = {
      for (i <- chars.indices) {
        if (chars(i) == ch) return true
      }
      false
    }

    private[RtspUriAccessor] def hasNext = pos < chars.length

    private[RtspUriAccessor] def readInt: Int = {
      val sb = new StringBuilder
      while (hasNext && ch >= '0' && ch <= '9') {
        sb.append(ch)
        next()
      }
      sb.toString.toInt
    }

    @throws[ParseException]
    def read(c: Char): Unit = {
      if (ch != c) throw new ParseException("expected: '" + c + "'", pos)
      next()
    }
  }

}
