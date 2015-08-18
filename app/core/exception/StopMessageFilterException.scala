package core.exception

/**
 * Created by zephyre on 8/18/15.
 */
class StopMessageFilterException(override val errorMsg: String) extends HedyBaseException(999, errorMsg)

object StopMessageFilterException {
  def apply(errorMsg: String = ""): StopMessageFilterException = new StopMessageFilterException(errorMsg)
}
