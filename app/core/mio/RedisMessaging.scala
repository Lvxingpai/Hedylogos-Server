package core.mio

import models.{HedyRedis, Message}
import org.bson.types.ObjectId

/**
 * Created by zephyre on 4/22/15.
 */
object RedisMessaging extends MessageDeliever {
  def userId2key(userId: Long): String = s"$userId.fetchInfo"

  override def sendMessage(message: Message, targets: Seq[Long]): Unit =
    targets.foreach(uid => HedyRedis.client.sadd(userId2key(uid), message.getId.toString))

  def fetchMessages(userId: Long): Seq[Message] = {
    val key = userId2key(userId)
    val msgKeys = HedyRedis.client.smembers[String](key).get.filter(_.nonEmpty).map(_.get)
    if (msgKeys.isEmpty)
      Seq[Message]()
    else {
      val msgIds = msgKeys.map(new ObjectId(_: String)).toSeq
      val results = MongoStorage.fetchMessages(msgIds)
      HedyRedis.client.srem(key, "", msgKeys.toSeq: _*)
      results
    }
  }

  def destroyFetchSets(userIds: Seq[Long]): Unit = {
    val keyList = userIds.map(userId2key)
    HedyRedis.client.del("", keyList: _*)
  }
}
