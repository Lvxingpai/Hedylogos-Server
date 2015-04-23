package controllers

import core.Chat
import models.Message
import org.bson.types.ObjectId
import play.api.libs.json._
import play.api.mvc.{Action, Controller}

/**
 * Created by zephyre on 4/23/15.
 */
object ChatCtrl extends Controller {
  def sendMessage() = Action {
    request => {
      val jsonNode = request.body.asJson.get
      val senderId = (jsonNode \ "sender").asOpt[Long].get
      val recvId = (jsonNode \ "receiver").asOpt[Long]
      val cid = (jsonNode \ "conversation").asOpt[String]
      val msgType = (jsonNode \ "msgType").asOpt[Int].get
      val contents = (jsonNode \ "contents").asOpt[String].get

      val msg = if (cid.nonEmpty)
        Chat.sendMessage(msgType, contents, new ObjectId(cid.get), senderId)
      else
        Chat.sendMessage(msgType, contents, recvId.get, senderId)

      val result = Seq(
        "conversation" -> JsString(msg.getConversation.toString),
        "msgId" -> JsNumber(msg.getMsgId.toLong),
        "timestamp" -> JsNumber(msg.getTimestamp.toLong)
      )
      Helpers.JsonResponse(data = JsObject(result))
    }
  }

  def fetchMessages(user: Long) = Action {
    def msg2json(msg: Message): JsValue = {
      JsObject(Seq(
        "msgId" -> JsNumber(msg.getMsgId.toLong),
        "msgType" -> JsNumber(msg.getMsgType.toInt),
        "conversation" -> JsString(msg.getConversation.toString),
        "contents" -> JsString(msg.getContents),
        "senderId" -> JsNumber(msg.getSenderId.toLong),
        "senderAvatar" -> JsString(""),
        "senderName" -> JsString("测试用户"),
        "timestamp" -> JsNumber(msg.getTimestamp.toLong)))
    }

    val msgList = JsArray(Chat.fetchMessage(user).map(msg2json))

    Helpers.JsonResponse(data = msgList)
  }
}
