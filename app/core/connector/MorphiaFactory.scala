package core.connector

import com.mongodb.{ MongoClient, MongoClientOptions, MongoCredential, ServerAddress }
import core.GlobalConfig
import models.Conversation
import org.mongodb.morphia.Morphia

import scala.collection.JavaConversions._
import scala.language.postfixOps

/**
 * Created by zephyre on 4/16/15.
 */
object MorphiaFactory {

  lazy val morphia = {
    val m = new Morphia()
    m.map(classOf[Conversation], classOf[models.Message])
    m
  }

  lazy val client = {
    val conf = GlobalConfig.playConf
    val dbName = conf.getString("hedylogos.server.mongo.db").get

    conf.getConfig("backends.mongo")

    val mongoBackends = conf.getConfig("backends.mongo").get
    val services = mongoBackends.subKeys.toSeq map (mongoBackends.getConfig(_).get)

    val serverAddress = services map (c => {
      new ServerAddress(c.getString("host").get, c.getInt("port").get)
    })

    val credential = for {
      user <- conf.getString("hedylogos.server.mongo.user")
      password <- conf.getString("hedylogos.server.mongo.password")
    } yield {
      MongoCredential.createScramSha1Credential(user, dbName, password.toCharArray)
    }

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

    if (credential nonEmpty)
      new MongoClient(serverAddress, Seq(credential.get), options)
    else
      new MongoClient(serverAddress, options)
  }

  lazy val datastore = {
    val dbName = GlobalConfig.playConf.getString("hedylogos.server.mongo.db").get
    val ds = morphia.createDatastore(client, dbName)
    ds.ensureIndexes()
    ds.ensureCaps()
    ds
  }
}
