package core

import core.connector.HedyRedis
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future


/**
 * 用户相关的操作
 *
 * Created by zephyre on 4/20/15.
 */
object User {
  def userId2key(userId: Long): String = s"$userId.loginInfo"

  def login(userId: Long, regId: String, deviceToken: Option[String] = None): Future[Unit] = {
    Future {
      HedyRedis.clients.withClient(_.hmset(userId2key(userId),
        Map("regId" -> regId, "deviceToken" -> deviceToken.getOrElse(""), "loginTs" -> System.currentTimeMillis,
          "status" -> "login"))
      )
    }
  }

  def logout(userId: Long): Future[Unit] = {
    Future {
      HedyRedis.clients.withClient(_.hmset(userId2key(userId),
        Map("logoutTs" -> System.currentTimeMillis, "status" -> "logout")));
    }
  }

  def loginInfo(userId: Long): Future[Option[Map[String, Any]]] = {
    Future {
      val result = HedyRedis.clients.withClient(_.hgetall[String, String](userId2key(userId)).get)
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
  }

  def destroyUser(userId: Long): Unit = HedyRedis.clients.withClient(_.del(userId2key(userId)))
}