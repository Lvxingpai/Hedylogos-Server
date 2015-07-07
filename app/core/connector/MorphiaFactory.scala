package core.connector

import com.mongodb.{ MongoClient, MongoClientOptions, MongoCredential, ServerAddress }
import core.GlobalConfig
import models.{ Conversation, UserInfo }
import org.mongodb.morphia.Morphia

import scala.collection.JavaConversions._

/**
 * Created by zephyre on 4/16/15.
 */
object MorphiaFactory {

  lazy val morphia = {
    val m = new Morphia()
    m.map(classOf[Conversation], classOf[models.Group], classOf[models.Message], classOf[models.Sequence],
      classOf[UserInfo])
    m
  }

  lazy val client = {
    val conf = GlobalConfig.playConf
    val dbName = conf.getString("hedylogos.server.mongo.db")

    val mongoBackends = conf.getConfig("backends.mongo").entrySet().toSeq
    val serverAddress = mongoBackends map (backend => {
      val tmp = backend.getValue.unwrapped().toString.split(":")
      val host = tmp(0)
      val port = tmp(1).toInt
      new ServerAddress(host, port)
    })
    val user = conf.getString("hedylogos.server.mongo.user")
    val password = conf.getString("hedylogos.server.mongo.password")
    val credential = MongoCredential.createScramSha1Credential(user, dbName, password.toCharArray)

    val options = new MongoClientOptions.Builder()
      //连接超时
      .connectTimeout(60000)
      //IO超时
      .socketTimeout(10000)
      //与数据库能够建立的最大连接数
      .connectionsPerHost(50)
      //每个连接可以有多少线程排队等待
      .threadsAllowedToBlockForConnectionMultiplier(50)
      .build()

    new MongoClient(serverAddress, Seq(credential), options)
  }

  lazy val datastore = {
    val dbName = GlobalConfig.playConf.getString("hedylogos.server.mongo.db")
    val ds = morphia.createDatastore(client, dbName)
    ds.ensureIndexes()
    ds.ensureCaps()
    ds
  }
}
