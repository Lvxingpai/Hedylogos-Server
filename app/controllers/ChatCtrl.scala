package controllers

import core.Chat
import core.aspectj.WithAccessLog
import core.json.MessageFormatter
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

  //  def sendMessageBase(msgInfo: MessageInfo): Future[Result] = {
  //    val chatType = msgInfo.chatType
  //    val futureConversation = Chat.chatGroupConversation(msgInfo.receiverId.toString)
  //    val empty = futureConversation map (item => if (item nonEmpty) true else false)
  //
  //    val futureMsg: Future[Message] = for {
  //      opt <- futureConversation
  //      conversation <- {
  //        opt map (Future(_)) getOrElse {
  //          for {
  //            group <- FinagleCore.getChatGroup(msgInfo.receiverId)
  //            // 创建一个conversation
  //            con <- Chat.chatGroupConversation(group)
  //          } yield con
  //        }
  //      }
  //      message <- {
  //        chatType match {
  //          case SEND_TYPE_SINGLE => Chat.sendMessage(msgInfo.msgType, msgInfo.contents.getOrElse(""), msgInfo.receiverId, msgInfo.senderId, msgInfo.chatType)
  //          case SEND_TYPE_GROUP => Chat.sendMessage(msgInfo.msgType, msgInfo.contents.getOrElse(""), conversation.id, msgInfo.receiverId, msgInfo.senderId, msgInfo.chatType)
  //          case _ => throw new IllegalArgumentException("illegal conversation type")
  //        }
  //      }
  //    } yield message
  //
  //    futureMsg map (msg => {
  //      val result = JsObject(Seq(
  //        "conversation" -> JsString(msg.getConversation.toString),
  //        "msgId" -> JsNumber(msg.getMsgId),
  //        "timestamp" -> JsNumber(msg.getTimestamp)
  //      ))
  //      Helpers.JsonResponse(data = Some(result))
  //    })
  //  }

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
}
