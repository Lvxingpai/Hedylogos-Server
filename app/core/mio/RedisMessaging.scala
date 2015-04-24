package core.mio

import core.connector.HedyRedis
import models.Message
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
      targets.foreach(uid => HedyRedis.clientsPool.withClient(_.sadd(userId2key(uid), message.getId.toString)))
    }
  }

  def fetchMessages(userId: Long): Future[Seq[Message]] = {
    Future {
      val key = userId2key(userId)
      val msgKeys = HedyRedis.clientsPool.withClient(_.smembers[String](key).get.filter(_.nonEmpty).map(_.get))
      if (msgKeys.isEmpty)
        Seq[Message]()
      else {
        val msgIds = msgKeys.map(new ObjectId(_: String)).toSeq
        val results = Await.result(MongoStorage.fetchMessages(msgIds), Duration.Inf)
        HedyRedis.clientsPool.withClient(_.srem(key, "", msgKeys.toSeq: _*))
        results
      }
    }
  }

  def acknowledge(userId: Long, msgList: Seq[String]): Future[Unit] = {
    Future {
      val key = userId2key(userId)
      HedyRedis.clientsPool.withClient(_.srem(key, "", msgList: _*))
    }
  }

  def destroyFetchSets(userIds: Seq[Long]): Unit = {
    val keyList = userIds.map(userId2key)
    HedyRedis.clientsPool.withClient(_.del("", keyList: _*))
  }

  override def sendMessage(message: Message, target: Seq[Long]): Unit = {
    target.foreach(uid =>
      HedyRedis.clientsPool.withClient(_.sadd(
        userId2key(uid),
        message.getId.toString)))
  }
}
