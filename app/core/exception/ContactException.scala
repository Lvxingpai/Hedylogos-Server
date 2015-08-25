package core.exception

/**
 * Created by pengyt on 2015/8/25.
 */
class ContactException(override val errorMsg: String) extends StopMessageFilterException(errorMsg)

object ContactException {
  def apply(errorMsg: String = ""): ContactException = new ContactException(errorMsg)
}
