package core.filter

import com.lvxingpai.yunkai.BlackListException
import core.Implicits.TwitterConverter._
import scala.concurrent.ExecutionContext.Implicits.global
import misc.FinagleFactory

import scala.concurrent.Future
import models.Message
/**
 * Created by pengyt on 2015/8/11.
 */
class BlackListFilter extends Filter {
  def response(isBlack: Boolean, message: Message): AnyRef = {
    if (isBlack) None else message
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
    case None => None
    }

}
