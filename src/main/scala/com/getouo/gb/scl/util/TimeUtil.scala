package com.getouo.gb.scl.util

import java.net.InetAddress

import org.apache.commons.net.ntp.NTPUDPClient

object TimeUtil {
  val timeClient: NTPUDPClient  = new NTPUDPClient()


  def ntpTimeMillisStamp(): Long = {
    val timeInfo = timeClient.getTime(InetAddress.getByName("ntp.baijinshan.cn"))
    val timeStamp = timeInfo.getMessage.getTransmitTimeStamp
    timeStamp.getDate.getTime
  }

  def ntpTimeSecStamp(): Int = {
    (ntpTimeMillisStamp() / 1000).toInt
  }

  def currentMicTime(): Long = {
    val cutime = System.currentTimeMillis * 1000 // 微秒
    val nanoTime = System.nanoTime // 纳秒
    cutime + (nanoTime - nanoTime / 1000000 * 1000000) / 1000
  }
}
