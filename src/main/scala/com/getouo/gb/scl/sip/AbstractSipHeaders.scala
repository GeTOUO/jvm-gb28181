//package com.getouo.gb.scl.sip
//
//import java.util
//import java.util.{Iterator, List, Set}
//
//import io.netty.handler.codec.HeadersUtils
//import io.netty.util.AsciiString
//import io.netty.util.AsciiString.{contentEquals, contentEqualsIgnoreCase, trim}
//import io.netty.util.internal.ObjectUtil
//import io.netty.util.internal.ObjectUtil.checkNotNull
//
///**
// * Sip 消息头. 参照 AbstractSipHeaders. 地下的实现类需要提供给外部的一些获取方法.
// *
// * @author carzy
// * @see io.netty.handler.codec.http.HttpHeaders
// * @see HashMap
// */
//object AbstractSipHeaders {
//  private def containsCommaSeparatedTrimmed(rawNext: CharSequence, expected: CharSequence, ignoreCase: Boolean): Boolean = {
//    var begin: Int = 0
//    var end: Int = 0
//    if (ignoreCase) if ((end = AsciiString.indexOf(rawNext, ',', begin)) == -1) return contentEqualsIgnoreCase(trim(rawNext), expected)
//    else {
//      do {
//        if (contentEqualsIgnoreCase(trim(rawNext.subSequence(begin, end)), expected)) return true
//        begin = end + 1
//      } while ( {
//        (end = AsciiString.indexOf(rawNext, ',', begin)) != -1
//      })
//      if (begin < rawNext.length) return contentEqualsIgnoreCase(trim(rawNext.subSequence(begin, rawNext.length)), expected)
//    }
//    else if ((end = AsciiString.indexOf(rawNext, ',', begin)) == -1) return contentEquals(trim(rawNext), expected)
//    else {
//      do {
//        if (contentEquals(trim(rawNext.subSequence(begin, end)), expected)) return true
//        begin = end + 1
//      } while ( {
//        (end = AsciiString.indexOf(rawNext, ',', begin)) != -1
//      })
//      if (begin < rawNext.length) return contentEquals(trim(rawNext.subSequence(begin, rawNext.length)), expected)
//    }
//    false
//  }
//}
//
//abstract class AbstractSipHeaders protected() extends Iterable[util.Map.Entry[String, String]] {
//  /**
//   * @see #get(CharSequence)
//   */
//  def get(name: String): String
//
//  /**
//   * 返回对应的头信息,如果没有则返回null.
//   */
//  def get(name: CharSequence): String = get(name.toString)
//
//  /**
//   * 返回对应的头信息,如果没有则返回defaultValue.
//   */
//  def get(name: CharSequence, defaultValue: String): String = {
//    val value: String = get(name)
//    if (value == null) return defaultValue
//    value
//  }
//
//  def getInt(name: CharSequence): Integer
//
//  /**
//   * 返回对应的头信息,如果没有则返回默认值
//   */
//  def getInt(name: CharSequence, defaultValue: Int): Int
//
//  def getShort(name: CharSequence): Short
//
//  def getShort(name: CharSequence, defaultValue: Short): Short
//
//  /**
//   * 返回对应的头信息,如果没有则返回null
//   */
//  def getTimeMillis(name: CharSequence): Long
//
//  def getTimeMillis(name: CharSequence, defaultValue: Long): Long
//
//  /**
//   * @see #getAll(CharSequence)
//   */
//  def getAll(name: String): util.List[String]
//
//  def getAll(name: CharSequence): util.List[String] = getAll(name.toString)
//
//  /**
//   * Returns a new {@link List} that contains all headers in this object.  Note that modifying the
//   * returned {@link List} will not affect the state of this object.  If you intend to enumerate over the header
//   * entries only, use {@link #iterator()} instead, which has much less overhead.
//   *
//   * @see #iteratorCharSequence()
//   */
//  def entries: util.List[util.Map.Entry[String, String]]
//
//  /**
//   * @see #contains(CharSequence)
//   */
//  def contains(name: String): Boolean
//
//  /**
//   * @return Iterator over the name/value header pairs.
//   */
//  def iteratorCharSequence: util.Iterator[util.Map.Entry[CharSequence, CharSequence]]
//
//  /**
//   * Equivalent to {@link #getAll(String)} but it is possible that no intermediate list is generated.
//   *
//   * @param name the name of the header to retrieve
//   * @return an { @link Iterator} of header values corresponding to { @code name}.
//   */
//  def valueStringIterator(name: CharSequence): util.Iterator[String] = getAll(name).iterator
//
//  def valueCharSequenceIterator(name: CharSequence): util.Iterator[_ <: CharSequence] = valueStringIterator(name)
//
//  /**
//   * Checks to see if there is a header with the specified name
//   *
//   * @param name The name of the header to search for
//   * @return True if at least one header is found
//   */
//  def contains(name: CharSequence): Boolean = contains(name.toString)
//
//  /**
//   * Checks if no header exists.
//   */
//  def isEmpty: Boolean
//
//  /**
//   * Returns the number of headers in this object.
//   */
//  def size: Int
//
//  /**
//   * Returns a new {@link Set} that contains the names of all headers in this object.  Note that modifying the
//   * returned {@link Set} will not affect the state of this object.  If you intend to enumerate over the header
//   * entries only, use {@link #iterator()} instead, which has much less overhead.
//   */
//  def names: util.Set[String]
//
//  /**
//   * @see #add(CharSequence, Object)
//   */
//  def add(name: String, value: Any): AbstractSipHeaders
//
//  /**
//   * Adds a new header with the specified name and value.
//   * <p>
//   * If the specified value is not a {@link String}, it is converted
//   * into a {@link String} by {@link Object#toString()}, except in the cases
//   * of {@link Date} and {@link Calendar}, which are formatted to the date
//   * format defined in <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3.1">RFC2616</a>.
//   *
//   * @param name  The name of the header being added
//   * @param value The value of the header being added
//   * @return { @code this}
//   */
//  def add(name: CharSequence, value: Any): AbstractSipHeaders = add(name.toString, value)
//
//  /**
//   * @see #add(CharSequence, Iterable)
//   */
//  def add(name: String, values: Iterable[_]): AbstractSipHeaders
//
//  /**
//   * Adds a new header with the specified name and values.
//   * <p>
//   * This getMethod can be represented approximately as the following code:
//   * <pre>
//   * for (Object v: values) {
//   * if (v == null) {
//   * break;
//   * }
//   *     headers.add(name, v);
//   * }
//   * </pre>
//   *
//   * @param name   The name of the headers being set
//   * @param values The values of the headers being set
//   * @return { @code this}
//   */
//  def add(name: CharSequence, values: Iterable[_]): AbstractSipHeaders = add(name.toString, values)
//
//  /**
//   * Adds all header entries of the specified {@code headers}.
//   *
//   * @return { @code this}
//   */
//  def add(headers: AbstractSipHeaders): AbstractSipHeaders = {
//    ObjectUtil.checkNotNull(headers, "headers")
//    for (e <- headers) {
//      add(e.getKey, e.getValue)
//    }
//    this
//  }
//
//  /**
//   * Add the {@code name} to {@code value}.
//   *
//   * @param name  The name to modify
//   * @param value The value
//   * @return { @code this}
//   */
//  def addInt(name: CharSequence, value: Int): AbstractSipHeaders
//
//  def addShort(name: CharSequence, value: Short): AbstractSipHeaders
//
//  /**
//   * @see #set(CharSequence, Object)
//   */
//  def set(name: String, value: Any): AbstractSipHeaders
//
//  /**
//   * Sets a header with the specified name and value.
//   * <p>
//   * If there is an existing header with the same name, it is removed.
//   * If the specified value is not a {@link String}, it is converted into a
//   * {@link String} by {@link Object#toString()}, except for {@link Date}
//   * and {@link Calendar}, which are formatted to the date format defined in
//   * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3.1">RFC2616</a>.
//   *
//   * @param name  The name of the header being set
//   * @param value The value of the header being set
//   * @return { @code this}
//   */
//  def set(name: CharSequence, value: Any): AbstractSipHeaders = set(name.toString, value)
//
//  /**
//   * @see #set(CharSequence, Iterable)
//   */
//  def set(name: String, values: Iterable[_]): AbstractSipHeaders
//
//  /**
//   * Sets a header with the specified name and values.
//   * <p>
//   * If there is an existing header with the same name, it is removed.
//   * This getMethod can be represented approximately as the following code:
//   * <pre>
//   * headers.remove(name);
//   * for (Object v: values) {
//   * if (v == null) {
//   * break;
//   * }
//   *     headers.add(name, v);
//   * }
//   * </pre>
//   *
//   * @param name   The name of the headers being set
//   * @param values The values of the headers being set
//   * @return { @code this}
//   */
//  def set(name: CharSequence, values: Iterable[_]): AbstractSipHeaders = set(name.toString, values)
//
//  /**
//   * Cleans the current header entries and copies all header entries of the specified {@code headers}.
//   *
//   * @return { @code this}
//   */
//  def set(headers: AbstractSipHeaders): AbstractSipHeaders = {
//    checkNotNull(headers, "headers")
//    clear
//    if (headers.isEmpty) return this
//    for (entry <- headers) {
//      add(entry.getKey, entry.getValue)
//    }
//    this
//  }
//
//  /**
//   * Retains all current headers but calls {@link #set(String, Object)} for each entry in {@code headers}
//   *
//   * @param headers The headers used to { @link #set(String, Object)} values in this instance
//   * @return { @code this}
//   */
//  def setAll(headers: AbstractSipHeaders): AbstractSipHeaders = {
//    checkNotNull(headers, "headers")
//    if (headers.isEmpty) return this
//    for (entry <- headers) {
//      set(entry.getKey, entry.getValue)
//    }
//    this
//  }
//
//  /**
//   * Set the {@code name} to {@code value}. This will remove all previous values associated with {@code name}.
//   *
//   * @param name  The name to modify
//   * @param value The value
//   * @return { @code this}
//   */
//  def setInt(name: CharSequence, value: Int): AbstractSipHeaders
//
//  def setShort(name: CharSequence, value: Short): AbstractSipHeaders
//
//  /**
//   * @see #remove(CharSequence)
//   */
//  def remove(name: String): AbstractSipHeaders
//
//  /**
//   * Removes the header with the specified name.
//   *
//   * @param name The name of the header to remove
//   * @return { @code this}
//   */
//  def remove(name: CharSequence): AbstractSipHeaders = remove(name.toString)
//
//  /**
//   * Removes all headers from this {@link SipMessage}.
//   *
//   * @return { @code this}
//   */
//  def clear: AbstractSipHeaders
//
//  /**
//   * @see #contains(CharSequence, CharSequence, boolean)
//   */
//  def contains(name: String, value: String, ignoreCase: Boolean): Boolean = {
//    val valueIterator: util.Iterator[String] = valueStringIterator(name)
//    if (ignoreCase) while ( {
//      valueIterator.hasNext
//    }) if (valueIterator.next.equalsIgnoreCase(value)) return true
//    else while ( {
//      valueIterator.hasNext
//    }) if (valueIterator.next == value) return true
//    false
//  }
//
//  /**
//   * Returns {@code true} if a header with the {@code name} and {@code value} exists, {@code false} otherwise.
//   * This also handles multiple values that are separated with a {@code ,}.
//   * <p>
//   * If {@code ignoreCase} is {@code true} then a case insensitive compare is done on the value.
//   *
//   * @param name       the name of the header to find
//   * @param value      the value of the header to find
//   * @param ignoreCase { @code true} then a case insensitive compare is run to compare values.
//   *                           otherwise a case sensitive compare is run to compare values.
//   */
//  def containsValue(name: CharSequence, value: CharSequence, ignoreCase: Boolean): Boolean = {
//    val itr: util.Iterator[_ <: CharSequence] = valueCharSequenceIterator(name)
//    while ( {
//      itr.hasNext
//    }) if (AbstractSipHeaders.containsCommaSeparatedTrimmed(itr.next, value, ignoreCase)) return true
//    false
//  }
//
//  /**
//   * {@link Headers#get(Object)} and convert the result to a {@link String}.
//   *
//   * @param name the name of the header to retrieve
//   * @return the first header value if the header is found. { @code null} if there's no such header.
//   */
//  final def getAsString(name: CharSequence): String = get(name)
//
//  /**
//   * {@link Headers#getAll(Object)} and convert each element of {@link List} to a {@link String}.
//   *
//   * @param name the name of the header to retrieve
//   * @return a { @link List} of header values or an empty { @link List} if no values are found.
//   */
//  final def getAllAsString(name: CharSequence): util.List[String] = getAll(name)
//
//  /**
//   * {@link Iterator} that converts each {@link Entry}'s key and value to a {@link String}.
//   */
//  final def iteratorAsString: util.Iterator[util.Map.Entry[String, String]] = iterator
//
//  /**
//   * Returns {@code true} if a header with the {@code name} and {@code value} exists, {@code false} otherwise.
//   * <p>
//   * If {@code ignoreCase} is {@code true} then a case insensitive compare is done on the value.
//   *
//   * @param name       the name of the header to find
//   * @param value      the value of the header to find
//   * @param ignoreCase { @code true} then a case insensitive compare is run to compare values.
//   *                           otherwise a case sensitive compare is run to compare values.
//   */
//  def contains(name: CharSequence, value: CharSequence, ignoreCase: Boolean): Boolean = contains(name.toString, value.toString, ignoreCase)
//
//  override def toString: String = HeadersUtils.toString(getClass, iteratorCharSequence, size)
//
//  /**
//   * Returns a deep copy of the passed in {@link AbstractSipHeaders}.
//   */
//  def copy: AbstractSipHeaders = new DefaultSipHeaders().set(this)
//}
