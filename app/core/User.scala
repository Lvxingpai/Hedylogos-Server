package core

import play.api.cache.Cache
import play.api.Play.current


/**
 * 用户相关的操作
 *
 * Created by zephyre on 4/20/15.
 */
object User {
  def login(userId: Long, regId: String, deviceToken: String): Unit = {
    Cache.set(s"$userId", scala.collection.mutable.Map(("regId", regId), ("deviceToken", deviceToken),
      ("loginTime", System.currentTimeMillis())))
  }

  def logout(userId: Long): Unit = {
    val result = Cache.get(s"$userId")
    if (result != null) {
      val userInfo = result.asInstanceOf[scala.collection.mutable.Map[String, Object]]
      userInfo("logoutTime") = System.currentTimeMillis().asInstanceOf[Object]
      Cache.set(s"$userId", userInfo)
    }
  }

  def loginInfo(userId: Long): scala.collection.mutable.Map[String, Object] = {
    val result = Cache.get(s"$userId")
    if (result != null) result.asInstanceOf[scala.collection.mutable.Map[String, Object]]
    else null
  }
}
