package core.filter

import core.Implicits.TwitterConverter.twitterToScalaFuture
import core.exception.BlackListException
import com.lvxingpai.yunkai.Userservice.{ FinagledClient => YunkaiClient }
import models.Message
import play.api.Play
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Created by pengyt on 2015/8/11.
 */
class BlackListFilter extends Filter {

  lazy val yunkai = Play.application.injector instanceOf classOf[YunkaiClient]

  /**
   * 检查两个用户是否存在block关系
   *
   * @return
   */
  private def isBlocked(userA: Long, userB: Long): Future[Boolean] = yunkai.isBlocked(userA, userB)

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
