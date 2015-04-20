package models

import org.bson.types.ObjectId
import org.mongodb.morphia.annotations.{Entity, Id, Transient}

/**
 * Message消息的类
 *
 * Created by zephyre on 4/20/15.
 */
@Entity
class Message() {
  @Id var id: ObjectId = null
  var msgId: Long = 0
  var senderId: Long = 0
  @Transient var sender: String = null
  var conversation: ObjectId = null
  var timestamp: Long = 0
  var contents: String = null
  var fetched: Boolean = false
}
