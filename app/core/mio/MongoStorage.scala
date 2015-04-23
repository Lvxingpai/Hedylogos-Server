package core.mio

import java.util

import models.{Message, MorphiaFactory}
import org.bson.types.ObjectId

import scala.collection.JavaConversions._

/**
 * Created by zephyre on 4/22/15.
 */
object MongoStorage extends MessageDeliever {
  private val ds = MorphiaFactory.getDatastore()

  override def sendMessage(message: Message, target: Seq[Long]): Unit = {
    MorphiaFactory.getDatastore().save[Message](message)
  }

  def fetchMessages(idList: Seq[ObjectId]): Seq[Message] = {
    val nlist: java.util.List[ObjectId] = new util.ArrayList[ObjectId]()
    idList.foreach(nlist.append(_))
    val query = ds.createQuery(classOf[Message]).field("id").in(idList)
    query.asList()
  }

  def destroyMessage(idList: Seq[ObjectId]): Unit = {
    val query = ds.createQuery(classOf[Message]).field("id").in(idList)
    ds.delete(query)
  }
}
