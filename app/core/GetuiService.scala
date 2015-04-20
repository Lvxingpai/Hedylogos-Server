package core

import com.gexin.rp.sdk.base.impl.{SingleMessage, Target}
import com.gexin.rp.sdk.http.IGtPush
import com.gexin.rp.sdk.template.{APNTemplate, TransmissionTemplate}

/**
 * Created by zephyre on 4/20/15.
 */
object GetuiService {
  val master = "euhE1edWrR8yFRphzuYHK1"
  val host = "http://sdk.open.api.igexin.com/apiex.htm"
  val gtAppId = "GiZGT1lA4oAcKbQYJR89F2"
  val gtAppKey = "vFYAPNNkz9653Akzxe3zd8"

  val gtPush = new IGtPush(host, gtAppKey, master)

  def sendNotification(msg: String, deviceToken: String) {
    val apnTemplate = new APNTemplate
    apnTemplate.setPushInfo("", 1, msg, "")
    val apnMessage = new SingleMessage
    apnMessage.setData(apnTemplate)
    gtPush.pushAPNMessageToSingle(gtAppId, deviceToken, apnMessage)
  }

  def sendMessage(msg: String, clientIdList: Seq[String]) {
    val template = new TransmissionTemplate
    template.setAppId(gtAppId)
    template.setAppkey(gtAppKey)
    template.setTransmissionType(1)
    template.setTransmissionContent(msg)

    val targetList = clientIdList.map(cid => {
      val target = new Target
      target.setAppId(gtAppId)
      target.setClientId(cid)
      target
    })

    if (targetList.size == 1) {
      val message = new SingleMessage
      message.setData(template)
      message.setOffline(true)
      message.setOfflineExpireTime(3600 * 1000L)
      gtPush.pushMessageToSingle(message, targetList(0))
    }
  }
}
