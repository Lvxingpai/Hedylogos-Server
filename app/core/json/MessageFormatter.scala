package core.json

import models.{AbstractEntity, Message}
import play.api.libs.json.{JsNumber, JsObject, JsString, JsValue}

/**
 * Created by zephyre on 4/23/15.
 */
object MessageFormatter extends JsonFormatter {
  override def format(item: AbstractEntity): JsValue = {
    val msg = item.asInstanceOf[Message]
    JsObject(Seq(
      "id" -> JsString(msg.getId.toString),
      "chatType" -> JsString(msg.getChatType),
      "msgId" -> JsNumber(msg.getMsgId.toLong),
      "msgType" -> JsNumber(msg.getMsgType.toInt),
      "conversation" -> JsString(msg.getConversation.toString),
      "contents" -> JsString(msg.getContents),
      "senderId" -> JsNumber(msg.getSenderId.toLong),
      "receiverId" -> JsNumber(msg.getReceiverId.toLong),
      "senderAvatar" -> JsString(""),
      "senderName" -> JsString("测试用户"),
      "timestamp" -> JsNumber(msg.getTimestamp.toLong)))
  }

  def formatAddRouteKey(item: AbstractEntity, routeKey: String): JsValue = {
    val msg = item.asInstanceOf[Message]
    JsObject(Seq(
      "routeKey" -> JsString(routeKey),
      "message" -> JsObject(
        Seq(
          "id" -> JsString(msg.getId.toString),
          "chatType" -> JsString(msg.getChatType),
          "msgId" -> JsNumber(msg.getMsgId.toLong),
          "msgType" -> JsNumber(msg.getMsgType.toInt),
          "conversation" -> JsString(msg.getConversation.toString),
          "contents" -> JsString(msg.getContents),
          "senderId" -> JsNumber(msg.getSenderId.toLong),
          "receiverId" -> JsNumber(msg.getReceiverId.toLong),
          "senderAvatar" -> JsString(""),
          "senderName" -> JsString("测试用户"),
          "timestamp" -> JsNumber(msg.getTimestamp.toLong)
        )
      )
    )
    )
  }
}
