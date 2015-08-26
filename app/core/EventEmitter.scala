package core

import com.fasterxml.jackson.databind.{ JsonNode, ObjectMapper }
import com.lvxingpai.apium.{ ApiumSeed, ApiumPlant }
import com.lvxingpai.apium.ApiumPlant.ConnectionParam
import org.joda.time.DateTime

import scala.language.postfixOps

/**
 * 事件发布模块
 *
 * Created by zephyre on 6/2/15.
 */
object EventEmitter {

  /**
   * 消息过滤的事件
   */
  val evtFilterMessage = "filterMessage"

  // 初始化
  val apiumPlant = {
    val conf = GlobalConfig.playConf

    // 获得rabbitmq的地址
    val rabbitmqEntries = conf.getConfig("backends.rabbitmq").get
    val servers = rabbitmqEntries.subKeys map (rabbitmqEntries.getConfig(_).get)
    val host = servers.head.getString("host").get
    val port = servers.head.getInt("port").get

    val username = conf.getString("hedylogos.rabbitmq.username").get
    val password = conf.getString("hedylogos.rabbitmq.password").get
    val virtualHost = conf.getString("hedylogos.rabbitmq.virtualhost").get

    ApiumPlant(ConnectionParam(host, port, username, password, virtualHost), "hedylogos", Seq(evtFilterMessage))
  }

  /**
   * 触发事件
   *
   * @param eventName 事件名称
   * @param eventArgs 事件参数。要求是一个scala.collection.immutable.Map[String, JsonNode]类型的对象
   */
  def emitEvent(eventName: String, eventArgs: Map[String, JsonNode], eta: Option[Long] = None, expire: Option[Long] = None) {
    // miscInfo的默认值为{}
    val eventMap = Option(eventArgs) map (m => {
      if (m contains "miscInfo")
        m
      else
        m + ("miscInfo" -> new ObjectMapper().createObjectNode())
    })

    val expireDelta = expire match {
      case None => None
      case Some(v) => Some(DateTime.now plus v)
    }

    val etaDelta = eta match {
      case None => None
      case Some(v) => Some(DateTime.now plus v)
    }

    val seed = ApiumSeed(apiumPlant.defaultTaskName(eventName), kwargs = eventMap, expire = expireDelta, eta = etaDelta)
    apiumPlant.sendSeed(eventName, seed)
  }
}
