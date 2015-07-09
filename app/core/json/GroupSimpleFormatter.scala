package core.json

import com.lvxingpai.yunkai.ChatGroup
import models.{ AbstractEntiry, Message }
import play.api.libs.json.{ JsNumber, JsObject, JsString, JsValue }

/**
 * Created by zephyre on 4/23/15.
 */
object GroupSimpleFormatter extends JsonFormatter {

  val GROUPSIMPLEFIELDS = Seq("id", "groupId", "name", "avatar")

  override def format(item: AbstractEntiry): JsValue = {
    val group = item.asInstanceOf[ChatGroup]
    JsObject(Seq(
      "id" -> JsString(group.id.toString),
      "conversation" -> JsString(group.id.toString),
      "groupId" -> JsNumber(group.chatGroupId.toLong),
      "name" -> JsString(group.name),
      "avatar" -> JsString(group.avatar.getOrElse(""))

    ))
  }
}
