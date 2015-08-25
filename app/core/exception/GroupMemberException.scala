package core.exception

/**
 * Created by pengyt on 2015/8/25.
 */
class GroupMemberException(override val errorMsg: String) extends StopMessageFilterException(errorMsg)

object GroupMemberException {
  def apply(errorMsg: String = ""): GroupMemberException = new GroupMemberException(errorMsg)
}