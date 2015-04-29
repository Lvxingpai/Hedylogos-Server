import play.api.test.FakeRequest

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
 * Created by zephyre on 4/27/15.
 */
trait SyncInvoke {
  def syncInvoke[T](future: Future[Any]): T = {
    Await.result(future, Duration.Inf).asInstanceOf[T]
  }

  def buildRequest[T](req: FakeRequest[T]): FakeRequest[T] = req.withHeaders("Content-Type"->"application/json")
}
