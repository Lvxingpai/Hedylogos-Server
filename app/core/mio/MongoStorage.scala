package core.mio

import java.util.Date

import com.lvxingpai.inject.morphia.MorphiaMap
import models.Message.MessageType._
import models.{ ConversationView, Message }
import org.bson.types.ObjectId
import play.api.Play
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.Try

/**
 * Created by zephyre on 4/22/15.
 */
object MongoStorage extends MessageDeliever {
  private val ds = {
    import Play.current

    val morphiaMap = Play.application.injector instanceOf classOf[MorphiaMap]
    morphiaMap.map.get("hedylogos").get
  }

  /**
   * 根据消息内容, 获得摘要. 这是为ConversationView的lastMessage准备的
   * @param message
   * @return
   */
  private def messageAbbrev(message: Message): String = {
    Try(Message.MessageType(message.msgType)).toOption match {
      case Some(TEXT) =>
        val maxLen = 16
        // 截断后的消息
        if (message.contents.length >= maxLen)
          message.contents.take(maxLen) + "..."
        else
          message.contents
      case Some(IMAGE) => "[照片]"
      case Some(AUDIO) => "[音频]"
      case Some(LOCATION) => "[位置]"
      case Some(GUIDE) => "[攻略]"
      case Some(TRAVEL_NOTE) => "[游记]"
      case Some(CITY_POI) => "[目的地]"
      case Some(SPOT) => "[景点]"
      case Some(RESTAURANT) => "[餐厅]"
      case Some(SHOPPING) => "[购物]"
      case Some(HOTEL) => "[酒店]"
      case Some(COMMODITY) => "[商品]"
      case Some(ORDER) => "[订单动态]"
      case _ => ""
    }
  }

  private def updateConversationView(message: Message, targets: Set[Long]): Future[Message] = {
    // 先查看当前已有的ConversationView
    val query = {
      val tmp = ds.createQuery(classOf[ConversationView]) field "conversationId" equal message.conversation
      (if (targets.nonEmpty)
        tmp field "userId" in targets
      else
        tmp).retrievedFields(true, "userId")
    }
    val existed = query.asList().toSeq

    // 这些用户是新增加的, 可能需要新建ConversationView
    val toCreate = targets -- (existed map (_.userId))

    // 升级手段
    val ops = ds.createUpdateOperations(classOf[ConversationView]).inc("unreadCnt", 1).set("updateTime", new Date())
      .set("notifyFlag", true).set("lastMessage", messageAbbrev(message))

    // 更新已有的conversationView
    val future1 = {
      if (existed.nonEmpty) {
        val query = ds.createQuery(classOf[ConversationView]) field "id" in (existed map (_.id))
        Future {
          ds.update(query, ops)
          ()
        }
      } else {
        Future.successful()
      }
    }

    // 新建不存在的conversationView
    val future2 = toCreate map (userId => {
      val query = ds.createQuery(classOf[ConversationView]) field "conversationId" equal message.conversation field
        "userId" equal userId
      Future {
        ds.update(query, ops, true)
        ()
      }
    })

    Future.sequence(future2 + future1) map (_ => message)
  }

  override def sendMessage(message: Message, target: Seq[Long]): Future[Message] = {
    // 需要:
    // 1. 更新ConversationView
    // 2. 写入Message

    val future1 = updateConversationView(message, target.toSet)

    val future2 = Future {
      ds.save[Message](message)
    } map (_ => message)

    Future.sequence(Seq(future1, future2)) map (_ => message)
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
