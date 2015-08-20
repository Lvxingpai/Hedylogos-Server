package core.filter

import core.exception.StopMessageFilterException
import misc.FinagleFactory
import models.Message
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import core.Implicits.TwitterConverter._

/**
 * Created by pengyt on 2015/8/17.
 */
class ContactFilter extends Filter {

  /**
   * 检查两个用户是否存在好友关系
   *
   * @return
   */
  private def contact(userA: Long, userB: Long): Future[Boolean] = FinagleFactory.client.isContact(userA, userB)

  /**
   * 权限检查。根据message，如果sender和receiver不是好友关系，将抛出StopMessageFilterException的异常，终止消息过滤流水线。
   *
   * @param message 需要处理的消息
   * @return
   */
  private def validate(message: Message): Future[Message] = {
    if ("single".equals(message.chatType)) {
      for {
        c <- contact(message.senderId, message.receiverId)
      } yield {
        if (c)
          message
        else
          throw StopMessageFilterException("您不是对方好友, 无法发送消息给对方")
      }
    } else {
      Future { message }
    }
  }

  /**
   * 是否好友关系过滤器
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
