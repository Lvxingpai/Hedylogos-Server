package core.connector

import com.mongodb.{MongoClientOptions, MongoClient, ServerAddress}
import models.Conversation
import org.mongodb.morphia.{Datastore, Morphia}

import scala.collection.JavaConversions._

/**
 * Created by zephyre on 4/16/15.
 */
object MorphiaFactory {

  private var client: MongoClient = null
  val morphia = new Morphia()

  def getDatastore(dbName: String = "default"): Datastore = {
    val ds = morphia.createDatastore(client, dbName)
    ds.ensureIndexes()
    ds.ensureCaps()
    ds
  }

  def initialize(serverList: Seq[ServerAddress]): Unit = {
    client = new MongoClient(serverList)
    val cl = new MongoClientOptions.Builder()
      //连接超时
      .connectTimeout(60000)
      //IO超时
      .socketTimeout(10000)
      //与数据库能够建立的最大连接数
      .connectionsPerHost(50)
      //每个连接可以有多少线程排队等待
      .threadsAllowedToBlockForConnectionMultiplier(50)
    //morphia.map(classOf[Conversation])
  }
}
