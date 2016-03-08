package core.exception

/**
 * Created by zephyre on 1/26/16.
 */
class ResourceNotFoundException(override val errorMsg: String) extends HedyBaseException(errorMsg)

object ResourceNotFoundException {
  def apply(message: Option[String]) = new ResourceNotFoundException(message getOrElse "")
}
