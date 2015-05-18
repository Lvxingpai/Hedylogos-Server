package core.json

import models.{AbstractEntity, Message}
import play.api.libs.json.{JsNumber, JsObject, JsString, JsValue}

/**
 * Created by zephyre on 4/23/15.
 */
object GroupSimpleFormatter extends JsonFormatter {

  val GROUPSIMPLEFIELDS = Seq("id", "groupId", "name", "avatar")

  override def format(item: AbstractEntity): JsValue = {
    val group = item.asInstanceOf[models.Group]
    JsObject(Seq(
      "id" -> JsString(group.getId.toString),
      "conversation" -> JsString(group.getId.toString),
      "groupId" -> JsNumber(group.getGroupId.toLong),
      "name" -> JsString(group.getName),
      "avatar" -> JsString(group.getAvatar)

    ))
  }
}
