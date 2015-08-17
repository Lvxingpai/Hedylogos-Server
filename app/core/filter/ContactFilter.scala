package core.filter

import misc.FinagleFactory
import models.Message
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import core.Implicits.TwitterConverter._

/**
 * Created by pengyt on 2015/8/17.
 */
class ContactFilter extends Filter {

  def response(isBlack: Boolean, message: Message): AnyRef = {
    if (isBlack) None else message
  }

  val doFilter: PartialFunction[AnyRef, AnyRef] = {
    case futureMsg: Future[Message] => for {
      msg <- futureMsg
      c <- FinagleFactory.client.isContact(msg.senderId, msg.receiverId)
    } yield {
      response(c, msg)
    }
    case msg: Message => for {
      c <- FinagleFactory.client.isContact(msg.senderId, msg.receiverId)
    } yield {
      response(c, msg)
    }
    case None => None
  }

}
