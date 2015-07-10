package core.connector

import com.redis.RedisClientPool
import core.GlobalConfig

/**
 * Created by zephyre on 4/21/15.
 */
object HedyRedis {
  val pool = {
    val redisBackends = GlobalConfig.playConf.getConfig("backends.redis").get
    val redisServices = redisBackends.subKeys.toSeq map (redisBackends.getConfig(_).get)
    val service = redisServices.head

    val dbIndex = GlobalConfig.playConf.getInt("hedylogos.server.redis.db").get

    new RedisClientPool(service.getString("host").get, service.getInt("port").get, database = dbIndex)
  }
}
