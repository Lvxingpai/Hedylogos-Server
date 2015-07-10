package controllers

import core.GlobalConfig
import core.GlobalConfig.playConf
import core.aspectj.WithAccessLog
import core.qiniu.QiniuClient
import models.Message.MessageType
import org.bson.types.ObjectId
import play.api.libs.json._
import play.api.mvc.{ Action, Controller }

/**
 * Created by zephyre on 4/25/15.
 */
object MiscCtrl extends Controller {

  @WithAccessLog
  def uploadToken = Action {
    request =>
      {
        val jsonBody = request.body.asJson.get
        val key = java.util.UUID.randomUUID.toString
        val msgType = MessageType((jsonBody \ "msgType").asOpt[Int].get)

        implicit val playConf = GlobalConfig.playConf.getConfig("hedylogos")

        val token = sendMessageToken(key = key, msgType = msgType)
        val result = JsObject(Seq("key" -> JsString(key), "token" -> JsString(token)))
        Helpers.JsonResponse(data = Some(result))
      }
  }

  /**
   * 获得发送消息的七牛上传token
   *
   * @return
   */
  private def sendMessageToken(bucket: String = "imres", key: String, msgType: MessageType.Value): String = {
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
    val customParams = Seq("sender", "chatType", "receiver", "conversation", "msgType") ++ (msgType match {
      case MessageType.IMAGE | MessageType.AUDIO => Seq()
      case MessageType.LOCATION => Seq("lat", "lng", "address")
      case _ => throw new IllegalArgumentException
    }) map (key => {
      key -> ("$(x:%s)" format key)
    })

    // 魔法变量
    val magicParams = Seq("bucket", "etag", "key", "fname", "fsize", "mimeType") ++ (msgType match {
      case MessageType.IMAGE => Seq("imageInfo", "imageAve")
      case MessageType.AUDIO => Seq("avinfo")
      case MessageType.LOCATION => Seq()
      case _ => throw new IllegalArgumentException
    }) map (key => {
      key -> ("$(%s)" format key)
    })

    val params = urlencode(Map(customParams ++ magicParams: _*))

    val host = playConf.getString("server.host")
    val scheme = playConf.getString("server.scheme")
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

  /**
   * 处理七牛的回调。1-个人；2-群组
   * @return
   */
  @WithAccessLog
  def qiniuCallback() = Action.async {
    request =>
      {
        val postBody = request.body.asFormUrlEncoded.get
        // 过滤：Seq[String]不为空，并且其内容不为空字符串
        val postMap = Map(postBody.toSeq filter ((item: (String, Seq[String])) =>
          item._2.nonEmpty && item._2.head.nonEmpty): _*).mapValues(_.head)

        val msgType = MessageType(postMap("msgType").toInt)
        val senderId = postMap.get("sender").get.toLong
        val chatType = postMap.get("chatType").get.toString
        val recvId = postMap.get("receiver").map(_.toLong)
        val cid = postMap.get("conversation").map(v => new ObjectId(v))
        val bucket = postMap.get("bucket").get
        val key = postMap.get("key").get

        // 获得contents内容
        val conf = playConf.getConfig("hedylogos").get
        val host = conf.getString(s"qiniu.bucket.$bucket").get
        val baseUrl = s"http://$host/$key"
        val styleSeparator = "!"
        val expire = 7 * 24 * 3600

        def buildUrlFromStyle(style: String): String = baseUrl +
          (if (style.nonEmpty) "%s%s".format(styleSeparator, style) else "")

        val contents = JsObject(msgType match {
          case MessageType.IMAGE =>
            val imageInfo = Json.parse(postMap.get("imageInfo").get)

            val entries = conf.getConfig("qiniu.style").get.subKeys.toSeq map (prop =>
              prop -> conf.getString(s"qiniu.style.$prop").get)

            val styleSet = for {
              entry <- entries
            } yield {
              val (prop, style) = entry
              prop -> JsString(QiniuClient.privateDownloadUrl(buildUrlFromStyle(style), expire))
            }
            Seq(
              "width" -> JsNumber((imageInfo \ "width").asOpt[Int].get),
              "height" -> JsNumber((imageInfo \ "height").asOpt[Int].get)
            ) ++ styleSet.toSeq
          case MessageType.AUDIO =>
            val avinfo = Json.parse(postMap.get("avinfo").get)
            val duration = (avinfo \ "audio" \ "duration").asOpt[String].get.toDouble

            Seq(
              "url" -> JsString(QiniuClient.privateDownloadUrl(baseUrl, expire)),
              "duration" -> JsNumber(duration))
          case MessageType.LOCATION =>
            Seq(
              "snapshot" -> JsString(QiniuClient.privateDownloadUrl(buildUrlFromStyle("location"), expire)),
              "lat" -> JsNumber(postMap("lat").toDouble),
              "lng" -> JsNumber(postMap("lng").toDouble),
              "address" -> JsString(postMap("address"))
            )
          case _ => throw new IllegalArgumentException
        }).toString()

        ChatCtrl.sendMessageBase(msgType.id, contents, recvId.get, senderId, chatType)
        //        ChatCtrl.sendMessageBase(MessageInfo(senderId, chatType, recvId.get, msgType.id, Some(contents)))
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
    val chatType = postMap.get("chatType").get.toString
    val recvId = postMap.get("receiver").map(_.toLong)
    val cid = postMap.get("conversation").map(v => new ObjectId(v))
    val bucket = postMap.get("bucket").get
    val key = postMap.get("key").get

    // 获得contents内容
    val conf = playConf.getConfig("hedylogos").get
    val host = conf.getString(s"qiniu.bucket.$bucket").get
    val baseUrl = s"http://$host/$key"
    val styleSeparator = "!"
    val expire = 7 * 24 * 3600

    val contents = msgType match {
      case 1 =>
        val avinfo = Json.parse(postMap.get("avinfo").get)
        val duration = (avinfo \ "audio" \ "duration").asOpt[String].get.toDouble

        JsObject(Seq(
          "url" -> JsString(QiniuClient.privateDownloadUrl(baseUrl, expire)),
          "duration" -> JsNumber(duration))).toString()
      case 2 =>
        val imageInfo = Json.parse(postMap.get("imageInfo").get)

        def buildUrlFromStyle(style: String): String = baseUrl +
          (if (style.nonEmpty) "%s%s".format(styleSeparator, style) else "")

        val entries = conf.getConfig("qiniu.style").get.subKeys.toSeq map (prop =>
          prop -> conf.getString(s"qiniu.style.$prop").get)

        val styleSet = for {
          entry <- entries
        } yield {
          val (prop, style) = entry
          prop -> JsString(QiniuClient.privateDownloadUrl(buildUrlFromStyle(style), expire))
        }

        JsObject(Seq(
          "width" -> JsNumber((imageInfo \ "width").asOpt[Int].get),
          "height" -> JsNumber((imageInfo \ "height").asOpt[Int].get)
        ) ++ styleSet.toSeq).toString()
      case _ => throw new IllegalArgumentException
    }
    ChatCtrl.sendMessageBase(msgType, contents, recvId.get, senderId, chatType)
    //    ChatCtrl.sendMessageBase(MessageInfo(senderId, chatType, recvId.get, msgType, Some(contents)))
  }

}
