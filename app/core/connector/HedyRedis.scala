package core.connector

import com.redis.RedisClientPool
import core.GlobalConfig

import scala.collection.JavaConversions._


/**
 * Created by zephyre on 4/21/15.
 */
object HedyRedis {
  val clients = {
    val tmp = GlobalConfig.playConf.getConfig("backends.rabbitmq").entrySet().toSeq.head.getValue.toString.split("/")
    val host = tmp(0)
    val port = tmp(1).toInt
    new RedisClientPool(host, port)
  }
}
