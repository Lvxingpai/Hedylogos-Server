package core.json

import models.{AbstractEntity, Message}
import play.api.libs.json.{JsNumber, JsObject, JsString, JsValue}

/**
 * Created by zephyre on 4/23/15.
 */
object
MessageFormatter extends JsonFormatter {
  override def format(item: AbstractEntity): JsValue = {
    val msg = item.asInstanceOf[Message]
    val stContent = Seq(
      "id" -> JsString(msg.getId.toString),
      "chatType" -> JsString(msg.getChatType),
      "msgId" -> JsNumber(msg.getMsgId.toLong),
      "msgType" -> JsNumber(msg.getMsgType.toInt),
      "conversation" -> JsString(msg.getConversation.toString),
      "contents" -> JsString(msg.getContents),
      "senderId" -> JsNumber(msg.getSenderId.toLong),
      //      "senderAvatar" -> JsString(""),
      //      "senderName" -> JsString("测试用户"),
      "timestamp" -> JsNumber(msg.getTimestamp.toLong))
    val content = if (msg.getChatType.nonEmpty && msg.getChatType.equals("group"))
      stContent ++ Seq("groupId" -> JsNumber(msg.getReceiverId.toLong))
    else stContent
    JsObject(
      content
    )
  }

  def formatAddRouteKey(item: AbstractEntity, routeKey: String): JsValue = {
    val msg = item.asInstanceOf[Message]
    val msgStContent = Seq(
      "id" -> JsString(msg.getId.toString),
      "chatType" -> JsString(msg.getChatType),
      "msgId" -> JsNumber(msg.getMsgId.toLong),
      "msgType" -> JsNumber(msg.getMsgType.toInt),
      "conversation" -> JsString(msg.getConversation.toString),
      "contents" -> JsString(msg.getContents),
      "senderId" -> JsNumber(msg.getSenderId.toLong),
      //          "senderAvatar" -> JsString(""),
      //          "senderName" -> JsString("测试用户"),
      "timestamp" -> JsNumber(msg.getTimestamp.toLong)
    )
    val msgContent = if (msg.getChatType.nonEmpty && msg.getChatType.equals("group"))
      msgStContent ++ Seq("groupId" -> JsNumber(msg.getReceiverId.toLong))
    else msgStContent
    val content = Seq(
      "routeKey" -> JsString(routeKey),
      "message" -> JsObject(msgContent)
    )
    JsObject(content)
  }
}
