package core.filter

import core.Implicits.TwitterConverter._
import core.exception.{ MessageException, BlackListException }
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
      throw new BlackListException(415, "对方拒绝了您的发送")
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
    case None => throw new MessageException(416, "no such message entity")
  }
  //  val doFilter: PartialFunction[AnyRef, AnyRef] = {
  //    case futureOptMsg: Future[Option[Message]] => for {
  //      optMsg <- futureOptMsg
  //      isBlack <- if (optMsg nonEmpty) {
  //        val msg = optMsg.get
  //        FinagleFactory.client.checkBlackList(msg.senderId, msg.receiverId) map (item => Option(item))
  //      } else com.twitter.util.Future(None)
  //    } yield {
  //        if (isBlack nonEmpty)
  //          response(isBlack.get, optMsg.get)
  //        else None
  //      }
  //    case msgOpt: Option[Message] => if (msgOpt nonEmpty) {
  //      val msg = msgOpt.get
  //      for {
  //        isBlack <- FinagleFactory.client.checkBlackList(msg.senderId, msg.receiverId)
  //      } yield {
  //        response(isBlack, msg)
  //      }
  //    } else com.twitter.util.Future(None)
  //    //    case  None => None
  //  }
}
