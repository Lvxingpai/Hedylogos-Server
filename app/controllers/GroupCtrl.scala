package controllers


import core.Group
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsBoolean, JsNumber, JsObject, JsString}
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

  //  def modifyGroup() = Action.async {
  //    request => {
  //      val uid = request.headers.get("UserId").get.toLong
  //      val jsonNode = request.body.asJson.get
  //      val name = (jsonNode \ "name").asOpt[String].get
  //      val desc = (jsonNode \ "desc").asOpt[String].get
  //      // 添加成员-addMember，删除成员-delMember
  //      val action = (jsonNode \ "action").asOpt[String]
  //      val participants = (jsonNode \ "participants").asOpt[Array[Int]].get
  //      val admin = (jsonNode \ "participants").asOpt[Array[Int]].get
  //      val maxUsers = (jsonNode \ "maxUsers").asOpt[Int].get
  //      // 加入公开群是否需要批准, 默认值是true,此属性为可选的
  //      val approval = (jsonNode \ "approval").asOpt[Boolean].get
  //      // 群组成员最大数(包括群主), 值为数值类型,默认值200,此属性为可选的
  //      val public = (jsonNode \ "public").asOpt[Boolean].get
  //
  //      //val group = Group.createGroup(uid, name)
  //
  //      //val jsonNode = request.body.asJson.get
  //      val senderId = (jsonNode \ "sender").asOpt[Long].get
  //      val recvId = (jsonNode \ "receiver").asOpt[Long]
  //      val cid = (jsonNode \ "conversation").asOpt[String]
  //      val msgType = (jsonNode \ "msgType").asOpt[Int].get
  //      val contents = (jsonNode \ "contents").asOpt[String].get
  //
  //      val futureMsg = if (cid.nonEmpty)
  //        Chat.sendMessage(msgType, contents, new ObjectId(cid.get), senderId)
  //      else
  //        Chat.sendMessage(msgType, contents, recvId.get, senderId)
  //
  //      futureMsg.map(msg => {
  //        val result = JsObject(Seq(
  //          "conversation" -> JsString(msg.getConversation.toString),
  //          "msgId" -> JsNumber(msg.getMsgId.toLong),
  //          "timestamp" -> JsNumber(msg.getTimestamp.toLong)
  //        ))
  //        Helpers.JsonResponse(data = Some(result))
  //      })
  //    }
  //  }

}
