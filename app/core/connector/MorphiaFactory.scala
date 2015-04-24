package core.connector

import com.mongodb.{MongoClient, ServerAddress}
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
    morphia.map(classOf[Conversation])
  }
}
