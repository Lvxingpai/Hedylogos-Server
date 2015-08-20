package core.exception

/**
 * Created by zephyre on 4/21/15.
 */
class RedisException(override val errorCode: Int, override val errorMsg: String) extends HedyBaseException(errorCode,
  errorMsg)
