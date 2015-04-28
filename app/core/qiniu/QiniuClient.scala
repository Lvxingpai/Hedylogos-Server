package core.qiniu

import com.qiniu.util.{Auth, StringMap}
import play.api.Play
import play.api.Play.current

/**
 * Created by zephyre on 4/25/15.
 */
object QiniuClient {

  private def getConfig: Map[String, String] =
    Map("accessKey" -> Play.configuration.getString("qiniu.accessKey").get,
      "secretKey" -> Play.configuration.getString("qiniu.secretKey").get)

  val secretKey = getConfig("secretKey")

  val accessKey = getConfig("accessKey")

  val auth = Auth.create(accessKey, secretKey)

  val publicBucket = "impubres"

  val privateBucket = "imprivres"

  def uploadToken(key: String, bucket: String = "imres", expire: Long = 3600, policy: Map[String, String] = null) = {
    val m = new StringMap
    if (policy != null) {
      for ((k, v) <- policy.iterator)
        m.putNotEmpty(k, v)
    }

    auth.uploadToken(bucket, key, expire, m)
  }

  def privateDownloadUrl(baseUrl: String, expireSec: Long): String =
    auth.privateDownloadUrl(baseUrl, expireSec)

  def privateDownloadUrl(baseUrl: String): String = auth.privateDownloadUrl(baseUrl)

}
