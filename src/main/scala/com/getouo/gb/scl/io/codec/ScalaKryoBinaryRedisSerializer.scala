package com.getouo.gb.scl.io.codec

import com.twitter.chill.KryoPool
import org.springframework.data.redis.serializer.{RedisSerializer, SerializationException}

class ScalaKryoBinaryRedisSerializer[T](kryoPool: KryoPool) extends RedisSerializer[T] {

  @throws[SerializationException]
  override def serialize(t: T): Array[Byte] = kryoPool.toBytesWithClass(t)

  @throws[SerializationException]
  override def deserialize(bytes: Array[Byte]): T = {
    val nul: T = null.asInstanceOf[T]
    if (bytes == null || bytes.length == 0) nul
    else kryoPool.fromBytes(bytes).asInstanceOf[T]
  }

  override def canSerialize(`type`: Class[_]): Boolean = true
}
