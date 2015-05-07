package core

import java.util.Arrays

import com.mongodb.DuplicateKeyException
import core.connector.{HedyRedis, MorphiaFactory}
import core.mio.{GetuiService, MongoStorage, RedisMessaging}
import models._
import org.bson.types.ObjectId
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.collection.JavaConversions._
import scala.concurrent.Future

/**
 * Created by zephyre on 4/20/15.
 */
object Chat {
  val ds = MorphiaFactory.getDatastore()

  /**
   * 通过id获得conversation信息
   */
  def conversation(cid: ObjectId): Future[Option[Conversation]] = {
    Future {
      val result = ds.find(classOf[Conversation], "id", cid).get()
      if (result != null) Some(result) else None
    }
  }

  def destroyConversation(cid: ObjectId): Unit = {
    ds.delete(classOf[Conversation], cid)
    HedyRedis.clients.withClient(_.del(s"$cid.msgId"))
  }

  /**
   * 获得一个单聊的Conversation
   *
   * @param create  如果该conversation不存在，是否新建？
   * @return
   */
  def singleConversation(userA: Long, userB: Long, create: Boolean = true): Future[Option[Conversation]] = {
    assert(userA > 0 && userB > 0 && userA != userB, s"Invalid users: $userA, $userB")

    Future {
      val c: Conversation = Conversation.create(userA, userB)

      try {
        ds.save[Conversation](c)
        Some(c)
      } catch {
        case e: DuplicateKeyException =>
          val result = ds.find(classOf[Conversation], Conversation.FD_FINGERPRINT, c.getFingerprint).get
          if (result != null) Some(result) else None
      }
    }
  }

  /**
   * 创建群组的会话
   *
   * @param group
   * @param create
   * @return
   */
  def groupConversation(group: models.Group, create: Boolean = true): Future[Conversation] = {
    Future {
      val c: Conversation = new Conversation
      c.setId(group.getId)
      c.setFingerprint(group.getGroupId.toString)
      c.setParticipants(group.getParticipants)
      c.setMsgCounter(0L)
      c.setCreateTime(System.currentTimeMillis())
      c.setUpdateTime(System.currentTimeMillis())
      try {
        ds.save[Conversation](c)
        c
      } catch {
        case e: DuplicateKeyException =>
          val result = ds.find(classOf[Conversation], Conversation.FD_FINGERPRINT, c.getFingerprint).get
          result
      }
    }
  }

  def opGroupConversation(group: models.Group, participants: Seq[Long], isAdd: Boolean = true): Future[Unit] = {
    Future {
      val ops = ds.createUpdateOperations(classOf[Conversation])
      if (isAdd)
        ops.addAll(models.Conversation.FD_PARTICIPANTS, participants, false)
      else
        ops.removeAll(models.Conversation.FD_PARTICIPANTS, participants)
      ds.updateFirst(ds.createQuery(classOf[Conversation]).field("id").equal(group.getId), ops)
    }
  }

  /**
   * 将指定conversation的msgIdCounter自增，并返回，作为新的msgId
   *
   * @param cid Converastion的Id
   * @return
   */
  def generateMsgId(cid: ObjectId): Future[Option[Long]] = {
    Future {
      HedyRedis.clients.withClient(_.incr(s"$cid.msgId"))
    }
  }

  /**
   * 获得指定conversation的当前msgId
   *
   * @param cid Conversation的Id
   * @return
   */
  def msgId(cid: ObjectId): Future[Option[Long]] = {
    Future {
      val result: Option[String] = HedyRedis.clients.withClient(_.get(s"$cid.msgId"))
      if (result != None) Some(result.get.toLong) else None
    }
  }

  def groupConversation(fingerprint: String): Future[Option[Conversation]] = {
    Future {
      val result = ds.find(classOf[Conversation], Conversation.FD_FINGERPRINT, fingerprint).get
      if (result != null) Some(result) else None
    }
  }

  def buildMessage(msgType: Int, contents: String, cid: ObjectId, sender: Long): Future[Message] = {
    val msg = new Message()
    msg.setId(new ObjectId())
    msg.setContents(contents)
    msg.setMsgType(msgType)
    msg.setTimestamp(System.currentTimeMillis)
    msg.setConversation(cid)
    msg.setSenderId(sender)

    val futureMsgId = generateMsgId(cid)
    futureMsgId.map(v => {
      msg.setMsgId(v.get)
      msg
    })
  }


  def sendMessage(msgType: Int, contents: String, cid: ObjectId, sender: Long): Future[Message] = {
    val futureMsg = buildMessage(msgType, contents, cid, sender)
    val futureConv = conversation(cid)
    val futureTargets = futureConv.map(v => {
      Set(v.get.getParticipants.filter(_ != sender).map(scala.Long.unbox(_)): _*).toSeq
    })

    val mongoResult = for {
      targets <- futureTargets
      msg <- futureMsg
      result <- MongoStorage.sendMessage(msg, targets)
    } yield result

    for {
      targets <- futureTargets
      msg <- mongoResult
      redisResult <- RedisMessaging.sendMessage(msg, targets)
      getuiResult <- GetuiService.sendMessage(redisResult, targets)
    } yield getuiResult

    mongoResult
  }

  def sendMessage(msgType: Int, contents: String, receiver: Long, sender: Long): Future[Message] = {
    for {
      c <- Chat.singleConversation(sender, receiver)
      msg <- sendMessage(msgType, contents, c.get.getId, sender)
    } yield msg
  }

  def fetchMessage(userId: Long): Future[Seq[Message]] = {
    RedisMessaging.fetchMessages(userId)
  }

  def acknowledge(userId: Long, msgIdList: Seq[String]): Future[Unit] = {
    RedisMessaging.acknowledge(userId, msgIdList)
  }
}
