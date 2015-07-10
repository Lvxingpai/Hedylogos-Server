package core


import com.lvxingpai.yunkai.ChatGroup


import com.mongodb.{ BasicDBObject, BasicDBObjectBuilder, DuplicateKeyException }

import core.connector.{ HedyRedis, MorphiaFactory }
import core.finagle.FinagleCore
import core.mio.{ GetuiService, MongoStorage, RedisMessaging }
import models.Message.MessageType
import models._
import org.bson.types.ObjectId
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.collection.JavaConversions._
import scala.concurrent.Future

/**
 * Created by zephyre on 4/20/15.
 */
object Chat {
  val ds = MorphiaFactory.datastore

  /**
   * 通过id获得conversation信息
   */
  def conversation(cid: ObjectId): Future[Option[Conversation]] = {
    Future {
      val result = ds.find(classOf[Conversation], "id", cid).get()
      Option(result)
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
    assert(userA >= 0 && userB > 0 && userA != userB, s"Invalid users: $userA, $userB")

    Future {
      val c: Conversation = Conversation(userA, userB)

      try {
        ds.save[Conversation](c)
        Some(c)
      } catch {
        case e: DuplicateKeyException =>
          val result = ds.find(classOf[Conversation], Conversation.fdFingerprint, c.getFingerprint).get
          Option(result)
      }
    }
  }

  /**
   * 创建群组的会话
   *
   * @param chatGroup
   * @param create
   * @return
   */
  def chatGroupConversation(chatGroup: ChatGroup, create: Boolean = true): Future[Conversation] = {
    Future {
      val conversation: Conversation = Conversation(chatGroup.id, chatGroup.chatGroupId)
      try {
        ds.save[Conversation](conversation)
        conversation
      } catch {
        case e: DuplicateKeyException =>
          val result = ds.find(classOf[Conversation], Conversation.fdFingerprint, conversation.getFingerprint).get
          result
      }
    }
  }
  def opGroupConversation(chatGroup: ChatGroup, participants: Seq[Long], isAdd: Boolean = true): Future[Unit] = {
    Future {
      val ops = ds.createUpdateOperations(classOf[Conversation])
      if (isAdd)
        ops.addAll(models.Conversation.fdFingerprint, participants, false)
      else
        ops.removeAll(models.Conversation.fdFingerprint, participants)
      ds.updateFirst(ds.createQuery(classOf[Conversation]).field("id").equal(chatGroup.id), ops)
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

  def chatGroupConversation(fingerprint: String): Future[Option[Conversation]] = {
    Future {
      val result = ds.find(classOf[Conversation], Conversation.fdFingerprint, fingerprint).get
      Option(result)
    }
  }

  def buildMessage(msgType: Int, contents: String, cid: ObjectId, receiver: Long, sender: Long, chatType: String): Future[Message] =
    generateMsgId(cid) map (v => Message(MessageType(msgType), contents, cid, v.get, receiver, sender, chatType))

  def buildMessage(msgType: Int, contents: String, cidList: Seq[ObjectId], receiver: Long, sender: Long, chatType: String): Future[Seq[Message]] = {
    def cidList2Message(cids: Seq[ObjectId]): Seq[Message] = {
      for {
        cid <- cids
      } yield {
        val msg = new Message()
        msg.setId(new ObjectId())
        msg.setContents(contents)
        msg.setMsgType(msgType)
        msg.setTimestamp(System.currentTimeMillis)
        msg.setConversation(cid)
        msg.setSenderId(sender)
        msg.setReceiverId(receiver)
        msg.setChatType(chatType)

        val futureMsgId = generateMsgId(cid)
        futureMsgId.map(v => {
          msg.setMsgId(v.get)
          msg
        })
        msg
      }
    }
    val result = Future {
      cidList2Message(cidList)
    }
    result
  }

  // 给群组发消息
  def sendMessage(msgType: Int, contents: String, cid: ObjectId, receiver: Long, sender: Long, chatType: String): Future[Message] = {
    val futureMsg = buildMessage(msgType, contents, cid, receiver, sender, chatType)
    val futureConv = conversation(cid)
    val participants = FinagleCore.getChatGroup(receiver) map (item => item.participants)

    val futureTargets = futureConv flatMap (v => {
      for {
        members <- participants
      } yield Set(members.filter(_ != sender): _*).toSeq
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

  def sendMessage(msgType: Int, contents: String, receiver: Long, sender: Long, chatType: String): Future[Message] = {
    for {
      c <- Chat.singleConversation(sender, receiver)
      msg <- sendMessage(msgType, contents, c.get.getId, receiver, sender, chatType)
    } yield msg
  }

  def fetchMessage(userId: Long): Future[Seq[Message]] = {
    RedisMessaging.fetchMessages(userId)
  }

  def acknowledge(userId: Long, msgIdList: Seq[String]): Future[Unit] = {
    RedisMessaging.acknowledge(userId, msgIdList)
  }

  // 添加消息免打扰
  def addMuteNotif(userId: Long, convId: ObjectId): Future[Unit] = {
    val col = MorphiaFactory.getCollection(classOf[Conversation])
    val query = BasicDBObjectBuilder.start().add(Conversation.fdId, convId).get()
    val ops = new BasicDBObject("$addToSet",
      new BasicDBObject(Conversation.fdMuteNotif,
        BasicDBObjectBuilder.start().add("$each", userId)))
    Future {
      col.update(query, ops, true, false)
    }
  }
  def addMuteNotif(userId: Long, fingerprint: String): Future[Unit] = {
    val col = MorphiaFactory.getCollection(classOf[Conversation])
    val query = BasicDBObjectBuilder.start().add(Conversation.fdFingerprint, fingerprint).get()
    val ops = new BasicDBObject("$addToSet",
      new BasicDBObject(Conversation.fdMuteNotif,
        BasicDBObjectBuilder.start().add("$each", userId)))
    Future {
      col.update(query, ops, true, false)
    }
  }
  // 取消消息免打扰
  def removeMuteNotif(userId: Long, convId: ObjectId): Future[Unit] = {
    val query = ds.find(classOf[Conversation], Conversation.fdFingerprint, convId)
    val updateOps = ds.createUpdateOperations(classOf[Conversation]).removeAll(Conversation.fdMuteNotif, userId)
    Future {
      ds.updateFirst(query, updateOps)
    }
  }
  def removeMuteNotif(userId: Long, fingerprint: String): Future[Unit] = {
    val query = ds.find(classOf[Conversation], Conversation.fdFingerprint, fingerprint)
    val updateOps = ds.createUpdateOperations(classOf[Conversation]).removeAll(Conversation.fdMuteNotif, userId)
    Future {
      ds.updateFirst(query, updateOps)
    }
  }
}
