package models

import com.redis.RedisClient
import core.GlobalConfig


/**
 * Created by zephyre on 4/21/15.
 */
object HedyRedis {
  var client: RedisClient = null

  def init(host: String = null, port: Int = 0): RedisClient = {
    if (client != null)
      return client

    var configHost = GlobalConfig.playConf.getString("redis.host").getOrElse("localhost")
    var configPort = GlobalConfig.playConf.getInt("redis.port").getOrElse(6379)

    if (host != null) configHost = host
    if (port != 0) configPort = port

    client = new RedisClient(configHost, configPort)
    client
  }
}
