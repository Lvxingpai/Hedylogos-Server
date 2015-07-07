package controllers

import core.Chat
import core.aspectj.WithAccessLog
import core.finagle.FinagleCore
import core.json.MessageFormatter
import models._
import org.bson.types.ObjectId
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc.{ Action, Controller, Result, Results }

import scala.concurrent.Future
import scala.language.postfixOps

/**
 * Created by zephyre on 4/23/15.
 */
object ChatCtrl extends Controller {

  case class MessageInfo(senderId: Long, chatType: String, receiverId: Option[Long], cid: Option[ObjectId], msgType: Int,
    contents: Option[String])

  val SEND_TYPE_SINGLE = "single"
  val SEND_TYPE_GROUP = "group"

  def sendMessageBase(msgInfo: MessageInfo): Future[Result] = {
    val cid = msgInfo.cid
    val chatType = msgInfo.chatType

    val futureMsg: Future[Message] =
      if (cid.nonEmpty)
        Chat.sendMessage(msgInfo.msgType, msgInfo.contents.getOrElse(""), cid.get, msgInfo.receiverId.get, msgInfo.senderId, msgInfo.chatType)
      else if (chatType.equals(SEND_TYPE_SINGLE))
        Chat.sendMessage(msgInfo.msgType, msgInfo.contents.getOrElse(""), msgInfo.receiverId.get, msgInfo.senderId, msgInfo.chatType)
      else if (chatType.equals(SEND_TYPE_GROUP))
        for {
          group <- FinagleCore.getGroupObjId(msgInfo.receiverId.get)
          chat <- Chat.sendMessage(msgInfo.msgType, msgInfo.contents.getOrElse(""), group.getId, msgInfo.receiverId.get, msgInfo.senderId, msgInfo.chatType)
        } yield chat
      else null

    futureMsg.map(msg => {
      val result = JsObject(Seq(
        "conversation" -> JsString(msg.getConversation.toString),
        "msgId" -> JsNumber(msg.getMsgId),
        "timestamp" -> JsNumber(msg.getTimestamp)
      ))
      Helpers.JsonResponse(data = Some(result))
    })
  }

  @WithAccessLog
  def sendMessage() = Action.async {
    request =>
      {
        val ret = for {
          jsonNode <- request.body.asJson
          senderId <- (jsonNode \ "sender").asOpt[Long]
          chatType <- (jsonNode \ "chatType").asOpt[String]
          msgType <- (jsonNode \ "msgType").asOpt[Int]
          contents <- (jsonNode \ "contents").asOpt[String]
          target <- {
            // receiverId和conversation，二者至少居其一
            val receiverId = (jsonNode \ "receiver").asOpt[Long]
            val cid = (jsonNode \ "conversation").asOpt[String] map (new ObjectId(_))
            if (cid nonEmpty)
              cid
            else
              receiverId
          }
        } yield {
          target match {
            case receiverId: Long =>
              sendMessageBase(MessageInfo(senderId, chatType, Some(receiverId), None, msgType, Some(contents)))
            case cid: ObjectId =>
              sendMessageBase(MessageInfo(senderId, chatType, None, Some(cid), msgType, Some(contents)))
            case _ =>
              Future(Results.UnprocessableEntity)
          }
        }
        ret getOrElse Future(Results.UnprocessableEntity)
      }
  }

  @WithAccessLog
  def acknowledgeAndFetchMessages(user: Long) = Action.async {
    request =>
      {
        val jsonNode = request.body.asJson.get
        val ackMessages = (jsonNode \ "msgList").asInstanceOf[JsArray].value.map(_.asOpt[String].get)

        Chat.acknowledge(user, ackMessages).flatMap(_ =>
          _fetchMessages(user).map(jsvalue => Helpers.JsonResponse(data = Some(jsvalue))))
      }
  }

  def _fetchMessages(user: Long): Future[JsValue] = {
    Chat.fetchMessage(user).map(msgSeq => {
      JsArray(msgSeq.map(MessageFormatter.format(_)))
    })
  }

  //
  //  def fetchMessages(user: Long) = Action.async {
  //    Chat.fetchMessage(user).map(msgSeq => {
  //      Helpers.JsonResponse(data = Some(JsArray(msgSeq.map(MessageFormatter.format))))
  //    })
  //  }
}
