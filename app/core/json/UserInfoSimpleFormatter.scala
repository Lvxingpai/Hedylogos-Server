package core.json

import com.lvxingpai.yunkai.UserInfo
import models.{ AbstractEntiry, Message }
import play.api.libs.json.{ JsNumber, JsObject, JsString, JsValue }

/**
 * Created by zephyre on 4/23/15.
 */
object UserInfoSimpleFormatter extends JsonFormatter {

  val USERINFOSIMPLEFIELDS = Seq("userId", "nickName", "avatar")

  override def format(item: AbstractEntiry): JsValue = {
    val user = item.asInstanceOf[UserInfo]
    JsObject(Seq(
      "userId" -> JsNumber(user.userId),
      "nickName" -> JsString(user.nickName),
      "avatar" -> JsString(user.avatar.getOrElse(""))
    ))
  }
}
