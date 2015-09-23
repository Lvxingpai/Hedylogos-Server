package core

import com.mongodb.DuplicateKeyException
import core.Implicits.TwitterConverter._
import core.Implicits._
import core.connector.{ HedyRedis, MorphiaFactory }
import core.filter.FilterManager
import core.finagle.FinagleCore
import core.mio.{ GetuiService, MongoStorage, RedisMessaging }
import misc.FinagleFactory
import models.Message.{ ChatType, MessageType }
import models._
import org.bson.types.ObjectId
import org.mongodb.morphia.query.UpdateOperations
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.collection.JavaConversions._
import scala.collection.Map
import scala.concurrent.Future
import scala.language.postfixOps

/**
 * Created by zephyre on 4/20/15.
 */
object ChatImpl extends Chat {
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

  def buildMessage(msgType: MessageType.Value, contents: String, cid: ObjectId, receiver: Long, sender: Long, chatType: ChatType.Value): Future[Message] = {
    for {
      msgId <- generateMsgId(cid)
      msg <- Future(Message(msgType, contents, cid, msgId.get, receiver, sender, chatType))
      optAbbrev <- buildMessageAbbrev(msg)
    } yield {
      msg.abbrev = optAbbrev.orNull
      msg
    }
  }

  /**
   * 向一个会话发送消息
   *
   * @return
   */
  def sendMessageToConv(msg: Message, conversation: Conversation, includes: Seq[Long], excludes: Seq[Long]): Future[Message] = {
    val chatType: ChatType.Value = msg.chatType
    val receiver = msg.receiverId
    val sender = msg.senderId

    val futureTargets = chatType match {
      case item if item.id == ChatType.SINGLE.id => Future(Set(receiver))
      case item if item.id == ChatType.CHATGROUP.id =>
        FinagleCore.getChatGroup(receiver) map (cg =>
          (cg.participants filter (_ != sender)).toSet ++ includes -- excludes)
    }

    // 设置了消息免打扰的用户
    val futureMuted = futureTargets map (members => {
      Option(conversation.muteNotif) map (_.toSeq) getOrElse Seq() filter members.contains
    })

    for {
      members <- futureTargets map (_.toSeq)
      msgWithTargets <- futureTargets map (v => {
        msg.targets = (v + sender).toSeq
        msg
      })
      muted <- futureMuted
      _ <- MongoStorage.sendMessage(msgWithTargets, members)
      _ <- RedisMessaging.sendMessage(msgWithTargets, members)
      _ <- GetuiService.sendMesageWithMute(msgWithTargets, members, muted)
    } yield msg
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
  def sendMessage(msgType: MessageType.Value, contents: String, receiver: Long, sender: Long, chatType: ChatType.Value, includes: Seq[Long], excludes: Seq[Long]): Future[Message] = {
    // 是否为单聊
    val isSingleChat = chatType == ChatType.SINGLE

    val conversation = if (isSingleChat)
      getSingleConversation(sender, receiver)
    else
      getChatGroupConversation(receiver)
    // val msgRes = FilterManager.process(buildMessage(msgType, contents, conv.id, receiver, sender, chatType))

    for {
      conv <- conversation
      msg <- buildMessage(msgType, contents, conv.id, receiver, sender, chatType)
      filteredMsg <- FilterManager.process(msg) match {
        // 对消息进行过滤处理，并统一转换为Future
        case v: Future[Message] => v
        case m: Message => Future(m)
      }
      ret <- {
        // 发送消息
        filteredMsg match {
          case msg: Message =>
            if (isSingleChat)
              sendMessageToConv(msg, conv, Seq(), Seq())
            else
              sendMessageToConv(msg, conv, includes, excludes)
        }
      }
    } yield {
      ret
    }
  }

  def fetchAndAckMessage(userId: Long, purgeBefore: Long): Future[Seq[Message]] = {
    RedisMessaging.removeMessages(userId, purgeBefore)
    RedisMessaging.fetchMessages(userId, purgeBefore)
  }

  def opConversationProperty(uid: Long, cid: ObjectId, settings: Map[String, Boolean]): Future[Unit] = {
    val ret = settings.toSeq map (item => item._1 match {
      case "mute" => opMuteNotif(uid, cid, item._2)
    })

    Future.sequence(ret) map (_ => ())
  }
  def opConversationProperty(uid: Long, targetId: Long, settings: Map[String, Boolean]): Future[Unit] = {
    val ret = settings.toSeq map (item => item._1 match {
      case "mute" => opMuteNotif(uid, targetId, item._2)
    })

    Future.sequence(ret) map (_ => ())
  }

  def getConversation(cid: ObjectId): Future[Option[Conversation]] =
    Future(Option(ds.find(classOf[Conversation], Conversation.fdId, cid).get))

  def getConversation(uid: Long, targetId: Long): Future[Option[Conversation]] = {
    val l = Seq(uid, targetId).sorted

    val str = Array(s"${l.head}.${l.last}", targetId.toString)

    Future(Option(ds.createQuery(classOf[Conversation]).field(Conversation.fdFingerprint).in(str.toList).get))
  }
  /**
   * 设置/取消消息免打扰
   * @param userId
   * @param convId
   * @param mute true为免打扰, false为取消免打扰
   * @return
   */
  def opMuteNotif(userId: Long, convId: ObjectId, mute: Boolean): Future[Unit] = {
    val query = ds.find(classOf[Conversation], Conversation.fdId, convId)
    var updateOps: UpdateOperations[Conversation] = null
    mute match {
      case true => updateOps = ds.createUpdateOperations(classOf[Conversation]).add(Conversation.fdMuteNotif, userId, false)
      case false => updateOps = ds.createUpdateOperations(classOf[Conversation]).removeAll(Conversation.fdMuteNotif, userId)
    }
    Future {
      ds.updateFirst(query, updateOps)
    }
  }
  def opMuteNotif(userId: Long, targetId: Long, mute: Boolean): Future[Unit] = {
    val l = Seq(userId, targetId).sorted
    val str = Array(s"${l.head}.${l.last}", targetId.toString)

    val query = ds.createQuery(classOf[Conversation]).field(Conversation.fdFingerprint).in(str.toList)
    var updateOps: UpdateOperations[Conversation] = null
    mute match {
      case true => updateOps = ds.createUpdateOperations(classOf[Conversation]).add(Conversation.fdMuteNotif, userId, false)
      case false => updateOps = ds.createUpdateOperations(classOf[Conversation]).removeAll(Conversation.fdMuteNotif, userId)
    }
    Future {
      ds.updateFirst(query, updateOps)
    }
  }
}
