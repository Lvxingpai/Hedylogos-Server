package core.mio

import core.connector.HedyRedis
import models.Message
import org.bson.types.ObjectId
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

/**
 * Created by zephyre on 4/22/15.
 */
object RedisMessaging extends MessageDeliever {
  def userId2key(userId: Long): String = s"hedy:users/$userId/mailbox"

  override def sendMessage(message: Message, targets: Seq[Long]): Future[Message] = {
    // 发送给单个用户
    def send2individual(userId: Long): Unit =
      HedyRedis.pool.withClient(_.sadd(userId2key(userId), message.getId.toString))

    val tasks = targets.map(uid => Future {
      send2individual(uid)
    })

    Future.fold(tasks)(Seq())((result, _) => result).map(_ => message)
  }

  def fetchMessages(userId: Long): Future[Seq[Message]] = {
    val key = userId2key(userId)
    val msgKeys = HedyRedis.pool.withClient(_.smembers[String](key).get.filter(_.nonEmpty).map(_.get))
    val msgIds = (msgKeys map (new ObjectId(_))).toSeq
    val messages = MongoStorage.fetchMessages(msgIds)

    messages.map(msgList => HedyRedis.pool.withClient(_.srem(key, "", msgList: _*)))
    messages
  }

  def acknowledge(userId: Long, msgList: Seq[String]): Future[Unit] = {
    Future {
      val key = userId2key(userId)
      HedyRedis.pool.withClient(_.srem(key, "", msgList: _*))
    }
  }
}
