package core.mio

import models.{HedyRedis, Message}
import org.bson.types.ObjectId
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
 * Created by zephyre on 4/22/15.
 */
object RedisMessaging extends MessageDeliever {
  def userId2key(userId: Long): String = s"$userId.fetchInfo"

  override def sendMessageAsync(message: Message, targets: Seq[Long]): Future[Unit] = {
    Future {
      targets.foreach(uid => HedyRedis.client.sadd(userId2key(uid), message.getId.toString))
    }
  }

  def fetchMessages(userId: Long): Future[Seq[Message]] = {
    Future {
      val key = userId2key(userId)
      val msgKeys = HedyRedis.client.smembers[String](key).get.filter(_.nonEmpty).map(_.get)
      if (msgKeys.isEmpty)
        Seq[Message]()
      else {
        val msgIds = msgKeys.map(new ObjectId(_: String)).toSeq
        val results = Await.result(MongoStorage.fetchMessages(msgIds), Duration.Inf)
        HedyRedis.client.srem(key, "", msgKeys.toSeq: _*)
        results
      }
    }
  }

  def acknowledge(userId: Long, msgList: Seq[String]): Unit = {
    val key = userId2key(userId)
    HedyRedis.client.srem(key, "", msgList: _*)
  }

  def destroyFetchSets(userIds: Seq[Long]): Unit = {
    val keyList = userIds.map(userId2key)
    HedyRedis.client.del("", keyList: _*)
  }

  override def sendMessage(message: Message, target: Seq[Long]): Unit = {
    target.foreach(uid =>
      HedyRedis.client.sadd(
        userId2key(uid),
        message.getId.toString))
  }
}
