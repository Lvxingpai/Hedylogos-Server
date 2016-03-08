package core.filter

import com.lvxingpai.yunkai.Userservice.{ FinagledClient => YunkaiClient }
import core.Implicits.TwitterConverter._
import core.exception.GroupMemberException
import models.Message
import play.api.Play
import play.api.Play.current

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Created by pengyt on 2015/8/17.
 */
class GroupMemberFilter extends Filter {
  lazy val yunkai = Play.application.injector instanceOf classOf[YunkaiClient]

  /**
   * 检查用户是否是群成员
   *
   * @return
   */
  private def member(userId: Long, chatGroupId: Long): Future[Boolean] = yunkai.isMember(userId, chatGroupId)

  /**
   * 权限检查。根据message，如果用户sender不是群receiver中的成员，将抛出StopMessageFilterException的异常，终止消息过滤流水线。
   *
   * @param message 需要处理的消息
   * @return
   */
  private def validate(message: Message): Future[Message] = {
    if ("group".equals(message.chatType)) {
      for {
        m <- member(message.senderId, message.receiverId)
      } yield {
        if (m)
          message
        else
          throw GroupMemberException("您还不是群成员")
      }
    } else {
      Future { message }
    }
  }

  /**
   * 用户是否为群成员过滤器
   */
  override val doFilter: PartialFunction[AnyRef, AnyRef] = {
    case futureMsg: Future[Message] =>
      for {
        msg <- futureMsg
        validatedMessage <- validate(msg)
      } yield {
        validatedMessage
      }
    case msg: Message => validate(msg)
  }
}
