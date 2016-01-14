package core.finagle

import com.lvxingpai.yunkai.{ ChatGroup, ChatGroupProp, NotFoundException, UserInfo, UserInfoProp }
import com.twitter.util.{ Future => TwitterFuture }
import core.finagle.TwitterConverter._
import play.api.Play
import play.api.cache.Cache
import play.api.Play.current
import com.lvxingpai.yunkai.Userservice.{ FinagledClient => YunkaiClient }
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by topy on 2015/7/6.
 */
object FinagleCore {
  lazy val yunkai = Play.application.injector instanceOf classOf[YunkaiClient]

  val basicChatGroupFields = Seq(ChatGroupProp.ChatGroupId, ChatGroupProp.Name, ChatGroupProp.Visible, ChatGroupProp.Avatar, ChatGroupProp.GroupDesc, ChatGroupProp.Id)
  val responseFields = Seq(ChatGroupProp.Id, ChatGroupProp.ChatGroupId, ChatGroupProp.Name, ChatGroupProp.Participants)

  def getUserById(userId: Long, fields: Seq[UserInfoProp] = Seq()): Future[Option[UserInfo]] = {
    val fieldsDigest = fields map (_.value.toString) mkString "|"
    val key = s"user:basic/$userId?digest=$fieldsDigest"
    val cached = Cache.getAs[UserInfo](key)

    cached map (v => Future[Option[UserInfo]](Some(v))) getOrElse {
      val retrievedFields = (fields ++ Seq(UserInfoProp.UserId, UserInfoProp.NickName)).distinct
      val future: Future[Option[UserInfo]] = yunkai.getUserById(userId, Some(retrievedFields), None) map (v => {
        Cache.set(key, v, 100)
        Some(v)
      }) rescue {
        case _: NotFoundException => TwitterFuture(None)
      }
      future
    }
  }

  def getChatGroup(chatGroupId: Long): Future[ChatGroup] = {
    // rescue 用于捕获future异常，因为try catch捕获不了future的异常
    yunkai.getChatGroup(chatGroupId, Some(responseFields)) rescue {
      case _: NotFoundException =>
        TwitterFuture {
          throw NotFoundException()
        }
    }
  }

}
