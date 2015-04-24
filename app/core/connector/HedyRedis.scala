package core.connector

import com.redis.{RedisClientPool, RedisClient}
import core.GlobalConfig


/**
 * Created by zephyre on 4/21/15.
 */
object HedyRedis {
//  def client: RedisClient = init()

  val clientsPool = {
    val configHost = GlobalConfig.playConf.getString("redis.host").getOrElse("localhost")
    val configPort = GlobalConfig.playConf.getInt("redis.port").getOrElse(6379)
    new RedisClientPool(configHost, configPort)
  }

  def init(host: String = null, port: Int = 0): RedisClient = {
    var configHost = GlobalConfig.playConf.getString("redis.host").getOrElse("localhost")
    var configPort = GlobalConfig.playConf.getInt("redis.port").getOrElse(6379)

    if (host != null) configHost = host
    if (port != 0) configPort = port

    new RedisClient(configHost, configPort)
  }
}
