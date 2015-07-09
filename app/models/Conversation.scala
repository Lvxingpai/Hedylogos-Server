package models

import javax.validation.constraints.{ NotNull, Size }

import org.bson.types.ObjectId
import org.mongodb.morphia.annotations.{ Entity, Indexed }

import scala.beans.BeanProperty
import scala.language.postfixOps

/**
 * Created by pengyt on 2015/7/8.
 */
@Entity
class Conversation extends AbstractEntiry {

  @BeanProperty
  @Size(min = 1, max = 64)
  @Indexed(unique = true)
  var fingerprint: String = ""

  @BeanProperty
  var msgCounter: Long = 0

  @BeanProperty
  @NotNull
  var createTime: Long = 0

  @BeanProperty
  @NotNull
  var updateTime: Long = 0
}

object Conversation {
  val fdParticipants = "participants"
  val fdFingerprint = "fingerprint"
  val fdId = "id"

  def apply(userA: Long, userB: Long): Conversation = {
    val result = new Conversation
    val l = Seq(userA, userB).sorted
    result.id = new ObjectId()
    result.fingerprint = String.format("%d.%d", (l head, l last))
    result.createTime = System.currentTimeMillis()
    result.updateTime = System.currentTimeMillis()
    result
  }
  def apply(objectId: ObjectId, chatGroupId: Long): Conversation = {
    val result = new Conversation
    result.id = objectId
    result.fingerprint = chatGroupId.toString
    result.createTime = System.currentTimeMillis()
    result.updateTime = System.currentTimeMillis()
    result
  }

  def apply(objectId: String, chatGroupId: Long): Conversation = {
    val result = new Conversation
    result.id = new ObjectId(objectId)
    result.fingerprint = chatGroupId.toString
    result.createTime = System.currentTimeMillis()
    result.updateTime = System.currentTimeMillis()
    result
  }
}

