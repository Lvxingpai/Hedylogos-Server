package core.mio

import models.Message

/**
 * Created by zephyre on 4/22/15.
 */
trait MessageDeliever {
  def sendMessage(message: Message)
}
