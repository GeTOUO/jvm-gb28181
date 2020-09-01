package com.getouo.gb.scl.stream

trait SourceId {
  def idHash(): Long = Integer.toUnsignedLong(this.hashCode)
}

case class FileSourceId(file: String, setupTime: Long) extends SourceId

/**
 * 国标资源id
 * @param deviceId 设备id
 * @param channelId 通道id
 * @param setupTime 启用时间， 若是实时点播则为0，回放则为时间戳
 */
case class GBSourceId(deviceId: String, channelId: String, setupTime: Long = 0) extends SourceId
