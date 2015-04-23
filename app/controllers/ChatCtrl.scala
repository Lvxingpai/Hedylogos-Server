package controllers

import core.Chat
import core.json.MessageFormatter
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

  def acknowledge(user: Long) = Action {
    request => {
      val jsonNode = request.body.asJson.get
      val msgList = (jsonNode \ "msgList").asInstanceOf[JsArray].value.map(_.asOpt[String].get)
      Chat.acknowledge(user, msgList)

      Helpers.JsonResponse(data = _fetchMessages(user))
    }
  }

  def fetchMessages(user: Long) = Action {
    Helpers.JsonResponse(data = _fetchMessages(user))
  }

  def _fetchMessages(user: Long): JsValue = {
    JsArray(Chat.fetchMessage(user).map(MessageFormatter.format(_)))
  }
}
