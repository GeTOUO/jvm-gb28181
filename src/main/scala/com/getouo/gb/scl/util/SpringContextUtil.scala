package com.getouo.gb.scl.util

import org.slf4j.{Logger, LoggerFactory}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.{ApplicationContext, ApplicationContextAware}
import org.springframework.stereotype.Component

import scala.util.{Failure, Success, Try}

/**
 * spring 上下文工具
 */
@Component
class SpringContextUtil extends ApplicationContextAware {
  @Autowired
  override def setApplicationContext(applicationContext: ApplicationContext): Unit = SpringContextUtil.applicationContext = applicationContext
}

/**
 * spring 上下文工具
 */

object SpringContextUtil {

  private var applicationContext: ApplicationContext = _
  private val logger: Logger = LoggerFactory.getLogger(getClass)


  /**
   * 获取applicationContext
   *
   * @return spring应用上下文
   */
  private def getApplicationContext = applicationContext

  /**
   * 通过name获取 Bean.
   *
   * @param name 参数传入要获取的实例的类名 首字母小写，这是默认的
   * @return
   */
  def getBean(name: String): Any = getApplicationContext.getBean(name)

  /**
   *
   * 通过class获取Bean.
   *
   * @param clazz 类型
   * @tparam T 类型
   * @return bean实例
   */
  def getBean[T](clazz: Class[T]): Option[T] = Try(getApplicationContext.getBean(clazz)) match {
    case Failure(exception) => logger.error(s"根据类型[$clazz]获取bean异常:${exception.getMessage}!"); None
    case Success(bean) => Option(bean)
  }

  /**
   * 通过name,以及Clazz返回指定的Bean
   *
   * @param name bean名称
   * @param clazz bean类型
   * @tparam T 类型
   * @return bean实例
   */
  def getBean[T](name: String, clazz: Class[T]): Option[T] = Try(getApplicationContext.getBean(name, clazz)) match {
    case Failure(exception) => logger.error(s"根据bean名[$name]和类型[$clazz]获取bean异常:${exception.getMessage}!"); None
    case Success(bean) => Option(bean)
  }
}
