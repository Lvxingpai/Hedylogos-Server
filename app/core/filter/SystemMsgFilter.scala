package core.filter

import core.EventEmitter
import core.exception.BlackListException
import misc.FinagleFactory
import scala.concurrent.ExecutionContext.Implicits.global
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
   * 触发派派的活动
   *
   * @param message 需要处理的消息
   * @return
   */
  private def emit(message: Message): Unit = {
    if ("single".equals(message.chatType) && isSystemAccout(message.receiverId)) {
      // 触发事件

    }
  }

  /**
   *
   */
  override val doFilter: PartialFunction[AnyRef, AnyRef] = {
    case futureMsg: Future[Message] => for {
      msg <- futureMsg
    } yield {
      emit(msg)
      msg
    }
    case msg: Message =>
      emit(msg)
      Future { msg }
  }
}
