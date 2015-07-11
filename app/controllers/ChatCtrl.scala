package controllers

import core.Chat
import core.Implicits._
import core.aspectj.WithAccessLog
import core.json.MessageFormatter
import org.bson.types.ObjectId
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc.{ Action, Controller, Result, Results }

import scala.collection.Map
import scala.concurrent.Future
import scala.language.postfixOps

/**
 * Created by zephyre on 4/23/15.
 */
object ChatCtrl extends Controller {

  case class MessageInfo(senderId: Long, chatType: String, receiverId: Long, msgType: Int, contents: Option[String])

  def sendMessageBase(msgType: Int, contents: String, receiver: Long, sender: Long, chatType: String, includes: Seq[Long] = Seq(), excludes: Seq[Long] = Seq()): Future[Result] = {
    Chat.sendMessage(msgType, contents, receiver, sender, chatType, includes, excludes) map (msg => {
      val result = JsObject(Seq(
        "conversation" -> JsString(msg.getConversation.toString),
        "msgId" -> JsNumber(msg.getMsgId),
        "timestamp" -> JsNumber(msg.getTimestamp)
      ))
      Helpers.JsonResponse(data = Some(result))
    })
  }

  @WithAccessLog
  def updateConversationProperty(uid: Long, cid: String) = Action.async {
    request =>
      {
        val result = for {
          body <- request.body.asJson
        } yield {
          val muteOpt = (body \ "mute").asOpt[Boolean]
          val settings = Map("mute" -> muteOpt).filter(_._2 nonEmpty).mapValues(_.get)
          Chat.opConversationProperty(uid, new ObjectId(cid), settings) map (_ => Helpers.JsonResponse())
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

        ret getOrElse Future(Results.UnprocessableEntity)
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
        val nodes = JsArray(msgList map MessageFormatter.format)
        Helpers.JsonResponse(data = Some(nodes))
      }
    }

    ret getOrElse Future(Helpers.JsonResponse(1))
  })

  def getConversationProperty(uid: Long, cid: String) = Action.async(request => {
    for {
      conv <- Chat.getConversation(new ObjectId(cid))
    } yield {
      val isMuted = Option(conv.muteNotif) exists (_ contains uid)
      val node = JsObject(Seq("muted" -> JsBoolean(isMuted)))
      Helpers.JsonResponse(data = Some(node))
    }
  })
}
