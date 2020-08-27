package com.getouo.gb.scl.util

import java.net.UnknownHostException
import java.net.InetAddress
import java.net.NetworkInterface

import scala.collection.mutable.ArrayBuffer

object NetAddressUtil {

  private def allAddress: Seq[InetAddress] = {
    val arrBuffer: ArrayBuffer[InetAddress] = ArrayBuffer.empty
    val allInterface = NetworkInterface.getNetworkInterfaces
    while (allInterface.hasMoreElements) {
      // 在所有的接口下再遍历IP
      val addresses = allInterface.nextElement.getInetAddresses
      while (addresses.hasMoreElements) {
        arrBuffer.addOne(addresses.nextElement)
      }
    }
    arrBuffer.toSeq
  }

  def netAddress(): Option[InetAddress] =
    allAddress.find(addr => !addr.isSiteLocalAddress() && !addr.isLoopbackAddress && addr.getHostAddress().indexOf(":") == -1)

  def localHostLANAddress: Option[InetAddress] =
    allAddress.find(addr => !addr.isLoopbackAddress && addr.isSiteLocalAddress)

  @throws[UnknownHostException]
  def localAddress: InetAddress = {
    localHostLANAddress.getOrElse{
      allAddress.find(addr => !addr.isLoopbackAddress).getOrElse{
        val jdkSuppliedAddress = InetAddress.getLocalHost
        if (jdkSuppliedAddress == null) throw new UnknownHostException("The JDK InetAddress.getLocalHost() method unexpectedly returned null.")
        jdkSuppliedAddress
      }
    }
  }

  def main(args: Array[String]): Unit = {

    println(allAddress.size)
    println(allAddress.find(addr => !addr.isSiteLocalAddress() && !addr.isLoopbackAddress))
    System.out.println("get LocalHost LAN Address : " + netAddress.map(_.getHostAddress))
    System.out.println("get LocalHost LAN Address : " + localHostLANAddress.map(_.getHostAddress))
    System.out.println("get LocalHost LAN Address : " + localAddress.getHostAddress)

  }
}
