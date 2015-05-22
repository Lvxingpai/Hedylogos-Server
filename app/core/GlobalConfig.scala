package core

import dispatch.{Http, url}
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.{Configuration, Play}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
 * 处理工程的配置信息
 *
 * Created by zephyre on 4/17/15.
 */
object GlobalConfig {
  implicit val playConf = buildConfig()

  // 获得数据库的配置。主要的键有两个：host和port
  private def getDatabaseConf(service: (String, String)): Future[Configuration] = {
    val serviceName = service._1
    val confKey = service._2

    val page = url(s"http://etcd:2379/v2/keys/backends/$serviceName?recursive=true")
    val response = Http(page OK dispatch.as.String)

    response map (body => {
      val confNode = Json.parse(body).asInstanceOf[JsObject]
      val redisConf = (confNode \ "node" \ "nodes").asOpt[Seq[JsObject]].get.head

      val tmp = (redisConf \ "value").asOpt[String].get.split(":")
      val host = tmp(0)
      val port = tmp(1)
      Configuration.from(Map(confKey -> Map("host" -> host, "port" -> port.toInt)))
    })
  }

  // 从etcd数据库获取配置数据
  private def buildConfig(): Configuration = {
    val page = url("http://etcd:2379/v2/keys/project-conf/hedylogos?recursive=true")
    val response = Http(page OK dispatch.as.String)
    val confNode = Json.parse(Await.result(response, 10 seconds)).asInstanceOf[JsObject]

    val confList = for {
      confEntry <- (confNode \ "node" \ "nodes").asOpt[Seq[JsObject]].get
      conf <- buildConfNode(confEntry)
    } yield conf

    val sideConf = Future.sequence(Seq(getDatabaseConf("redis" -> "redis"), getDatabaseConf("mongo" -> "mongo")))

    val result = Await.result(sideConf, 10 seconds).reduce(_ ++ _)

    Play.configuration ++ Configuration.from(Map(confList: _*)) ++ result
  }

  private def buildConfNode(node: JsObject): Map[String, Any] = {

    // 将数字转换成double或者long或者int
    def procNumber(num: Double): AnyVal = {
      if (Math.floor(num) == num && !Double.box(num).isInfinite) {
        // 这是整数
        if (Math.abs(num.toLong) < Int.MaxValue) num.toInt else num.toLong
      } else num
    }

    // 是否为dir类型的键
    def isDir(rootNode: JsObject): Boolean = {
      val dirNode = node.\("dir").asOpt[Boolean]
      dirNode.nonEmpty && dirNode.get
    }

    // 获得key
    def getKeyName(rootNode: JsObject): String = {
      val keyStr = (rootNode \ "key").asOpt[String].get
      (keyStr split "/").last
    }

    if (isDir(node)) {
      val kvList = for {
        item <- (node \ "nodes").asOpt[Seq[JsValue]].get
        item2 <- buildConfNode(item.asInstanceOf[JsObject]) if item.isInstanceOf[JsObject]
      } yield item2
      val key = getKeyName(node)
      val value = Map(kvList: _*)
      Map(key -> value)
    } else {
      val key = getKeyName(node)
      val valueNode = node \ "value"
      val value = valueNode match {
        case _: JsString => valueNode.asOpt[String].get
        case _: JsNumber => procNumber(valueNode.asOpt[Double].get)
        case _: JsBoolean => valueNode.asOpt[Boolean].get
        case JsNull => null
        case _ => throw new IllegalArgumentException
      }
      Map(key -> value)
    }
  }
}