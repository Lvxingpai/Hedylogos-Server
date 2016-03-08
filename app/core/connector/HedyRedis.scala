package core.connector

import com.redis.RedisClientPool
import play.api.{ Configuration, Play }
import play.api.inject.BindingKey

/**
 * Created by zephyre on 4/21/15.
 */
object HedyRedis {
  val pool = {
    import play.api.Play.current

    val conf = Play.application.injector instanceOf (BindingKey(classOf[Configuration]) qualifiedWith "default")
    val redisBackends = conf.getConfig("services.redis").get
    val redisServices = redisBackends.subKeys.toSeq map (redisBackends.getConfig(_).get)
    val service = redisServices.head

    val dbIndex = conf.getInt("hedylogos.server.redis.db").get

    new RedisClientPool(service.getString("host").get, service.getInt("port").get, database = dbIndex)
  }
}
