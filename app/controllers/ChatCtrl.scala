package controllers

import com.fasterxml.jackson.databind.{ JsonNode, ObjectMapper }
import core.Chat
import core.Implicits._
import core.aspectj.WithAccessLog
import core.exception.{ BlackListException, ContactException, GroupMemberException }
import core.formatter.serializer.{ ConversationSerializer, MessageSerializer, ObjectMapperFactory }
import models.{ Conversation, Message }
import org.bson.types.ObjectId
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{ Action, Controller, Result, Results }

import scala.collection.Map
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.language.postfixOps

/**
 * Created by zephyre on 4/23/15.
 */
object ChatCtrl extends Controller {

  case class MessageInfo(senderId: Long, chatType: String, receiverId: Long, msgType: Int, contents: Option[String])

  def sendMessageBase(msgType: Int, contents: String, receiver: Long, sender: Long, chatType: String, includes: Seq[Long] = Seq(), excludes: Seq[Long] = Seq()): Future[Result] = {
    val futureMessage = Chat.sendMessage(msgType, contents, receiver, sender, chatType, includes, excludes)

    val results = futureMessage map (msg => {
      val mapper = new ObjectMapper()
      val node = mapper.createObjectNode()
      node put ("conversation", msg.conversation.toString) put ("msgId", msg.msgId) put ("timestamp", msg.timestamp)
      HedyResults(data = Some(node))
    })

    results recover {
      case e: BlackListException =>
        HedyResults.forbidden(HedyResults.RetCode.FORBIDDEN_BLACKLIST, errorMsg = Some(e.errorMsg))
      case e: ContactException =>
        HedyResults.forbidden(HedyResults.RetCode.FORBIDDEN_NOTCONTACT, errorMsg = Some(e.errorMsg))
      case e: GroupMemberException =>
        HedyResults.forbidden(HedyResults.RetCode.FORBIDDEN_NOTMEMBER, errorMsg = Some(e.errorMsg))
    }
  }

  @WithAccessLog
  def updateConversationPropertyById(uid: Long, cid: String) = Action.async {
    request =>
      {
        val result = for {
          body <- request.body.asJson
        } yield {
          val muteOpt = (body \ "mute").asOpt[Boolean]
          val settings = Map("mute" -> muteOpt).filter(_._2 nonEmpty).mapValues(_.get)
          Chat.opConversationProperty(uid, new ObjectId(cid), settings) map (_ => HedyResults())
        }
        result getOrElse Future(Results.BadRequest)
      }
  }

  @WithAccessLog
  def updateConversationProperty(uid: Long, targetId: Long) = Action.async {
    request =>
      {
        val result = for {
          body <- request.body.asJson
        } yield {
          val muteOpt = (body \ "mute").asOpt[Boolean]
          val settings = Map("mute" -> muteOpt).filter(_._2 nonEmpty).mapValues(_.get)
          Chat.opConversationProperty(uid, targetId, settings) map (_ => HedyResults())
        }
        result getOrElse Future(Results.BadRequest)
      }
  }

  @WithAccessLog
  def sendMessage() = Action.async {
    request =>
      {
        val ret = for {
          jsonNode <- request.body.asJson
          senderId <- (jsonNode \ "sender").asOpt[Long]
          receiverId <- (jsonNode \ "receiver").asOpt[Long]
          chatType <- (jsonNode \ "chatType").asOpt[String]
          msgType <- (jsonNode \ "msgType").asOpt[Int]
          contents <- (jsonNode \ "contents").asOpt[String]
        } yield {
          // includes和excludes是可选项目
          val includes = (jsonNode \ "includes").asOpt[Seq[Long]] getOrElse Seq()
          val excludes = (jsonNode \ "excludes").asOpt[Seq[Long]] getOrElse Seq()
          sendMessageBase(msgType, contents, receiverId, senderId, chatType, includes, excludes)
        }
        ret getOrElse Future(HedyResults.unprocessable())
      }
  }

  @WithAccessLog
  def fetchMessages(userId: Long) = Action.async(request => {
    val ret = for {
      body <- request.body.asJson
      timestamp <- (body \ "purgeBefore").asOpt[Long] orElse Some(0L)
    } yield {
      for {
        msgList <- Chat.fetchAndAckMessage(userId, timestamp)
      } yield {
        val mapper = ObjectMapperFactory().addSerializer(classOf[Message], MessageSerializer[Message]()).build()
        val data = mapper.valueToTree[JsonNode](msgList)
        HedyResults(data = Some(data))
      }
    }

    ret getOrElse Future(HedyResults.unprocessable())
  })

  def getConversationProperty(uid: Long, cid: String) = Action.async(request => {
    for {
      conv <- Chat.getConversation(new ObjectId(cid))
    } yield {
      if (conv nonEmpty) {
        val c = conv.get
        if (c.muteNotif == null) {
          c.muted = false
        } else {
          c.muted = c.muteNotif contains uid
        }
        val mapper = ObjectMapperFactory().addSerializer(classOf[Conversation], ConversationSerializer[Conversation]()).build()
        val node = mapper.valueToTree[JsonNode](conv)
        HedyResults(data = Some(node))
      } else {
        HedyResults.unprocessable()
      }
    }
  })

  def getConversationPropertyByUserIds(uid: Long, targetIds: String) = Action.async(request => {
    val targetIdsSeq = if (targetIds == null) {
      Seq()
    } else {
      targetIds.split(",").toSeq map (_.toLong)
    }
    val userIds = ArrayBuffer[Long]()
    val convList = targetIdsSeq map (targetId => {
      for {
        conv <- Chat.getConversation(uid, targetId)
      } yield {
        if (conv nonEmpty) {
          val c = conv.get
          if (c.muteNotif == null) {
            c.muted = false
          } else {
            c.muted = c.muteNotif contains uid
          }
          c.targetId = targetId
          Some(c)
        } else None
      }
    })
    for {
      result <- Future.sequence(convList) map (convSeq => {
        val mapper = ObjectMapperFactory().addSerializer(classOf[Conversation], ConversationSerializer[Conversation]()).build()
        mapper.valueToTree[JsonNode](convSeq.filter(_ nonEmpty) map (_.get))
      })
    } yield {
      HedyResults(data = Some(result))
    }
  })

  def pong() = Action.async(request => {
    val node = new ObjectMapper().createObjectNode()
    node.put("ack", "pong")
    Future {
      HedyResults(data = Some(node))
    }
  })
}
