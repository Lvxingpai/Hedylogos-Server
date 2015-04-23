package controllers

import core.User
import play.api.mvc.{Action, Controller}

/**
 * Created by zephyre on 4/20/15.
 */
object UserCtrl extends Controller {
  def login = Action {
    request => {
      val jsonNode = request.body.asJson.get
      val userId = (jsonNode \ "userId").asOpt[Long].get
      val regId = (jsonNode \ "regId").asOpt[String].get

      User.login(userId, regId)
      Helpers.JsonResponse()
    }
  }

  def logout = Action {
    request => {
      val jsonNode = request.body.asJson.get
      val userId = (jsonNode \ "userId").asOpt[Long].get
      User.logout(userId)
      Helpers.JsonResponse()
    }
  }
}