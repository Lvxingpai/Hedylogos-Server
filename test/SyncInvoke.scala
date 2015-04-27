import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
 * Created by zephyre on 4/27/15.
 */
trait SyncInvoke {
  def syncInvoke[T](future: Future[Any]): T = {
    Await.result(future, Duration.Inf).asInstanceOf[T]
  }

}
