package controllers

import controllers.ChatCtrl.MessageInfo
import core.GlobalConfig.playConf
import core.qiniu.QiniuClient
import org.bson.types.ObjectId
import play.api.libs.json._
import play.api.mvc.{Action, Controller}
import play.api.{Configuration, Logger}

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
      key <- Seq("bucket", "etag", "key", "fname", "fsize", "mimeType", "imageInfo")
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
      "callbackBody" -> params,
      "mimeLimit" -> "image/tiff;image/jpeg;image/png;audio/*"
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
   * 处理七牛图像的回调
   * @param postMap
   * @return
   */
  def qiniuSendMessageCallback(postMap: Map[String, String]) = {
    val msgType = postMap.get("msgType").get.toInt
    val senderId = postMap.get("sender").get.toLong
    val recvId = postMap.get("receiver").map(_.toLong)
    val cid = postMap.get("conversation").map(v => new ObjectId(v))
    val bucket = postMap.get("bucket").get
    val key = postMap.get("key").get

    // 获得contents内容
    val host = playConf.getString(s"qiniu.bucket.$bucket").get
    val baseUrl = s"http://$host/$key"
    val styleSeparator = "!"
    val expire = 7 * 24 * 3600
    Logger.info(playConf.getConfig("qiniu.style").get.entrySet.toString())

    val contents = msgType match {
      case 1 => JsObject(Seq("url" -> JsString(QiniuClient.privateDownloadUrl(baseUrl, expire)))).toString()
      case 2 =>
        val imageInfo = Json.parse(postMap.get("imageInfo").get)

        def buildUrlFromStyle(style: String): String = baseUrl +
          (if (style.nonEmpty) "%s%s".format(styleSeparator, style) else "")

        val styleSet = for {
          (prop, value) <- playConf.getConfig("qiniu.style").get.entrySet
        } yield prop -> JsString(QiniuClient.privateDownloadUrl(buildUrlFromStyle(value.unwrapped.toString), expire))
        Logger.info(styleSet.toString())

        JsObject(Seq(
          "width" -> JsNumber((imageInfo \ "width").asOpt[Int].get),
          "height" -> JsNumber((imageInfo \ "height").asOpt[Int].get)
        ) ++ styleSet.toSeq).toString()
      case _ => throw new IllegalArgumentException
    }
    ChatCtrl.sendMessageBase(MessageInfo(senderId, recvId, cid, msgType, Some(contents)))
  }

  /**
   * 处理七牛的回调。如果action为1，说明这是发送消息时的上传
   * @return
   */
  def qiniuCallback() = Action.async {
    request => {
      val postBody = request.body.asFormUrlEncoded.get
      // 过滤：Seq[String]不为空，并且其内容不为空字符串
      val postMap = Map(postBody.toSeq filter ((item: (String, Seq[String])) =>
        item._2.nonEmpty && item._2(0).nonEmpty): _*).mapValues(_(0))

      postMap("action") match {
        case "1" => qiniuSendMessageCallback(postMap)
        case _ => throw new IllegalArgumentException
      }
    }
  }
}
