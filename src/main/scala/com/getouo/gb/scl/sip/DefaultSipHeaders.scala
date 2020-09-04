package com.getouo.gb.scl.sip

import java.util
import java.util.{ArrayList, Calendar, Collections, Date, Iterator, List, Set}

import io.netty.handler.codec.{CharSequenceValueConverter, DateFormatter, DefaultHeaders, DefaultHeadersImpl, HeadersUtils, ValueConverter}
import io.netty.util.AsciiString.CASE_SENSITIVE_HASHER
import io.netty.util.{AsciiString, ByteProcessor}
import io.netty.util.internal.{ObjectUtil, PlatformDependent}

/**
 * 默认的sip头信息集合
 *
 * @author carzy
 * @date 2020/8/10
 */
object DefaultSipHeaders {
  private val HIGHEST_INVALID_VALUE_CHAR_MASK: Int = ~16
  private val ASCII_MAX_INT: Int = 127
  private val HEADER_NAME_VALIDATOR: ByteProcessor = (value: Byte) => {
    def foo(value: Byte) = {
      DefaultSipHeaders.validateHeaderNameElement(value)
      true
    }

    foo(value)
  }
  /**
   * 头信息的名称校验器. 按字节检测.
   */
  private[sip] val SIP_NAME_VALIDATOR: DefaultHeaders.NameValidator[CharSequence] = (name: CharSequence) => {
    def foo(name: CharSequence) = if (name != null && name.length != 0) if (name.isInstanceOf[AsciiString]) try name.asInstanceOf[AsciiString].forEachByte(DefaultSipHeaders.HEADER_NAME_VALIDATOR)
    catch {
      case var3: Exception =>
        PlatformDependent.throwException(var3)
    }
    else {
      var index: Int = 0
      while ( {
        index < name.length
      }) {
        DefaultSipHeaders.validateHeaderNameElement(name.charAt(index))

        {
          index += 1; index
        }
      }
    }
    else throw new IllegalArgumentException("empty headers are not allowed [" + name + "]")

    foo(name)
  }

  /**
   * 校验头名称.
   */
  private def validateHeaderNameElement(value: Byte): Unit = {
    value match {
      case 0x00 =>
      case '\t' =>
      case '\n' =>
      case 0x0b =>
      case '\f' =>
      case '\r' =>
      case ' ' =>
      case ',' =>
      case ':' =>
      case ';' =>
      case '=' =>
        throw new IllegalArgumentException("a header name cannot contain the following prohibited characters: =,;: \\t\\r\\n\\v\\f: " + value)
      case _ =>
        if (value < 0) throw new IllegalArgumentException("a header name cannot contain non-ASCII character: " + value)
    }
  }

  private def validateHeaderNameElement(value: Char): Unit = {
    value match {
      case '\u0000' =>
      case '\t' =>
      case '\n' =>
      case '\u000b' =>
      case '\f' =>
      case '\r' =>
      case ' ' =>
      case ',' =>
      case ':' =>
      case ';' =>
      case '=' =>
        throw new IllegalArgumentException("a header name cannot contain the following prohibited characters: =,;: \\t\\r\\n\\v\\f: " + value)
      case _ =>
        if (value > ASCII_MAX_INT) throw new IllegalArgumentException("a header name cannot contain non-ASCII character: " + value)
    }
  }

  private[sip] def valueConverter(validate: Boolean): ValueConverter[CharSequence] = if (validate) DefaultSipHeaders.HeaderValueConverterAndValidator.INSTANCE
  else DefaultSipHeaders.HeaderValueConverter.INSTANCE

  /**
   * 名称校验器.
   * 默认只要不为null就可以.
   * 通过 validate 控制, 通过构造器传入,构造器默认值为 true.
   */
  private[sip] def nameValidator(validate: Boolean): DefaultHeaders.NameValidator[CharSequence] = if (validate) SIP_NAME_VALIDATOR
  else (name: CharSequence) => ObjectUtil.checkNotNull(name, "name")

  /**
   * 默认的值转换器.
   */
  private object HeaderValueConverter {
    private[sip] val INSTANCE: DefaultSipHeaders.HeaderValueConverter = new DefaultSipHeaders.HeaderValueConverter
  }

