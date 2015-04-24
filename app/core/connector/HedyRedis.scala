package core.connector

import com.redis.RedisClientPool
import core.GlobalConfig


/**
 * Created by zephyre on 4/21/15.
 */
object HedyRedis {
  val clients = {
    val configHost = GlobalConfig.playConf.getString("redis.host").getOrElse("localhost")
    val configPort = GlobalConfig.playConf.getInt("redis.port").getOrElse(6379)
    new RedisClientPool(configHost, configPort)
  }
}
