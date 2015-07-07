package controllers

import core.User
import core.aspectj.WithAccessLog
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{ JsNumber, JsObject, JsString }
import play.api.mvc.{ Action, Controller }

import scala.concurrent.Future

/**
 * Created by zephyre on 4/20/15.
 */
object UserCtrl extends Controller {
  @WithAccessLog
  def login = Action.async {
    request =>
      {
        val jsonNode = request.body.asJson.get
        val userId = (jsonNode \ "userId").asOpt[Long].get
        val regId = (jsonNode \ "regId").asOpt[String].get
        User.login(userId, regId).map(v => Helpers.JsonResponse())
      }
  }

  @WithAccessLog
  def logout = Action.async {
    request =>
      {
        val jsonNode = request.body.asJson.get
        val userId = (jsonNode \ "userId").asOpt[Long].get
        User.logout(userId).map(v => Helpers.JsonResponse())
      }
  }

  @WithAccessLog
  def version() = Action.async(request => {
    val result = JsObject(Seq(
      "message" -> JsString("HELLO"),
      "timestamp" -> JsNumber(System.currentTimeMillis)
    ))

    Future(Helpers.JsonResponse(data = Some(result)))
  })
}