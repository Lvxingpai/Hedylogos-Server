package core

import com.mongodb.DuplicateKeyException
import core.Implicits.TwitterConverter._
import core.Implicits._

import core.connector.{ HedyRedis, MorphiaFactory }
import core.finagle.FinagleCore
import core.mio.{ GetuiService, MongoStorage, RedisMessaging }
import misc.FinagleFactory
import models.Message.{ ChatType, MessageType }
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

  /**
   * 将指定conversation的msgIdCounter自增，并返回，作为新的msgId
   *
   * @param cid Converastion的Id
   * @return
   */
  def generateMsgId(cid: ObjectId): Future[Option[Long]] = {
    Future {
      val key = s"hedy:conversations/${cid.toString}/msgId"
      HedyRedis.pool.withClient(_.incr(key))
    }
  }

  def buildMessage(msgType: MessageType.Value, contents: String, cid: ObjectId, receiver: Long, sender: Long, chatType: ChatType.Value): Future[Message] =
    generateMsgId(cid) map (v => Message(msgType, contents, cid, v.get, receiver, sender, chatType))

  def buildMessage(msgType: MessageType.Value, contents: String, cidList: Seq[ObjectId], receiver: Long, sender: Long, chatType: ChatType.Value): Future[Seq[Message]] = {
    val ret = cidList map (cid => buildMessage(msgType, contents, cid, receiver, sender, chatType))
    Future.sequence(ret)
  }

  /**
   * 向一个会话发送消息
   *
   * @return
   */
  def sendMessageToConv(msg: Message): Future[Message] = {
    val chatType: ChatType.Value = msg.chatType
    val receiver = msg.receiverId
    val sender = msg.senderId

    val futureTargets = chatType match {
      case item if item.id == ChatType.SINGLE.id => Future(Seq(receiver))
      case item if item.id == ChatType.CHATGROUP.id =>
        FinagleCore.getChatGroup(receiver) map (cg => {
          val members = cg.participants
          Set(members filter (_ != sender): _*).toSeq
        })
    }

    val mongoResult = for {
      targets <- futureTargets
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
   * 生成消息的摘要文本。可以为None
   * @return
   */
  def buildMessageAbbrev(msg: Message): Future[Option[String]] = {
    import Message.MessageType._

    val tailOpt =
      msg.msgType match {
        case item if item == TEXT.id =>
          val maxLen = 16
          // 截断后的消息
          val abbrev = if (msg.contents.length >= maxLen)
            msg.contents.take(maxLen) + "..."
          else
            msg.contents
          Some(s": $abbrev")
        case item if item == IMAGE.id =>
          Some("发来了一张照片")
        case item if item == AUDIO.id =>
          Some("发来了一段语音")
        case item if item == LOCATION.id =>
          Some("发来了一个位置")
        case item if item == GUIDE.id =>
          Some("发来了一篇攻略")
        case item if item == TRAVEL_NOTE.id =>
          Some("发来了一篇游记")
        case item if item == CITY_POI.id =>
          Some("发来了一段目的地介绍")
        case item if item == SPOT.id =>
          Some("发来了一段景点介绍")
        case item if item == RESTAURANT.id =>
          Some("发来了一个餐厅")
        case item if item == SHOPPING.id =>
          Some("发来了一个扫货好去处")
        case item if item == HOTEL.id =>
          Some("发来了一个酒店")
        case _ =>
          None
      }

    val abbrev = tailOpt map (tail =>
      FinagleCore.getUserById(msg.senderId) map (v => (v map (_.nickName) getOrElse "神秘人") + tail))

    abbrev map (future => {
      future map (s => {
        val r: Option[String] = Some(s)
        r
      })
    }) getOrElse Future(None)
  }

  /**
   * 向接收者（可以是个人，也可以是讨论组）发送消息
   *
   * @param chatType 消息类型：单聊和群聊这两种
   * @return
   */
  def sendMessage(msgType: MessageType.Value, contents: String, receiver: Long, sender: Long, chatType: ChatType.Value): Future[Message] = {
    val conversation = chatType match {
      case item if item.id == ChatType.SINGLE.id => Chat.getSingleConversation(sender, receiver)
      case item if item.id == ChatType.CHATGROUP.id => Chat.getChatGroupConversation(receiver)
    }

    for {
      c <- conversation
      msg <- buildMessage(msgType, contents, c.id, receiver, sender, chatType)
      abbrev <- buildMessageAbbrev(msg)
      ret <- {
        msg.abbrev = abbrev.orNull
        sendMessageToConv(msg)
      }
    } yield ret
  }

  def fetchAndAckMessage(userId: Long, purgeBefore: Long): Future[Seq[Message]] = {
    RedisMessaging.removeMessages(userId, purgeBefore)
    RedisMessaging.fetchMessages(userId, purgeBefore)
  }

  val ADD_MUTENOTIF = "addmutenotif"
  val REMOVE_MUTENOTIF = "removemutenotif"
  def opConversationProperty(action: String, uid: Long, cid: ObjectId): Future[Unit] = {
    Future {
      action match {
        case ADD_MUTENOTIF => addMuteNotif(uid, cid)
        case REMOVE_MUTENOTIF => removeMuteNotif(uid, cid)
      }
    }
  }

  /**
   * 添加消息免打扰
   * @param userId 被添加消息免打扰的用户Id
   * @param convId 根据ObjectId查找Conversation
   * @return
   */
  def addMuteNotif(userId: Long, convId: ObjectId): Future[Unit] = {
    val query = ds.find(classOf[Conversation], Conversation.fdId, convId)
    val updateOps = ds.createUpdateOperations(classOf[Conversation]).add(Conversation.fdMuteNotif, userId, false)
    Future {
      ds.updateFirst(query, updateOps)
    }
  }

  /**
   * 取消消息免打扰
   * @param userId 被取消的人Id
   * @param convId 根据ObjectId查找Conversation
   * @return
   */
  def removeMuteNotif(userId: Long, convId: ObjectId): Future[Unit] = {
    val query = ds.find(classOf[Conversation], Conversation.fdId, convId)
    val updateOps = ds.createUpdateOperations(classOf[Conversation]).removeAll(Conversation.fdMuteNotif, userId)
    Future {
      ds.updateFirst(query, updateOps)
    }
  }
}
