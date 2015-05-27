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
  def userId2key(userId: Long): String = s"$userId.fetchInfo"

  override def sendMessage(message: Message, targets: Seq[Long]): Future[Message] = {

    // 发送给单个用户
    def send2individual(userId: Long): Unit =
      HedyRedis.clients.withClient(_.sadd(userId2key(userId), message.getId.toString))

    val tasks = targets.map(uid => Future {
      send2individual(uid)
    })

    Future.fold(tasks)(Seq())((result, _) => result).map(_ => message)
  }

  def sendMessageList(message: Seq[Message], targets: Seq[Long]): Future[Seq[Message]] = {

    //    def msgAndTet2Map(message: Seq[Message], targets: Seq[Long]):Map[Long,Message] ={
    //
    //    }
    // TODO
    // 发送给多个用户
    def send2Mul(userId: Long): Unit =
      HedyRedis.clients.withClient(_.sadd(userId2key(userId), message(0).getId.toString))

    val tasks = targets.map(uid => Future {
      send2Mul(uid)
    })

    Future.fold(tasks)(Seq())((result, _) => result).map(_ => message)
  }

  def fetchMessages(userId: Long): Future[Seq[Message]] = {
    val key = userId2key(userId)
    val msgKeys = HedyRedis.clients.withClient(_.smembers[String](key).get.filter(_.nonEmpty).map(_.get))
    if (msgKeys.isEmpty)
      Future(Seq[Message]())
    else {
      val msgIds = msgKeys.map(new ObjectId(_: String)).toSeq
      val messages = MongoStorage.fetchMessages(msgIds)

      messages.map(msgList => HedyRedis.clients.withClient(_.srem(key, "", msgList.toSeq: _*)))
      messages
    }
  }

  def acknowledge(userId: Long, msgList: Seq[String]): Future[Unit] = {
    Future {
      val key = userId2key(userId)
      HedyRedis.clients.withClient(_.srem(key, "", msgList: _*))
    }
  }

  def destroyFetchSets(userIds: Seq[Long]): Unit = {
    val keyList = userIds.map(userId2key)
    HedyRedis.clients.withClient(_.del("", keyList: _*))
  }

}
