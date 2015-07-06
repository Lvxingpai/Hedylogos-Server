package core.cache

import com.redis.{RedisClient, RedisClientPool}

/**
 * Created by topy on 2015/6/23.
 */
object RedisCache extends App{

  val clients = {
  //192.168.100.15:11211
    new RedisClientPool("192.168.100.15", 11211)
  }

  override def main(args:Array[String]) = {

    val r = new RedisClient("192.168.100.10", 6379)
    r.set("key", "some value")
    println("Test")
    println(r.get("key"))
  }

}
