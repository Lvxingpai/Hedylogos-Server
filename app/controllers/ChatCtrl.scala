package controllers

import core.Chat
import core.aspectj.WithAccessLog
import core.finagle.FinagleCore
import core.json.MessageFormatter
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc.{ Action, Controller, Result, Results }

import scala.concurrent.Future
import scala.language.postfixOps

/**
 * Created by zephyre on 4/23/15.
 */
object ChatCtrl extends Controller {

  case class MessageInfo(senderId: Long, chatType: String, receiverId: Long, msgType: Int, contents: Option[String])

  val SEND_TYPE_SINGLE = "single"
  val SEND_TYPE_GROUP = "group"

  def sendMessageBase(msgInfo: MessageInfo): Future[Result] = {
    val chatType = msgInfo.chatType

    val futureMsg: Future[Message] =
      //      if (cid.nonEmpty)
      //        Chat.sendMessage(msgInfo.msgType, msgInfo.contents.getOrElse(""), cid.get, msgInfo.receiverId.get, msgInfo.senderId, msgInfo.chatType)
      if (chatType.equals(SEND_TYPE_SINGLE))
        Chat.sendMessage(msgInfo.msgType, msgInfo.contents.getOrElse(""), msgInfo.receiverId, msgInfo.senderId, msgInfo.chatType)
      else if (chatType.equals(SEND_TYPE_GROUP))
        for {
          group <- FinagleCore.getGroupObjId(msgInfo.receiverId)
          chat <- Chat.sendMessage(msgInfo.msgType, msgInfo.contents.getOrElse(""), group.getId, msgInfo.receiverId, msgInfo.senderId, msgInfo.chatType)
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
          receiverId <- (jsonNode \ "receiver").asOpt[Long]
          chatType <- (jsonNode \ "chatType").asOpt[String]
          msgType <- (jsonNode \ "msgType").asOpt[Int]
          contents <- (jsonNode \ "contents").asOpt[String]
        } yield sendMessageBase(MessageInfo(senderId, chatType, receiverId, msgType, Some(contents)))
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
