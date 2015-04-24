package core.mio

import models.{Message, MorphiaFactory}
import org.bson.types.ObjectId
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.collection.JavaConversions._
import scala.concurrent.Future

/**
 * Created by zephyre on 4/22/15.
 */
object MongoStorage extends MessageDeliever {
  private val ds = MorphiaFactory.getDatastore()

  override def sendMessageAsync(message: Message, target: Seq[Long]): Future[Unit] = {
    Future {
      MorphiaFactory.getDatastore().save[Message](message)
    }
  }

  def fetchMessages(idList: Seq[ObjectId]): Future[Seq[Message]] = {
    Future {
      if (idList.isEmpty)
        Seq[Message]()
      else {
        val nlist: java.util.List[ObjectId] = idList // new util.ArrayList[ObjectId]()
        val query = ds.createQuery(classOf[Message]).field("id").in(idList)
        query.asList()
      }
    }
  }

  def destroyMessage(idList: Seq[ObjectId]): Unit = {
    val query = ds.createQuery(classOf[Message]).field("id").in(idList)
    ds.delete(query)
  }

  override def sendMessage(message: Message, target: Seq[Long]): Unit = {
    sendMessageAsync(message, target)
  }
}
