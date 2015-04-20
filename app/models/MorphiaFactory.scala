package models

import com.mongodb.{MongoClient, ServerAddress}
import org.mongodb.morphia.{Datastore, Morphia}

import scala.collection.JavaConversions._

/**
 * Created by zephyre on 4/16/15.
 */
object MorphiaFactory {

  private var client: MongoClient = null
  val morphia = new Morphia()

  def getDatastore(dbName: String = "default"): Datastore = {
    morphia.createDatastore(client, dbName)
  }

  def initialize(serverList: Seq[ServerAddress]): Unit = {
    client = new MongoClient(serverList)
  }
}
