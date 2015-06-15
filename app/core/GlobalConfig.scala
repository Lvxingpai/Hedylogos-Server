package core

import com.lvxingpai.appconfig.AppConfig
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * 处理工程的配置信息
 *
 * Created by zephyre on 4/17/15.
 */
object GlobalConfig {
//  implicit val playConf = buildConfig()

  implicit val playConf = {
    val defaultConf = AppConfig.defaultConfig

    // 是否为生产环境
    val isProduction = defaultConf.hasPath("runlevel") && defaultConf.getString("runlevel") == "production"
    val mongoKey = if (isProduction) "mongo" else "mongo-dev"
    val timeout = 30 seconds

    Await.result(AppConfig.buildConfig(
      Some(Seq("hedylogos" -> "hedylogos")),
      Some(Seq(mongoKey -> "mongo", "redis-main" -> "redis"))),
      timeout)
  }
}