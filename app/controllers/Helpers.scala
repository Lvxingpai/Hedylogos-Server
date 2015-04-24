package controllers

import play.api.libs.json._
import play.api.mvc.{Result, Results}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object Helpers {

  /**
   * 生成JsValue的返回值
   *
   * @return
   */
  def JsonResponseBlocked(retCode: Int = 0, data: JsValue = null, errorMsg: String = null): Result = {
    Await.result(JsonResponse(retCode, Future(data), errorMsg), Duration.Inf)
  }

  def JsonResponse(retCode: Int = 0, data: Future[JsValue] = null, errorMsg: String = null): Future[Result] = {
    data.map(v => {
      val c = ArrayBuffer[(String, JsValue)]("timestamp" -> JsNumber(System.currentTimeMillis()),
        "code" -> JsNumber(retCode))
      if (data != null) c += ("result" -> v)
      if (errorMsg != null) c += ("error" -> JsString(errorMsg))
      Results.Ok(JsObject(c)).withHeaders("Content-Type" -> "application/json;charset=utf-8")
    })
  }
}
