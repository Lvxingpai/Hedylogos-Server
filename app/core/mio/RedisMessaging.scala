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
      HedyRedis.pool.withClient(_.zadd(userId2key(userId), message.timestamp, message.getId.toString))

    val tasks = targets.map(uid => Future {
      send2individual(uid)
    })

    Future.fold(tasks)(Seq())((result, _) => result).map(_ => message)
  }

  /**
   * 从mailbox里面取出消息（timestamp之后的）
   * @return
   */
  def fetchMessages(userId: Long, timestamp: Long = 0): Future[Seq[Message]] = {
    val key = userId2key(userId)

    val future = Future {
      val ret = for {
        l <- HedyRedis.pool.withClient(_.zrangebyscore[String](key, min = timestamp, minInclusive = false, limit = None))
      } yield l.toSeq map (new ObjectId(_))
      ret getOrElse Seq()
    }

    future flatMap MongoStorage.fetchMessages
  }

  /**
   * 从mailbox里面清除消息（timestamp之前的）
   * @param userId
   * @param timestamp
   * @return
   */
  def removeMessages(userId: Long, timestamp: Long = 0): Future[Unit] = {
    val key = userId2key(userId)

    Future {
      HedyRedis.pool.withClient(_.zremrangebyscore(key, end = timestamp + 0.01))
    }
  }
}
