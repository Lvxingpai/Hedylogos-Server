package models

import org.bson.types.ObjectId
import org.mongodb.morphia.annotations.{ Transient, Entity, Indexed }

import scala.beans.BeanProperty

/**
 * Created by zephyre on 6/26/15.
 */
@Entity
class Message extends AbstractEntiry {

  @BeanProperty
  @Indexed()
  var conversation: ObjectId = new ObjectId()

  @BeanProperty
  @Indexed()
  var msgId: Long = 0

  @BeanProperty
  var senderId: Long = 0

  @BeanProperty
  var receiverId: Long = 0

  @BeanProperty
  var senderName: String = null

  @BeanProperty
  var chatType: String = null

  @BeanProperty
  var senderAvatar: String = null

  @BeanProperty
  var contents: String = null

  @BeanProperty
  var msgType: Int = 0

  /**
   * 消息的摘要。用于显示在通知中心里面
   */
  @Transient
  var abbrev: String = null

  @BeanProperty
  var timestamp: Long = 0

  @BeanProperty
  var targets: java.util.List[Long] = null
}

object Message {

  object MessageType extends Enumeration {
    val TEXT = Value(0)
    val AUDIO = Value(1)
    val IMAGE = Value(2)
    val LOCATION = Value(3)
    val GUIDE = Value(10)
    val CITY_POI = Value(11)
    val TRAVEL_NOTE = Value(12)
    val SPOT = Value(13)
    val RESTAURANT = Value(14)
    val SHOPPING = Value(15)
    val HOTEL = Value(16)
    val QA = Value(17)
    val HTML = Value(18)
    val COMMODITY = Value(19)
    val ORDER = Value(20)
    val COUPON = Value(21)
    val CMD = Value(100)
    val TIP = Value(200)
  }

  object ChatType extends Enumeration {
    /**
     * 单聊
     */
    val SINGLE = Value(0, "single")

    /**
     * 群聊
     */
    val CHATGROUP = Value(1, "group")
  }

  def apply(msgType: MessageType.Value, contents: String, cid: ObjectId, msgId: Long, receiver: Long, sender: Long, chatType: ChatType.Value): Message = {
    val message = new Message
    message.id = new ObjectId()
    message.msgType = msgType.id
    message.contents = contents
    message.conversation = cid
    message.msgId = msgId
    message.receiverId = receiver
    message.senderId = sender
    message.chatType = chatType.toString
    message.timestamp = System.currentTimeMillis()
    message
  }
}
