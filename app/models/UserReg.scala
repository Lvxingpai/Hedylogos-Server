package models

/**
 * Created by zephyre on 4/20/15.
 */

/**
 * 用户在系统中注册的regId和token等信息
 */
class UserReg(val userId: Long, val regId: String, val deviceToken: String = null) {
  val loginTime = System.currentTimeMillis()
  var logoutTime: Long = 0
}

object UserReg {
  def apply(userId: Long, regId: String, deviceToken: String = null): UserReg =
    new UserReg(userId, regId, deviceToken)
}
