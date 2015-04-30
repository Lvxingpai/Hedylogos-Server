package controllers


import core.Group
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc.{Action, Controller}

/**
 * Created by topy on 2015/4/25.
 */
object GroupCtrl extends Controller {
  def createGroup() = Action.async {
    request => {
      val uid = request.headers.get("UserId").get.toLong
      val jsonNode = request.body.asJson.get
      val name = (jsonNode \ "name").asOpt[String].get
      val groupType = (jsonNode \ "groupType").asOpt[String].get
      val isPublic = (jsonNode \ "isPublic").asOpt[Boolean].get

      for {
        group <- Group.createGroup(uid, name, groupType, isPublic)
      } yield {
        val result = JsObject(Seq(
          "groupId" -> JsNumber(group.getGroupId.toLong),
          "name" -> JsString(group.getName),
          "creator" -> JsNumber(group.getCreator.toLong),
          "groupType" -> JsString(group.getType),
          "isPublic" -> JsBoolean(group.getVisible)
        ))
        Helpers.JsonResponse(data = Some(result))
      }
    }
  }

  def modifyGroup(gid: Long) = Action.async {
    request => {
      val uid = request.headers.get("UserId").get.toLong
      val jsonNode = request.body.asJson.get
      val groupId = (jsonNode \ "groupId").asOpt[Long].get
      val name = (jsonNode \ "name").asOpt[String]
      val desc = (jsonNode \ "desc").asOpt[String]
      val avatar = (jsonNode \ "avatar").asOpt[String]
      val maxUsers = (jsonNode \ "maxUsers").asOpt[Int]
      val isPublic = (jsonNode \ "isPublic").asOpt[Boolean]
      Group.modifyGroup(groupId, name, desc, avatar, maxUsers, isPublic).map(v => Helpers.JsonResponse())
    }
  }

}
