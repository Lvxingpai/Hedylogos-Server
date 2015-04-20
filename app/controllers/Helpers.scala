package controllers

import play.api.libs.json._
import play.api.mvc.{Result, Results}

object Helpers {

  /**
   * 生成JsValue的返回值
   *
   * @return
   */
  def JsonResponse(retCode: Int = 0, data: JsValue = null, errorMsg: String = null): Result = {
    val response = JsObject(Seq("timestamp" -> JsNumber(System.currentTimeMillis()), "code" -> JsNumber(retCode)))
    if (data != null) response +("result", data)
    if (errorMsg != null) response +("error", JsString(errorMsg))

    Results.Ok(response).withHeaders(("Content-Type", "application/json;charset=utf-8"))
  }
}
