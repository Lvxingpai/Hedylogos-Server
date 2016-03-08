package models

import models.Message.{ MessageType, ChatType }
import org.bson.types.ObjectId
import org.mongodb.morphia.annotations.Entity
import scala.collection.JavaConverters._

/**
 * 和订单相关的消息
 * Created by zephyre on 1/25/16.
 */
@Entity("Message")
class OrderMessage extends Message with Trackable

object OrderMessage {
  def apply(contents: String, cid: ObjectId, msgId: Long, receiver: Long, sender: Long): Message = {
    val message = new OrderMessage
    message.id = new ObjectId()
    message.msgType = MessageType.ORDER.id
    message.contents = contents
    message.conversation = cid
    message.msgId = msgId
    message.receiverId = receiver
    message.senderId = sender
    message.chatType = ChatType.SINGLE.toString
    message.read = Seq[Long]().asJava
    message.timestamp = System.currentTimeMillis()
    message
  }
}
