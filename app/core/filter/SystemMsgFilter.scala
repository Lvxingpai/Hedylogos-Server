package core.filter

import core.exception.BlackListException
import misc.FinagleFactory
import models.Message

import scala.concurrent.Future

/**
 * Created by pengyt on 2015/8/26.
 */
class SystemMsgFilter extends Filter {
  private val systemAccouts: Seq[Long] = Seq(10000, 10001)
  /**
   * 检查用户Id是否是系统账号
   *
   * @return
   */
  private def isSystemAccout(userId: Long): Boolean = {
    systemAccouts.contains(userId)
  }

  /**
   * 权限检查。根据message，如果sender在receiver的黑名单中，将抛出StopMessageFilterException的异常，终止消息过滤流水线。
   *
   * @param message 需要处理的消息
   * @return
   */
  private def validate(message: Message): Future[Message] = {
    if ("single".equals(message.chatType)) {
      for {
        // sender是否在receiver的黑名单中
        block <- isBlocked(message.receiverId, message.senderId)
      } yield {
        if (block)
          throw BlackListException("对方拒绝了您的消息")
        else
          message
      }
    } else {
      Future { message }
    }
  }

  /**
   *
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
