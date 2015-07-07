package core.mio

import com.gexin.rp.sdk.base.impl.{ ListMessage, SingleMessage, Target }
import com.gexin.rp.sdk.http.IGtPush
import com.gexin.rp.sdk.template.TransmissionTemplate
import core.json.MessageFormatter
import core.{ GlobalConfig, User }
import models.Message
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.collection.JavaConversions._
import scala.concurrent.Future

/**
 * 个推推送服务
 *
 * Created by zephyre on 4/20/15.
 */
object GetuiService extends MessageDeliever {

  val conf = GlobalConfig.playConf.getConfig("hedylogos")
  val master = conf.getString("getui.master")
  val host = conf.getString("getui.host")
  val gtAppId = conf.getString("getui.appId")
  val gtAppKey = conf.getString("getui.appKey")
  val gtPush = new IGtPush(host, gtAppKey, master)

  private def sendTransmission(msg: Message, clientIdList: Seq[String]): Unit = {
    val template = new TransmissionTemplate
    template.setAppId(gtAppId)
    template.setAppkey(gtAppKey)
    template.setTransmissionType(2)
    template.setTransmissionContent(MessageFormatter.formatAddRouteKey(msg, "IM").toString())

    // * 如果为普通文本消息，则可以直接通过APNS推送正文
    // * APNS消息的最大长度为maxLen
    val pushText = if (msg.getMsgId == 1) {
      val contents = msg.getContents
      val maxLen = 16
      if (contents.length > maxLen) contents.take(maxLen) + "..." else contents
    } else if (msg.getMsgId == 100) {
      val contents = msg.getContents
      val maxLen = 16
      if (contents.length > maxLen) contents.take(maxLen) + "..." else contents
    } else "你收到了一条新消息"
    template.setPushInfo("", 1, pushText, "default", "", "", "", "")

    val targetList = clientIdList.map(cid => {
      val target = new Target
      target.setAppId(gtAppId)
      target.setClientId(cid)
      target
    })

    val isSingle = targetList.size == 1
    val message = if (isSingle) new SingleMessage else new ListMessage
    message.setData(template)
    message.setOffline(true)
    message.setOfflineExpireTime(3600 * 1000L)

    val pushResult = if (isSingle) gtPush.pushMessageToSingle(message.asInstanceOf[SingleMessage], targetList(0))
    else {
      val contentId = gtPush.getContentId(message.asInstanceOf[ListMessage])
      gtPush.pushMessageToList(contentId, targetList)
    }
    Logger.debug("Push result: target=%s, %s".format(clientIdList mkString ",", pushResult.getResponse.toString))
  }

  override def sendMessage(message: Message, targets: Seq[Long]): Future[Message] = {
    // 将targets中的userId取出来，读取regId（类型：Seq[String]）
    def userId2regId(userId: Long): Future[Option[String]] = {
      for {
        optionMap <- User.loginInfo(userId)
      } yield for {
        m <- optionMap
        regId <- m.get("regId")
      } yield regId.toString
    }

    val regIdList = for {
      values <- Future.sequence(targets.map(userId2regId)) // Future[Seq[Option[String]]]
    } yield for {
      opt <- values if opt.nonEmpty
    } yield opt.get

    regIdList.map(targets => {
      sendTransmission(message, targets)
      message
    })
  }
}
