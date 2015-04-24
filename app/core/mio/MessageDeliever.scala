package core.mio

import models.Message

import scala.concurrent.Future

/**
 * Created by zephyre on 4/22/15.
 */
trait MessageDeliever {
  def sendMessage(message: Message, target: Seq[Long]): Unit

  def sendMessageAsync(message: Message, target: Seq[Long]): Future[Unit]
}
