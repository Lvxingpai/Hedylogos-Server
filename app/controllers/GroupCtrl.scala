package controllers


import controllers.ChatCtrl.MessageInfo
import core.{Chat, Group}
import core.json.{GroupSimpleFormatter, UserInfoSimpleFormatter, MessageFormatter}
import models.UserInfo
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc.{Action, Controller}
import scala.collection.JavaConversions._

/**
 * Created by topy on 2015/4/25.
 */
object GroupCtrl extends Controller {


  val ACTION_ADDMEMBERS = "addMembers"
  val ACTION_DELMEMBERS = "delMembers"

  /**
   * 创建群组
   *
   * @return
   */
  def createGroup() = Action.async {
    request => {
      val uid = request.headers.get("UserId").get.toLong
      val jsonNode = request.body.asJson.get
      //
      val name = (jsonNode \ "name").asOpt[String].getOrElse("")
      val avatar = (jsonNode \ "avatar").asOpt[String].getOrElse("")
      val groupType = (jsonNode \ "groupType").asOpt[String].getOrElse(models.Group.FD_TYPE_COMMON)
      val isPublic = (jsonNode \ "isPublic").asOpt[Boolean].getOrElse(true)
      val participants = (jsonNode \ "participants").asOpt[Array[Long]]
      val participantsValue = if (participants.nonEmpty) participants.get else null
      if (participantsValue != null) {
        for {
          group <- Group.createGroup(uid, name, avatar, groupType, isPublic, participantsValue)
          // TODO
          sendUser <- Group.getUserInfo(Seq(uid), Seq(UserInfo.fnUserId, UserInfo.fnNickName, UserInfo.fnAvatar))
          receiverUser <- Group.getUserInfo(participantsValue, Seq(UserInfo.fnUserId, UserInfo.fnNickName, UserInfo.fnAvatar))
          msg <- Chat.sendMessage(100, "", receiverUser(0).getUserId, sendUser(0).getUserId, "CMD")
        } yield msg
      }
      for {
        group <- Group.createGroup(uid, name, avatar, groupType, isPublic, participantsValue)
        conversation <- Chat.groupConversation(group)
      } yield {
        val result = JsObject(Seq(
          "groupId" -> JsNumber(group.getGroupId.toLong),
          "name" -> JsString(group.getName),
          "creator" -> JsNumber(group.getCreator.toLong),
          "groupType" -> JsString(group.getType),
          "isPublic" -> JsBoolean(group.getVisible),
          "conversation" -> JsString(conversation.getId.toString)
        ))
        Helpers.JsonResponse(data = Some(result))
      }
    }
  }

  /**
   * 修改群组
   *
   * @param gid
   * @return
   */
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

  /**
   * 取得群组信息
   *
   * @param gid
   * @return
   */
  def getGroup(gid: Long) = Action.async {
    request => {
      val uid = request.headers.get("UserId").get.toLong
      for {
        group <- Group.getGroup(gid)
        creator <- Group.getUserInfo(Seq(group.getCreator), UserInfoSimpleFormatter.USERINFOSIMPLEFIELDS) map (_(0))
        admin <- Group.getUserInfo(group.getAdmin map scala.Long.unbox, UserInfoSimpleFormatter.USERINFOSIMPLEFIELDS)
      } yield {
        val result = JsObject(Seq(
          "id" -> JsString(group.getId.toString),
          "conversation" -> JsString(group.getId.toString),
          "groupId" -> JsNumber(group.getGroupId.toLong),
          "name" -> JsString(group.getName),
          "creator" -> JsNumber(group.getCreator.toLong),
          "groupType" -> JsString(group.getType),
          "isPublic" -> JsBoolean(group.getVisible),
          "creator" -> JsObject(Seq(
            "userId" -> JsNumber(creator.getUserId.toLong),
            "nickName" -> JsString(creator.getNickName),
            "avatar" -> JsString(creator.getAvatar)
          )
          ),
          "admin" -> JsArray(admin.map(UserInfoSimpleFormatter.format)),
          "desc" -> JsString(group.getDesc),
          "maxUser" -> JsNumber(group.getMaxUsers.toInt),
          "createTime" -> JsNumber(group.getCreateTime.toLong),
          "updateTime" -> JsNumber(group.getUpdateTime.toLong),
          "visible" -> JsBoolean(group.getVisible),
          "participantCnt" -> JsNumber(group.getParticipantCnt.toInt)
        )
        )
        Helpers.JsonResponse(data = Some(result))
      }
    }
  }

  /**
   * 取得用户的群组信息
   *
   * @return
   */
  def getUserGroups(uid: Long) = Action.async {
    request => {
      //val uid = request.headers.get("UserId").get.toLong
      val fields = GroupSimpleFormatter.GROUPSIMPLEFIELDS
      for {
        group <- Group.getUserGroups(uid, fields)
      } yield {
        val result = JsArray(group.map(GroupSimpleFormatter.format))
        Helpers.JsonResponse(data = Some(result))
      }
    }
  }

  /**
   * 取得群组中的成员信息
   *
   * @param gid
   * @return
   */
  def getGroupUsers(gid: Long) = Action.async {
    request => {
      val uid = request.headers.get("UserId").get.toLong
      for {
        group <- Group.getGroup(gid, Seq(models.Group.FD_PARTICIPANTS))
        participant <- Group.getUserInfo(group.getParticipants map scala.Long.unbox, UserInfoSimpleFormatter.USERINFOSIMPLEFIELDS)
      } yield {
        val result = JsArray(participant.map(UserInfoSimpleFormatter.format))
        Helpers.JsonResponse(data = Some(result))
      }
    }
  }

  /**
   * 操作群组
   *
   * @param gid
   * @return
   */
  def opGroup(gid: Long) = Action.async {
    request => {
      val uid = request.headers.get("UserId").get.toLong
      val jsonNode = request.body.asJson.get
      val action = (jsonNode \ "action").asOpt[String].get
      val participants = (jsonNode \ "participants").asOpt[Array[Long]].get
      Group.opGroup(gid, action, participants, uid).map(v => Helpers.JsonResponse())
    }
  }

}