  private class HeaderValueConverter private[HeaderValueConverterAndValidator]() extends CharSequenceValueConverter {
    override def convertObject(value: Any): CharSequence = if (value.isInstanceOf[CharSequence]) value.asInstanceOf[CharSequence]
    else if (value.isInstanceOf[Date]) DateFormatter.format(value.asInstanceOf[Date])
    else if (value.isInstanceOf[Calendar]) DateFormatter.format(value.asInstanceOf[Calendar].getTime)
    else value.toString
  }

  /**
   * 消息头值转换器. 扩展 HeaderValueConverter. 并对值进行校验.
   */
  private object HeaderValueConverterAndValidator {
    private[sip] val INSTANCE: DefaultSipHeaders.HeaderValueConverterAndValidator = new DefaultSipHeaders.HeaderValueConverterAndValidator

    private def validateValueChar(seq: CharSequence, state: Int, character: Char): Int = {
      if ((character & HIGHEST_INVALID_VALUE_CHAR_MASK) == 0) character match {
        case '\u0000' =>
          throw new IllegalArgumentException("a header value contains a prohibited character '\u0000': " + seq)
        case '\u000b' =>
          throw new IllegalArgumentException("a header value contains a prohibited character '\\v': " + seq)
        case '\f' =>
          throw new IllegalArgumentException("a header value contains a prohibited character '\\f': " + seq)
        case _ =>
      }
      state match {
        case 0 =>
          character match {
            case '\n' =>
              2
            case '\r' =>
              1
            case _ =>
              state
          }
        case 1 =>
          if (character == '\n') return 2
          throw new IllegalArgumentException("only '\\n' is allowed after '\\r': " + seq)
        case 2 =>
          character match {
            case '\t' || ' ' =>
              0
            case _ =>
              throw new IllegalArgumentException("only ' ' and '\\t' are allowed after '\\n': " + seq)
          }
        case _ =>
          state
      }
    }
  }

  final private class HeaderValueConverterAndValidator private() extends DefaultSipHeaders.HeaderValueConverter() {
    override def convertObject(value: Any): CharSequence = {
      val seq: CharSequence = super.convertObject(value)
      var state: Int = 0
      var index: Int = 0
      while ( {
        index < seq.length
      }) {
        state = HeaderValueConverterAndValidator.validateValueChar(seq, state, seq.charAt(index))

        {
          index += 1; index
        }
      }
      if (state != 0) throw new IllegalArgumentException("a header value must not end with '\\r' or '\\n':" + seq)
      else seq
    }
  }

}

class DefaultSipHeaders protected(val headers: DefaultHeaders[CharSequence, CharSequence, _]) extends AbstractSipHeaders {
  def this(validate: Boolean, nameValidator: DefaultHeaders.NameValidator[CharSequence]) {
    this(new DefaultHeadersImpl[CharSequence, CharSequence](AsciiString.CASE_INSENSITIVE_HASHER, DefaultSipHeaders.valueConverter(validate), nameValidator))
  }

  def this(validate: Boolean) {
    this(validate, DefaultSipHeaders.nameValidator(validate))
  }

  def this {
    this(true)
  }

  override def add(headers: AbstractSipHeaders): AbstractSipHeaders = if (headers.isInstanceOf[DefaultSipHeaders]) {
    this.headers.add(headers.asInstanceOf[DefaultSipHeaders].headers)
    this
  }
  else super.add(headers)

  override def set(headers: AbstractSipHeaders): AbstractSipHeaders = if (headers.isInstanceOf[DefaultSipHeaders]) {
    this.headers.set(headers.asInstanceOf[DefaultSipHeaders].headers)
    this
  }
  else super.set(headers)

  /**
   * add 方法.
   */
  override def add(name: String, value: Any): AbstractSipHeaders = {
    this.headers.addObject(name, value)
    this
  }

  override def add(name: String, values: Iterable[_]): AbstractSipHeaders = {
    this.headers.addObject(name, values)
    this
  }

  override def addInt(name: CharSequence, value: Int): AbstractSipHeaders = {
    this.headers.addObject(name, value)
    this
  }

  override def addShort(name: CharSequence, value: Short): AbstractSipHeaders = {
    this.headers.addObject(name, value)
    this
  }

  /**
   * remove 方法区
   */
  override def remove(name: String): AbstractSipHeaders = {
    this.headers.remove(name)
    this
  }

  override def remove(name: CharSequence): AbstractSipHeaders = {
    this.headers.remove(name)
    this
  }

