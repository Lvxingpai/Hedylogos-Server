package core.exception

/**
 * Created by pengyt on 2015/8/25.
 */
class BlackListException(override val errorMsg: String) extends StopMessageFilterException(errorMsg)

object BlackListException {
  def apply(errorMsg: String = ""): BlackListException = new BlackListException(errorMsg)
}
