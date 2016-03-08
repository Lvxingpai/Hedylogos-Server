package controllers

import javax.inject.{ Inject, Named }

import com.lvxingpai.inject.morphia.MorphiaMap
import core.User
import core.aspectj.WithAccessLog
import play.api.Configuration
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{ Action, Controller }

import scala.concurrent.Future

/**
 * Created by zephyre on 4/20/15.
 */
class UserCtrl @Inject() (@Named("default") configuration: Configuration, datastore: MorphiaMap) extends Controller {
  @WithAccessLog
  def login = Action.async {
    request =>
      {
        val jsonNode = request.body.asJson.get
        val userId = (jsonNode \ "userId").asOpt[Long].get
        val clientId = (jsonNode \ "regId").asOpt[String].get
        User.login(userId, clientId).map(v => HedyResults())
      }
  }

  @WithAccessLog
  def logout = Action.async {
    request =>
      {
        val jsonNode = request.body.asJson.get
        val userId = (jsonNode \ "userId").asOpt[Long].get
        User.logout(userId).map(v => HedyResults())
      }
  }

  @WithAccessLog
  def version() = Action.async {
    Future(HedyResults())
  }
}