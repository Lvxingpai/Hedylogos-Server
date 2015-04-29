package controllers

import core.qiniu.QiniuClient
import play.api.Configuration
import play.api.libs.json._
import play.api.mvc.{Action, Controller}
import core.GlobalConfig.playConf

/**
 * Created by zephyre on 4/25/15.
 */
object MiscCtrl extends Controller {

  /**
   * 获得发送消息的七牛上传token
   *
   * @return
   */
  private def sendMessageToken(bucket: String = "imres", key: String)(implicit playConf: Configuration): String = {
    // 生成Map(location->x:location, price->x:price)之类的用户自定义变量
    // 参见：http://developer.qiniu.com/docs/v6/api/overview/up/response/vars.html#xvar

    // 生成urlencoded格式的数据
    def urlencode(m: Map[String, String]): String = {
      val termList = for {
        (k, v) <- m
      } yield s"$k=$v"

      termList mkString "&"
    }

    // 自定义变量
    val customParams = for {
      key <- Seq("sender", "receiver", "conversation", "msgType", "action")
    } yield key -> "$(x:%s)".format(key)

    // 魔法变量
    val magicParams = for {
      key <- Seq("bucket", "etag", "fname", "fsize", "mimeType", "imageInfo")
    } yield key -> "$(%s)".format(key)

    val params = urlencode(Map(customParams ++ magicParams :+ "action" -> "1": _*))

    val host = playConf.getString("server.host").get
    val scheme = playConf.getString("server.scheme").get
    val href = routes.MiscCtrl.qiniuCallback().url
    val callbackUrl = s"$scheme://$host$href"
    val expire = 3600

    val deadline = System.currentTimeMillis / 1000 + expire
    val putPolicy = Map(
      "scope" -> s"$bucket:$key",
      "saveKey" -> key,
      "deadline" -> deadline.toString,
      "callbackUrl" -> callbackUrl,
      "callbackBody" -> params
      //      "mimeLimit" -> "image/tiff;image/jpeg;image/png"
    )
    QiniuClient.uploadToken(key, expire = deadline, policy = putPolicy)
  }

  def uploadToken = Action {
    request => {
      val jsonBody = request.body.asJson.get
      val key = java.util.UUID.randomUUID.toString
      val token = (jsonBody \ "action").asOpt[Int].get match {
        case 1 => sendMessageToken(key = key)
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
