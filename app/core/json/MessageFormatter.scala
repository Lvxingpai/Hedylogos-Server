package core.json

import models.Message.ChatType
import models.{ AbstractEntiry, Message }
import play.api.libs.json.{ JsNumber, JsObject, JsString, JsValue }

/**
 * Created by zephyre on 4/23/15.
 */
object MessageFormatter extends JsonFormatter {
  override def format(item: AbstractEntiry): JsValue = {
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
    val content = if (msg.getChatType != null && msg.getChatType.equals("group"))
      stContent ++ Seq("groupId" -> JsNumber(msg.getReceiverId.toLong))
    else stContent
    JsObject(
      content
    )
  }

  def formatAddRouteKey(item: AbstractEntiry, routingKey: String): JsValue = {
    val msg = item.asInstanceOf[Message]
    val msgStContent = Seq(
      "id" -> JsString(msg.id.toString),
      "chatType" -> JsString(msg.chatType),
      "msgId" -> JsNumber(msg.msgId),
      "msgType" -> JsNumber(msg.msgType),
      "conversation" -> JsString(msg.conversation.toString),
      "contents" -> JsString(msg.contents),
      "abbrev" -> JsString(Option(msg.abbrev) getOrElse ""),
      "senderId" -> JsNumber(msg.senderId),
      "timestamp" -> JsNumber(msg.timestamp),
      (if (msg.chatType == ChatType.CHATGROUP.toString) "groupId" else "receiverId") -> JsNumber(msg.receiverId)
    )
    val content = Seq(
      "routeKey" -> JsString(routingKey),
      "message" -> JsObject(msgStContent)
    )
    JsObject(content)
  }
}
