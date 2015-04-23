package core

import models.HedyRedis

import scala.collection.mutable.ArrayBuffer


/**
 * 用户相关的操作
 *
 * Created by zephyre on 4/20/15.
 */
object User {
  def userId2key(userId: Long): String = s"$userId.loginInfo"

  def login(userId: Long, regId: String, deviceToken: Option[String]): Unit = {
    HedyRedis.client.hmset(userId2key(userId),
      Map("regId" -> regId, "deviceToken" -> deviceToken.getOrElse(""), "loginTs" -> System.currentTimeMillis,
        "status" -> "login"))
  }

  def logout(userId: Long): Unit = {
    HedyRedis.client.hmset(userId2key(userId),
      Map("logoutTs" -> System.currentTimeMillis, "status" -> "logout"))
  }

  def loginInfo(userId: Long): Option[Map[String, Any]] = {
    val result = HedyRedis.client.hgetall[String, String](userId2key(userId)).get
    val items = ArrayBuffer[(String, Any)]()
    if (result.nonEmpty) {
      Array("regId", "status").foreach(key => items += key -> result(key))
      Array("loginTs", "logoutTs").foreach(key => items += key -> result.get(key).flatMap(v => Some(v.toLong)))

      val dtKey = "deviceToken"
      items += dtKey -> result.get(dtKey)

      Some(items.toMap)
    } else {
      None
    }
  }

  def destroyUser(userId: Long): Unit = HedyRedis.client.del(userId2key(userId))
}