  /**
   * set 方法区
   */
  override def set(name: String, value: Any): AbstractSipHeaders = {
    this.headers.setObject(name, value)
    this
  }

  override def set(name: CharSequence, value: Any): AbstractSipHeaders = {
    this.headers.setObject(name, value)
    this
  }

  override def set(name: String, values: Iterable[_]): AbstractSipHeaders = {
    this.headers.setObject(name, values)
    this
  }

  override def set(name: CharSequence, values: Iterable[_]): AbstractSipHeaders = {
    this.headers.setObject(name, values)
    this
  }

  override def setInt(name: CharSequence, value: Int): AbstractSipHeaders = {
    this.headers.setObject(name, value)
    this
  }

  override def setShort(name: CharSequence, value: Short): AbstractSipHeaders = {
    this.headers.setObject(name, value)
    this
  }

  /**
   * 清空现有缓存的头信息.
   */
  override def clear: AbstractSipHeaders = {
    this.headers.clear
    this
  }

  /**
   * 获取头信息
   */
  override def get(name: String): String = this.get(name.asInstanceOf[CharSequence])

  override def get(name: CharSequence): String = HeadersUtils.getAsString(this.headers, name)

  override def getInt(name: CharSequence): Integer = this.headers.getInt(name)

  override def getInt(name: CharSequence, defaultValue: Int): Int = this.headers.getInt(name, defaultValue)

  override def getShort(name: CharSequence): Short = this.headers.getShort(name)

  override def getShort(name: CharSequence, defaultValue: Short): Short = this.headers.getShort(name, defaultValue)

  override def getTimeMillis(name: CharSequence): Long = this.headers.getTimeMillis(name)

  override def getTimeMillis(name: CharSequence, defaultValue: Long): Long = this.headers.getTimeMillis(name, defaultValue)

  override def getAll(name: String): util.List[String] = this.getAll(name.asInstanceOf[CharSequence])

  override def getAll(name: CharSequence): util.List[String] = HeadersUtils.getAllAsString(this.headers, name)

  override def entries: util.List[util.Map.Entry[String, String]] = if (this.isEmpty) Collections.emptyList[util.Map.Entry[String, String]]
  else {
    val entriesConverted: util.List[util.Map.Entry[String, String]] = new util.ArrayList[util.Map.Entry[String, String]](this.headers.size)
    for (entry <- this) {
      entriesConverted.add(entry)
    }
    entriesConverted
  }

  /**
   * 判断是否包含某个头信息.
   */
  override def contains(name: String): Boolean = this.contains(name.asInstanceOf[CharSequence])

  override def contains(name: CharSequence): Boolean = headers.contains(name)

  override def contains(name: String, value: String, ignoreCase: Boolean): Boolean = this.contains(name.asInstanceOf[CharSequence], value, ignoreCase)

  override def contains(name: CharSequence, value: CharSequence, ignoreCase: Boolean): Boolean = this.headers.contains(name, value, if (ignoreCase) AsciiString.CASE_INSENSITIVE_HASHER
  else AsciiString.CASE_SENSITIVE_HASHER)

  override def isEmpty: Boolean = this.headers.isEmpty

  override def size: Int = this.headers.size

  override def names: util.Set[String] = HeadersUtils.namesAsString(headers)

  override def equals(o: Any): Boolean = o.isInstanceOf[DefaultSipHeaders] && headers.equals(o.asInstanceOf[DefaultSipHeaders].headers, CASE_SENSITIVE_HASHER)

  override def hashCode: Int = headers.hashCode(CASE_SENSITIVE_HASHER)

  override def copy: AbstractSipHeaders = new DefaultSipHeaders(headers.copy)

  /**
   * 迭代器
   */
  @deprecated override def iterator: util.Iterator[util.Map.Entry[String, String]] = HeadersUtils.iteratorAsString(headers)

  override def iteratorCharSequence: util.Iterator[util.Map.Entry[CharSequence, CharSequence]] = headers.iterator

  override def valueStringIterator(name: CharSequence): util.Iterator[String] = {
    val itr: util.Iterator[CharSequence] = valueCharSequenceIterator(name)
    new util.Iterator[String]() {
      override def hasNext: Boolean = return itr.hasNext

      override

      def next: String = return itr.next.toString

      override

      def remove(): Unit = {
        itr.remove()
      }
    }
  }

  override def valueCharSequenceIterator(name: CharSequence): util.Iterator[CharSequence] = headers.valueIterator(name)
}
