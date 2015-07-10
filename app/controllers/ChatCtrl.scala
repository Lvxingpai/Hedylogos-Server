package controllers

import core.Chat
import core.aspectj.WithAccessLog
import core.json.MessageFormatter
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc.{ Action, Controller, Result, Results }
import core.Implicits._
import org.bson.types.ObjectId

import scala.concurrent.Future
import scala.language.postfixOps

/**
 * Created by zephyre on 4/23/15.
 */
object ChatCtrl extends Controller {

  case class MessageInfo(senderId: Long, chatType: String, receiverId: Long, msgType: Int, contents: Option[String])

  def sendMessageBase(msgType: Int, contents: String, receiver: Long, sender: Long, chatType: String): Future[Result] = {
    Chat.sendMessage(msgType, contents, receiver, sender, chatType) map (msg => {
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
        val jsonNode = request.body.asJson.get
        val action = (jsonNode \ "action").asOpt[String].get
        Chat.opConversationProperty(action, uid, new ObjectId(cid)) map (_ => Helpers.JsonResponse())
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
        } yield sendMessageBase(msgType, contents, receiverId, senderId, chatType)

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
}
