package core

import core.exception.UserException
import models.{HedyRedis, UserReg}
import play.api.Play.current
import play.api.cache.Cache


/**
 * 用户相关的操作
 *
 * Created by zephyre on 4/20/15.
 */
object User {
  def login(userId: Long, regId: String, deviceToken: String = null): Unit = {
    val userReg = UserReg(userId, regId, deviceToken)
    Cache.set(s"$userId", userReg)
  }

  def logout(userId: Long): Unit = {
    Cache.get(s"$userId").orNull.asInstanceOf[UserReg].logoutTime = System.currentTimeMillis()
  }

  def loginInfo(userId: Long): UserReg = {
    val result = Cache.get(s"$userId").orNull
    if (result != null) result.asInstanceOf[UserReg]
    else null
  }

  /**
   * 获得用户的msgCounter
   *
   */
  def getMsgCounter(userId: Long): Long = {
    val client = HedyRedis.client
    val key = s"$userId.counter"
    val tmp = client.get(key).orNull
    if (tmp != null) tmp.toInt
    else
      throw new UserException(-1, "Invalid userId")
  }

  /**
   * 对用户的msgCounter做INCR操作
   *
   * @param increment 增量
   * @param oldValue  是否返回旧的msgCounter
   * @return
   */
  def incrMsgCounter(userId: Long, increment: Long = 1, oldValue: Boolean = false): Long = {
    val client = HedyRedis.client
    val key = s"$userId.counter"
    val result: Long = (if (increment == 1) client.incr _ else client.incrby(_: String, increment.toInt))(key).get
    if (oldValue) result - increment else result
  }

  /**
   * 对用户的msgCounter做批量INCR操作
   *
   * @param increment 增量
   * @param oldValue  是否返回旧的msgCounter
   * @return
   */
  def incrMsgCounter(userIdList: Seq[Long], increment: Long, oldValue: Boolean): Seq[Long] = {
    userIdList.map(incrMsgCounter(_, increment, oldValue))
  }
}
