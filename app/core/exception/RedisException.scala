package core.exception

/**
 * Created by zephyre on 4/21/15.
 */
class RedisException(val errorCode: Int, val errorMsg: String) extends HedyBaseException {

}
