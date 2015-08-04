package core.mio

import com.gexin.rp.sdk.base.impl.{ ListMessage, SingleMessage, Target }
import com.gexin.rp.sdk.http.IGtPush
import com.gexin.rp.sdk.template.TransmissionTemplate
import core.formatter.serializer.ObjectMapperFactory
import core.formatter.serializer.InstantMessageSerializer
import core.{ GlobalConfig, User }
import models.Message
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.language.postfixOps

/**
 * 个推推送服务
 *
 * Created by zephyre on 4/20/15.
 */
object GetuiService extends MessageDeliever {

  val conf = GlobalConfig.playConf.getConfig("hedylogos").get
  val master = conf.getString("getui.master").get
  val host = conf.getString("getui.host").get
  val gtAppId = conf.getString("getui.appId").get
  val gtAppKey = conf.getString("getui.appKey").get
  val gtPush = new IGtPush(host, gtAppKey, master)

  private def sendTransmission(msg: Message, clientIdList: Seq[String], muteTargets: Seq[String] = Seq()): Unit = {
    if (clientIdList isEmpty)
      return

    // 需要APNS通知的用户
    val notifTargets = if (muteTargets isEmpty)
      clientIdList
    else
      (clientIdList.toSet -- muteTargets).toSeq

    def buildGetuiMessage(targets: Seq[String], mute: Boolean, contents: String, abbrev: String) = {
      val template = new TransmissionTemplate
      template.setAppId(gtAppId)
      template.setAppkey(gtAppKey)
      template.setTransmissionType(2)
      template.setTransmissionContent(contents)

      if (!mute && abbrev != null && abbrev.nonEmpty)
        template.setPushInfo("", 1, abbrev, "default", "", "", "", "")

      val targetList = targets.map(cid => {
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

      message -> targetList
    }

    val mapper = ObjectMapperFactory().addSerializer(classOf[Message], InstantMessageSerializer[Message]()).build()
    val contents = mapper.writeValueAsString(msg)
    val notifMessage = buildGetuiMessage(notifTargets, mute = false, contents, msg.abbrev)

    // 最终需要发送的非mute消息和mute消息
    val totalMessages = notifMessage :: (if (muteTargets nonEmpty)
      buildGetuiMessage(muteTargets, mute = true, contents, msg.abbrev) :: Nil
    else Nil)

    totalMessages foreach (entry => {
      val targets = entry._2
      val pushResult = entry._1 match {
        case message: SingleMessage => gtPush.pushMessageToSingle(message, targets.head)
        case message: ListMessage =>
          val contentId = gtPush.getContentId(message)
          gtPush.pushMessageToList(contentId, targets)
      }
      Logger.debug("Push result: target=%s, %s".format(clientIdList mkString ",", pushResult.getResponse.toString))
    })
  }

  def sendMesageWithMute(message: Message, allTargets: Seq[Long], mutedTargets: Seq[Long] = Seq()): Future[Message] = {
    // 将targets中的userId取出来，读取regId（类型：Seq[String]）
    def userId2regId(userId: Long): Future[Option[(Long, String)]] = {
      for {
        optionMap <- User.loginInfo(userId)
      } yield for {
        m <- optionMap
        regId <- m.get("regId")
      } yield userId -> regId.toString
    }

    for {
      ret <- Future.sequence(allTargets map userId2regId)
    } yield {
      // 建立完整的 userId -> clientId 映射
      val clientIdMap = Map(ret filter (_.nonEmpty) map (_.get): _*)
      // 这里之所以filter一下，是因为有些userId找不到对应的个推clientId，需要处理这样的情况
      val targets = allTargets filter (clientIdMap contains) map (clientIdMap(_))
      val muted = mutedTargets filter (clientIdMap contains) map (clientIdMap(_))
      sendTransmission(message, targets, muted)
      message
    }
  }

  override def sendMessage(message: Message, targets: Seq[Long]): Future[Message] = sendMesageWithMute(message, targets)
}
