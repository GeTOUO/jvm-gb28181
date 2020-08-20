package com.getouo.gb.scl.service

import java.util
import java.util.Set
import java.util.concurrent.TimeUnit

import com.getouo.gb.scl.util.LogSupport
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service

@Service
class RedisService(redisTemplate: RedisTemplate[String, Any]) extends LogSupport {

  private val LOGGER = logger

//  @Autowired
//  private var redisTemplate: RedisTemplate[String, String] = _

  /**
   * 获取Set集合数据
   *
   * @param k
   * @return Set[String]
   */
  def getSets[V](k: String): util.Set[V] = {
    redisTemplate.opsForSet.members(k).asInstanceOf[util.Set[V]]
  }

  /**
   * 移除Set集合中的value
   *
   * @param k
   * @param v
   */
  def removeSetValue(k: String, v: Object): Unit = {
    if (k == null && v == null) {
      return
    }
    redisTemplate.opsForSet().remove(k, v)
  }

  /**
   * 保存到Set集合中
   *
   * @param k
   * @param v
   */
  def setSet[V](k: String, v: V): Unit = {
    if (k == null && v == null) {
      return
    }
    redisTemplate.opsForSet().add(k, v)
  }

  def getMap[HV](key: String, hashKey: String): Option[HV] = {
    Option(redisTemplate.opsForHash().get(key, hashKey))
  }

  /**
   * 存储Map格式
   *
   * @param key
   * @param hashKey
   * @param hashValue
   *
   */
  def setMap[HV](key: String, hashKey: String, hashValue: HV): Unit = {
    redisTemplate.opsForHash().put(key, hashKey, hashValue)
  }

//  def setMapTimeLimit[HV](key: String, hashKey: String, hashValue: HV): Unit = {
//    redisTemplate.opsForHash().
//  }

  /**
   * 存储带有过期时间的key-value
   *
   * @param key
   * @param value
   * @param timeOut 过期时间
   * @param unit    时间单位
   *
   */
  def setKVTimeLimit[V](key: String, value: V, timeOut: Long, unit: TimeUnit): Unit = {
    if (value == null) {
      LOGGER.info("redis存储的value的值为空")
      throw new IllegalArgumentException("redis存储的value的值为空")
    }
    if (timeOut > 0) {
      redisTemplate.opsForValue().set(key, value, timeOut, unit)
      expire(key, timeOut, unit)
    } else {
      redisTemplate.opsForValue().set(key, value)
      persistent(key)
    }
  }

  def persistent(key: String): Unit = redisTemplate.persist(key)

  def expire(key: String, timeOut: Long, unit: TimeUnit): Unit = redisTemplate.expire(key, timeOut, unit)

  /**
   * 存储key-value
   *
   * @param key
   * @return Object
   *
   */
  def setKV[V](key: String, value: V): Unit = {
    if (value == null) {
      LOGGER.info("redis存储的value的值为空")
      throw new IllegalArgumentException("redis存储的value的值为空")
    }
    redisTemplate.opsForValue().set(key, value)
    persistent(key)
  }

  /**
   * 根据key获取value
   *
   * @param key
   * @return Object
   *
   */
  def get[V](key: String): V = redisTemplate.opsForValue().get(key).asInstanceOf[V]

  /**
   * 判断key是否存在
   *
   * @param key
   * @return Boolean
   *
   */
  def exists(key: String): Boolean = redisTemplate.hasKey(key)


  /**
   * 删除key对应的value
   *
   * @param key
   *
   */
  def removeValue(key: String): Unit = if (exists(key)) redisTemplate.delete(key)

  /**
   * 模式匹配批量删除key
   *
   * @param keyPattern
   *
   */
  def removePattern(keyPattern: String) = {
    val keys: Set[String] = redisTemplate.keys(keyPattern)
    if (keys.size() > 0) redisTemplate.delete(keys)
  }

}