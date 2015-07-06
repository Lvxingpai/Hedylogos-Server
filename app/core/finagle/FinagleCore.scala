package core.finagle

import com.lvxingpai.yunkai.{ChatGroup => YunkaiChatGroup, ChatGroupProp, NotFoundException, UserInfo => YunkaiUserInfo}
import com.twitter.util.{Future => TwitterFuture}
import misc.FinagleFactory
import models.ChatGroup
import scala.concurrent.Future
import core.finagle.TwitterConverter._


/**
 * Created by topy on 2015/7/6.
 */
object FinagleCore {

  implicit def groupInfoYunkai2Model(groupInfo: YunkaiChatGroup): ChatGroup = FinagleConvert.convertK2ChatGroup(groupInfo)

  val basicChatGroupFields = Seq(ChatGroupProp.ChatGroupId, ChatGroupProp.Name, ChatGroupProp.Visible, ChatGroupProp.Avatar, ChatGroupProp.GroupDesc, ChatGroupProp.Id)
  val basicObjIdFields = Seq(ChatGroupProp.Id)

  def getGroupObjId(gid: Long): Future[ChatGroup] = {
    FinagleFactory.client.getChatGroup(gid, Some(basicObjIdFields)) map (chatGroup => {
      groupInfoYunkai2Model(chatGroup)
    }) rescue {
      case _: NotFoundException =>
        TwitterFuture {
          new ChatGroup()
        }
    }
  }

}
