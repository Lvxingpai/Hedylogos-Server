package core.filter

import core.Implicits.TwitterConverter._
import core.exception.StopMessageFilterException
import scala.concurrent.ExecutionContext.Implicits.global
import misc.FinagleFactory

import scala.concurrent.Future
import models.Message
/**
 * Created by pengyt on 2015/8/11.
 */
class BlackListFilter extends Filter {
  def response(isBlack: Boolean, message: Message): AnyRef = {
    if (!isBlack)
      message
    else
      throw new StopMessageFilterException("对方拒绝了您的发送")
  }

  val doFilter: PartialFunction[AnyRef, AnyRef] = {
    case futureMsg: Future[Message] => for {
      msg <- futureMsg
      isBlack <- FinagleFactory.client.checkBlackList(msg.senderId, msg.receiverId)
    } yield {
      response(isBlack, msg)
    }
    case msg: Message => for {
      isBlack <- FinagleFactory.client.checkBlackList(msg.senderId, msg.receiverId)
    } yield {
      response(isBlack, msg)
    }
  }
}
