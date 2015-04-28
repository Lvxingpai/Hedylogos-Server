package controllers

import java.io.File

import com.qiniu.storage.UploadManager
import core.qiniu.QiniuClient
import play.api.Play.current
import play.api.libs.json._
import play.api.mvc.{Action, Controller}
import play.api.{Logger, Play}

/**
 * Created by zephyre on 4/25/15.
 */
object MiscCtrl extends Controller {

  /**
   * 获得发送消息的七牛上传token
   *
   * @param jsonBody
   * @return
   */
  private def sendMessageToken(bucket: String = "imres", key: String, jsonBody: JsValue): String = {
    // 生成Map(location->x:location, price->x:price)之类的用户自定义变量
    // 参见：http://developer.qiniu.com/docs/v6/api/overview/up/response/vars.html#xvar
    val extParamters: Map[String, String] =
      Map(jsonBody.asInstanceOf[JsObject].keys filter (_ startsWith "x:") map
        (key => (key substring 2) -> "$(%s)".format(key)) toSeq: _*)

    // 生成urlencoded格式的数据
    def urlencoder(m: Map[String, String]): String = {
      val termList = for {
        (k, v) <- m
      } yield s"$k=$v"

      termList mkString "&"
    }

    val locParams = Seq("bucket" -> "$(bucket)", "key" -> "$(key)", "etag" -> "$(etag)", "caller" -> "qiniu")
    val params = Map(locParams ++ extParamters.toSeq: _*)

    val host = Play.configuration.getString("server.host").get
    val scheme = Play.configuration.getString("server.scheme").get
    val href = routes.MiscCtrl.qiniuCallback().url
    val callbackUrl = s"$scheme://$host$href"
    val expire = 3600

    val deadline = System.currentTimeMillis / 1000 + expire
    val putPolicy = Map(
      "scope" -> s"$bucket:$key",
      "deadline" -> deadline.toString,
      "callbackUrl" -> callbackUrl,
      "callbackHost" -> host,
      "callbackBody" -> urlencoder(params)
//      "returnBody" -> Json.toJson(params).toString
//      "mimeLimit" -> "image/tiff;image/jpeg;image/png"
    )
    QiniuClient.uploadToken(key, expire = expire, policy = putPolicy)
  }

  private def upload(key: String, token: String): Unit = {
    val uploadMgr = new UploadManager
    val response = uploadMgr.put(new File("/Users/zephyre/Desktop/zen-297x300.jpg"), key, token)
    Logger.info(response.toString)

  }

  def uploadToken = Action {
    request => {
      val jsonBody = request.body.asJson.get
      val key = java.util.UUID.randomUUID.toString
      val token = (jsonBody \ "action").asOpt[Int].get match {
        case 1 => sendMessageToken(key = key, jsonBody = jsonBody)
        case _ => throw new IllegalArgumentException
      }

      val result = JsObject(Seq("key" -> JsString(key), "token" -> JsString(token)))

      Helpers.JsonResponse(data = Some(result))
    }
  }

  /**
   * 处理七牛的回调。如果action为1，说明这是发送消息时的上传
   * @return
   */
  def qiniuCallback() = Action.async {
    request => {
      val postData = request.body.asFormUrlEncoded.get
      postData("action")(0) match {
        case "1" => ChatCtrl.sendMessageQiniu(request)
        case _ => throw new IllegalArgumentException
      }
    }
  }
}
