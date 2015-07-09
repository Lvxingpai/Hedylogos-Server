package models

import org.bson.types.ObjectId
import org.mongodb.morphia.annotations.{ Entity, Indexed }

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
  var msgType: Integer = 0

  @BeanProperty
  var timestamp: Long = 0
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
    val CMD = Value(100)
    val TIP = Value(200)
  }

  def apply(msgType: MessageType.Value, contents: String, cid: ObjectId, msgId: Long, receiver: Long, sender: Long, chatType: String): Message = {
    val message = new Message
    message.id = new ObjectId()
    message.msgType = msgType.id
    message.contents = contents
    message.conversation = cid
    message.msgId = msgId
    message.receiverId = receiver
    message.senderId = sender
    message.chatType = chatType
    message.timestamp = System.currentTimeMillis()
    message
  }
}
