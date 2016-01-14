package core.mio

import com.lvxingpai.inject.morphia.MorphiaMap
import models.Message
import org.bson.types.ObjectId
import play.api.Play
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.language.postfixOps

/**
 * Created by zephyre on 4/22/15.
 */
object MongoStorage extends MessageDeliever {
  private val ds = {
    import Play.current

    val morphiaMap = Play.application.injector instanceOf classOf[MorphiaMap]
    morphiaMap.map.get("hedylogos").get
  }

  override def sendMessage(message: Message, target: Seq[Long]): Future[Message] = {
    Future {
      ds.save[Message](message)
    } map (_ => message)
  }

  def sendMessageList(messages: Seq[Message], target: Seq[Long]): Future[Seq[Message]] = {
    Future {
      ds.save[Message](messages)
    } map (_ => messages)
  }

  def fetchMessages(idList: Seq[ObjectId]): Future[Seq[Message]] = {
    Future {
      if (idList isEmpty)
        Seq[Message]()
      else {
        ds.createQuery(classOf[Message]) field "id" in idList asList
      }
    }
  }

  def destroyMessage(idList: Seq[ObjectId]): Unit = {
    val query = ds.createQuery(classOf[Message]) field "id" in idList
    ds.delete(query)
  }
}
