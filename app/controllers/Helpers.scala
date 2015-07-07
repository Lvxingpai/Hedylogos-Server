package controllers

import play.api.libs.json._
import play.api.mvc.{ Result, Results }

import scala.collection.mutable.ArrayBuffer

object Helpers {

  /**
   * 生成JsValue的返回值
   *
   * @return
   */
  def JsonResponse(retCode: Int = 0, data: Option[JsValue] = None, errorMsg: Option[String] = None): Result = {
    val c = ArrayBuffer[(String, JsValue)](
      "timestamp" -> JsNumber(System.currentTimeMillis()),
      "code" -> JsNumber(retCode))
    if (data.nonEmpty) c += ("result" -> data.get)
    if (errorMsg.nonEmpty) c += ("error" -> JsString(errorMsg.get))
    Results.Ok(JsObject(c)).withHeaders("Content-Type" -> "application/json;charset=utf-8")
  }
}
