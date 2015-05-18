package core.json

import models.{AbstractEntity, Message}
import play.api.libs.json.{JsNumber, JsObject, JsString, JsValue}

/**
 * Created by zephyre on 4/23/15.
 */
object UserInfoSimpleFormatter extends JsonFormatter {

  val USERINFOSIMPLEFIELDS = Seq("userId", "nickName", "avatar")

  override def format(item: AbstractEntity): JsValue = {
    val user = item.asInstanceOf[models.UserInfo]
    JsObject(Seq(
      "userId" -> JsNumber(user.getUserId.toLong),
      "nickName" -> JsString(user.getNickName),
      "avatar" -> JsString(user.getAvatar)
    ))
  }
}
