package core.aspectj

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.{ Around, Aspect }
import play.api.Logger

/**
 * Created by zephyre on 7/7/15.
 */
@Aspect
class AccessLogger {
  @Around(value = "execution(* controllers..*(..)) && @annotation(core.aspectj.WithAccessLog)")
  def log(jp: ProceedingJoinPoint) = {
    val startTime = System.currentTimeMillis
    val result = jp.proceed()
    val endTime = System.currentTimeMillis

    val signature = jp.getSignature
    val message = s"Invoked: ${signature.toLongString}, time cost: ${endTime - startTime} msec"
    Logger.info(message)

    result
  }
}
