package core

import com.mongodb.DuplicateKeyException
import core.TwitterConverter._
import core.connector.{ HedyRedis, MorphiaFactory }
import core.finagle.FinagleCore
import core.mio.{ GetuiService, MongoStorage, RedisMessaging }
import misc.FinagleFactory
import models.Message.MessageType
import models._
import org.bson.types.ObjectId
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.language.postfixOps

/**
 * Created by zephyre on 4/20/15.
 */
object Chat {
  val ds = MorphiaFactory.datastore

  //  /**
  //   * 通过id获得conversation信息
  //   */
  //  def conversation(cid: ObjectId): Future[Option[Conversation]] = {
  //    Future {
  //      val result = ds.find(classOf[Conversation], "id", cid).get()
  //      Option(result)
  //    }
  //  }

  def destroyConversation(cid: ObjectId): Unit = {
    ds.delete(classOf[Conversation], cid)
    HedyRedis.clients.withClient(_.del(s"$cid.msgId"))
  }

  /**
   * 获得一个单聊的Conversation
   *
   * @return
   */
  def getSingleConversation(userA: Long, userB: Long): Future[Conversation] = {
    assert(userA >= 0 && userB > 0 && userA != userB, s"Invalid users: $userA, $userB")

    // 根据userA和userB查找
    val sortedUsers = (userA :: userB :: Nil).sorted
    val fingerprint = s"${sortedUsers.head}.${sortedUsers.last}"
    val query = ds.createQuery(classOf[Conversation]) field Conversation.fdFingerprint equal fingerprint
    val existedConversation = Future {
      Option(query.get)
    }

    existedConversation map (opt => {
      opt getOrElse {
        val c: Conversation = Conversation(userA, userB)
        try {
          ds.save[Conversation](c)
          c
        } catch {
          case e: DuplicateKeyException =>
            ds.find(classOf[Conversation], Conversation.fdFingerprint, c.getFingerprint).get
        }
      }
    })
  }

  /**
   * 获得一个群聊的Conversation
   *
   * @return
   */
  def getChatGroupConversation(chatGroupId: Long): Future[Conversation] = {
    assert(chatGroupId > 0, s"Invalid chatGroupId: $chatGroupId")

    // 根据chatGroupId查找
    val fingerprint = s"$chatGroupId"
    val query = ds.createQuery(classOf[Conversation]) field Conversation.fdFingerprint equal fingerprint
    val existedConversation = Future {
      Option(query.get)
    }

    def buildChatGroupConversation(gid: Long): Future[Conversation] = {
      val future = FinagleFactory.client.getChatGroup(gid, None) map (g => {
        val c = Conversation(new ObjectId(g.id), gid)
        try {
          ds.save[Conversation](c)
          c
        } catch {
          case _: DuplicateKeyException =>
            ds.find(classOf[Conversation], Conversation.fdFingerprint, c.fingerprint).get
        }
      })
      future
    }

    for {
      opt <- existedConversation
      conversation <- opt map (Future(_)) getOrElse buildChatGroupConversation(chatGroupId)
    } yield conversation
  }

  //  /**
  //   * 创建群组的会话
  //   *
  //   * @param chatGroup
  //   * @param create
  //   * @return
  //   */
  //  def chatGroupConversation(chatGroup: ChatGroup, create: Boolean = true): Future[Conversation] = {
  //    Future {
  //      val conversation: Conversation = Conversation(chatGroup.id, chatGroup.chatGroupId)
  //      try {
  //        ds.save[Conversation](conversation)
  //        conversation
  //      } catch {
  //        case e: DuplicateKeyException =>
  //          val result = ds.find(classOf[Conversation], Conversation.fdFingerprint, conversation.getFingerprint).get
  //          result
  //      }
  //    }
  //  }

  //  def opGroupConversation(chatGroup: ChatGroup, participants: Seq[Long], isAdd: Boolean = true): Future[Unit] = {
  //    Future {
  //      val ops = ds.createUpdateOperations(classOf[Conversation])
  //      if (isAdd)
  //        ops.addAll(models.Conversation.fdFingerprint, participants, false)
  //      else
  //        ops.removeAll(models.Conversation.fdFingerprint, participants)
  //      ds.updateFirst(ds.createQuery(classOf[Conversation]).field("id").equal(chatGroup.id), ops)
  //    }
  //  }

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

  //  def chatGroupConversation(fingerprint: String): Future[Option[Conversation]] = {
  //    Future {
  //      val result = ds.find(classOf[Conversation], Conversation.fdFingerprint, fingerprint).get
  //      Option(result)
  //    }
  //  }

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

  /**
   * 向一个会话发送消息
   *
   * @return
   */
  def sendMessageToConv(msgType: Int, contents: String, cid: ObjectId, receiver: Long, sender: Long, chatType: String): Future[Message] = {
    val futureMsg = buildMessage(msgType, contents, cid, receiver, sender, chatType)
    val futureTargets = chatType match {
      case "single" => Future(Seq(receiver))
      case "group" =>
        FinagleCore.getChatGroup(receiver) map (cg => {
          val members = cg.participants
          Set(members filter (_ != sender): _*).toSeq
        })
    }

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

  /**
   * 向接收者（可以是个人，也可以是讨论组）发送消息
   *
   * @param chatType 消息类型：单聊和群聊这两种
   * @return
   */
  def sendMessage(msgType: Int, contents: String, receiver: Long, sender: Long, chatType: String): Future[Message] = {
    val conversation = chatType match {
      case "single" => Chat.getSingleConversation(sender, receiver)
      case "group" => Chat.getChatGroupConversation(receiver)
    }

    for {
      c <- conversation
      msg <- sendMessageToConv(msgType, contents, c.id, receiver, sender, chatType)
    } yield msg
  }

  def fetchMessage(userId: Long): Future[Seq[Message]] = {
    RedisMessaging.fetchMessages(userId)
  }

  def acknowledge(userId: Long, msgIdList: Seq[String]): Future[Unit] = {
    RedisMessaging.acknowledge(userId, msgIdList)
  }
}
