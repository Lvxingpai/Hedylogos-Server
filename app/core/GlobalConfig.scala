package core

import com.lvxingpai.appconfig.{ AppConfig, EtcdConfBuilder, EtcdServiceBuilder }
import play.api.Configuration
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
import scala.language.postfixOps

/**
 * 处理工程的配置信息
 *
 * Created by zephyre on 4/17/15.
 */
object GlobalConfig {
  lazy val playConf = {

    val defaultConf = Configuration(AppConfig.defaultConfig)

    // 是否为生产环境
    val runlevel = defaultConf.getString("runlevel").orNull

    assert(Seq("production", "dev") contains runlevel, s"Invalid runlevel: $runlevel")

    val isProduction = runlevel == "production"

    val mongoKey = if (isProduction) "mongo" else "mongo-dev"
    val redisKey = "redis-main"
    val confKeys = if (isProduction)
      Seq("hedylogos" -> "hedylogos", "hedylogos-base" -> "hedylogos")
    else
      Seq("hedylogos-dev" -> "hedylogos", "hedylogos-base" -> "hedylogos")

    val services = EtcdServiceBuilder().addKey(mongoKey, "mongo").addKey(redisKey, "redis").build()
    val conf = confKeys.foldLeft(EtcdConfBuilder())((builder, pair) => {
      builder.addKey(pair._1, pair._2)
    }).build()

    val timeout = 30 seconds

    val future = Future.sequence(Seq(services, conf)) map (_ reduce ((c1, c2) => c1 withFallback c2)) map (Configuration(_))

    Await.result(future map (_ ++ defaultConf), timeout)
  }
}