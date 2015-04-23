package controllers

import play.api.libs.json._
import play.api.mvc.{Result, Results}

import scala.collection.mutable.ArrayBuffer

object Helpers {

  /**
   * 生成JsValue的返回值
   *
   * @return
   */
  def JsonResponse(retCode: Int = 0, data: JsValue = null, errorMsg: String = null): Result = {
    val c = ArrayBuffer[(String, JsValue)]("timestamp" -> JsNumber(System.currentTimeMillis()),
      "code" -> JsNumber(retCode))
    if (data != null) c += ("result" -> data)
    if (errorMsg != null) c += ("error" -> JsString(errorMsg))
    Results.Ok(JsObject(c)).withHeaders("Content-Type" -> "application/json;charset=utf-8")
  }
}
