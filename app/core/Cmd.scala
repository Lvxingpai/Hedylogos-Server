package core

import controllers.GroupCtrl
import core.connector.MorphiaFactory
import core.mio.{GetuiService, MongoStorage, RedisMessaging}
import models.{Conversation, Message, UserInfo}
import org.bson.types.ObjectId
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsNumber, JsObject, JsString, JsValue}

import scala.concurrent.Future


/**
 * CMD相关的操作
 *
 * Created by zephyre on 4/20/15.
 */
object Cmd {

  val ds = MorphiaFactory.datastore
  val CMD_CHAT_TYPE = "CMD"
  val GROUP_CMD_CONVERSATION_FINGER = "CMD"
  val GROUP_CMD_MESSAGE_TYPE = 100

  def sendGroupCmdMessage(action: String, group: models.Group, sender: UserInfo, receiver: Seq[UserInfo]): Future[Seq[Message]] = {
    Logger.trace("int")
    for {
      c <- cmdConversation(group, receiver)
      msg <- sendGroupCmdMessage(action, GROUP_CMD_MESSAGE_TYPE, c.map(v => v.getId), group, sender, receiver)
    } yield msg
  }

  def sendGroupCmdMessage(action: String, msgType: Int, cidList: Seq[ObjectId], group: models.Group, sender: UserInfo, receiver: Seq[UserInfo]): Future[Seq[Message]] = {
    Logger.trace("sendGroupCmdMessage")
    val cmdContent = CmdInfo.createCmd(action, msgType, group, sender)
    val futureMsg = Chat.buildMessage(msgType, cmdContent.toString(), cidList, 0, sender.getUserId, CMD_CHAT_TYPE)
    val targets = receiver.map(_.getUserId)

    //    val futureConv = Chat.conversation(cidList)
    val mongoResult = for {
      msg <- futureMsg
      result <- MongoStorage.sendMessageList(msg, targets map scala.Long.unbox)
    } yield result

    for {
      msg <- mongoResult
      redisResult <- RedisMessaging.sendMessageList(msg, targets map scala.Long.unbox)
      getuiResult <- GetuiService.sendMessage(redisResult(0), targets map scala.Long.unbox)
    } yield getuiResult
    mongoResult

  }

  def cmdConversation(group: models.Group, receiver: Seq[UserInfo]): Future[Seq[Conversation]] = {
    Future {
      def receiver2ConList(receiver: Seq[UserInfo]): Seq[Conversation] = {
        for {
          user <- receiver
        } yield {
          val c: Conversation = new Conversation
          c.setId(new ObjectId())
          c.setFingerprint(GROUP_CMD_CONVERSATION_FINGER + "." + user.getUserId)
          c.setParticipants(java.util.Arrays.asList(user.getUserId))
          c.setMsgCounter(0L)
          c.setCreateTime(System.currentTimeMillis())
          c.setUpdateTime(System.currentTimeMillis())
          c
        }
      }
      Logger.trace("cmdConversation")
      val cList = receiver2ConList(receiver)
      val result = java.util.Arrays.asList(cList: _*)
      ds.save[Conversation](cList(0))
      cList

    }
  }

}

object CmdInfo {

  val DISCUSSION_ACTION_INVITE_ADD = "D_INVITE"
  val DISCUSSION_ACTION_APPLY_ADD = "D_APPLY"
  val DISCUSSION_ACTION_AGREE_ADD = "D_AGREE"
  val DISCUSSION_ACTION_QUIT = "D_QUIT"
  val DISCUSSION_ACTION_REMOVE = "D_REMOVE"
  val DISCUSSION_ACTION_DESTROY = "D_DESTROY"
  val GROUP_ACTION_APPLY_ADD = "G_APPLY"
  val GROUP_ACTION_AGREE_ADD = "G_AGREE"
  val GROUP_ACTION_INVITE_ADD = "G_INVITE"
  val GROUP_ACTION_QUIT = "G_QUIT"
  val GROUP_ACTION_REMOVE = "G_REMOVE"
  val GROUP_ACTION_DESTROY = "G_DESTROY"

  def createCmd(action: String, mType: Int, group: models.Group, user: UserInfo): JsValue = {
    val msgAction = action match {
      case GroupCtrl.ACTION_ADDMEMBERS => DISCUSSION_ACTION_INVITE_ADD
      case GroupCtrl.ACTION_DELMEMBERS => DISCUSSION_ACTION_REMOVE
    }
    JsObject(Seq(
      // "messageType" -> JsNumber(mType),
      //  "contents" -> JsObject(Seq(
      "action" -> JsString(msgAction),
      "userId" -> JsNumber(user.getUserId.toLong),
      "nickName" -> JsString(user.getNickName),
      "avatar" -> JsString(user.getAvatar),
      "groupId" -> JsNumber(group.getGroupId.toLong),
      "groupName" -> JsString(group.getName),
      "groupAvatar" -> JsString(group.getAvatar)
    )
      //)
      //)
    )
  }
}