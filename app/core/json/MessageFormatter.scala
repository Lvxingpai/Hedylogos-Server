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
      "msgId" -> JsNumber(msg.getMsgId.toLong),
      "msgType" -> JsNumber(msg.getMsgType.toInt),
      "conversation" -> JsString(msg.getConversation.toString),
      "contents" -> JsString(msg.getContents),
      "senderId" -> JsNumber(msg.getSenderId.toLong),
      "senderAvatar" -> JsString(""),
      "senderName" -> JsString("测试用户"),
      "timestamp" -> JsNumber(msg.getTimestamp.toLong)))
  }
}
