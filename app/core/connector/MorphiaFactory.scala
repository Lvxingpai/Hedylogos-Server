package core.connector

import com.mongodb._
import org.mongodb.morphia.annotations.Property

import com.mongodb.{ MongoClient, MongoClientOptions, MongoCredential, ServerAddress }
import core.GlobalConfig
import models.Conversation
import org.mongodb.morphia.Morphia


import scala.collection.JavaConversions._

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

    val user = conf.getString("hedylogos.server.mongo.user").get
    val password = conf.getString("hedylogos.server.mongo.password").get
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
    val dbName = GlobalConfig.playConf.getString("hedylogos.server.mongo.db").get
    val ds = morphia.createDatastore(client, dbName)
    ds.ensureIndexes()
    ds.ensureCaps()
    ds
  }

  def getCollection[T](cls: Class[T]): DBCollection = {
    val annotation = cls.getAnnotation(classOf[Property])
    val colName = if (annotation != null)
      annotation.value()
    else
      cls.getSimpleName
    val db = datastore.getDB
    db.getCollection(colName)
  }
}
