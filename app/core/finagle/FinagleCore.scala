package core.finagle

import com.lvxingpai.yunkai.{ ChatGroup, ChatGroupProp, NotFoundException }
import com.twitter.util.{ Future => TwitterFuture }
//import core.finagle.FinagleConvert
import misc.FinagleFactory
import scala.concurrent.Future
import core.finagle.TwitterConverter._

/**
 * Created by topy on 2015/7/6.
 */
object FinagleCore {

  val basicChatGroupFields = Seq(ChatGroupProp.ChatGroupId, ChatGroupProp.Name, ChatGroupProp.Visible, ChatGroupProp.Avatar, ChatGroupProp.GroupDesc, ChatGroupProp.Id)
  val responseFields = Seq(ChatGroupProp.Id, ChatGroupProp.ChatGroupId, ChatGroupProp.Name, ChatGroupProp.Participants)

  def getChatGroup(chatGroupId: Long): Future[ChatGroup] = {
    // rescue 用于捕获future异常，因为try catch捕获不了future的异常
    FinagleFactory.client.getChatGroup(chatGroupId, Some(responseFields)) rescue {
      case _: NotFoundException =>
        TwitterFuture {
          throw NotFoundException()
        }
    }
  }

}
