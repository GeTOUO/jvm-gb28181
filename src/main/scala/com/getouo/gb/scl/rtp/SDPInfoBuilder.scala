package com.getouo.gb.scl.rtp

/**
 * SDP协议
 *
 * 一.格式
 * SDP 信息是文本信息，采用 UTF-8 编 码中的ISO 10646 字符集。SDP 会话描述如下：（标注 * 符号的表示可选字段）：
 * ·      v = （协议版本）
 * 格式为 o=<用户名> <会话id> <会话版本> <网络类型><地址类型> <地址>
 * ·      o = （所有者/创建者和会话标识符）
 *
 * ·      s = （会话名称）
 *
 * ·      i = * （会话信息）
 *
 * ·      u = * （URI 描述）
 *
 * ·      e = * （Email 地址）
 *
 * ·      p = * （电话号码）
 *
 * ·      c = * （连接信息 ― 如果包含在所有媒体中，则不需要该字段）
 *
 * ·      b = * （带宽信息）
 *
 * 一个或更多时间描述（如下所示）：
 *
 * ·      z = * （时间区域调整）
 *
 * ·      k = * （加密密钥）
 *
 * ·      a = * （0 个或多个会话属性行）
 *
 * ·      0个或多个媒体描述（如下所示）
 *
 * 时间描述
 *
 * ·      t = （会话活动时间）
 *
 * ·      r = * （0或多次重复次数）
 *
 * 媒体描述
 *
 * ·      m = （媒体名称和传输地址）
 *
 * ·      i = * （媒体标题）
 *
 * ·      c = * （连接信息 — 如果包含在会话层则该字段可选）
 *
 * ·      b = * （带宽信息）
 *
 * ·      k = * （加密密钥）
 *
 * ·      a = * （0 个或多个会话属性行）
 */
object SDPInfoBuilder {

  def build(localIp: String, controlNo: String = "track0"): String = {
    s"""
       |v=0
       |o=- ${System.currentTimeMillis()} 1 IN IP4 ${localIp}
       |s=CounterPath X-Lite 3.0
       |c=IN IP4 218.107.241.235
       |t=0 0
       |a=control:*
       |m=video 0 RTP/AVP 96
       |a=rtpmap:96 H264/90000
       |a=framerate:25
       |a=control:${controlNo}
       |""".stripMargin
  }

}
