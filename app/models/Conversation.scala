package models

import javax.validation.constraints.{ NotNull, Size }
import java.util.{ List => JList }
import org.bson.types.ObjectId
import org.mongodb.morphia.annotations.{ Transient, Entity, Indexed }

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

  /**
   * 消息免打扰机制。这个list保存了那些设置消息免打扰的用户的ID
   */
  @BeanProperty
  var muteNotif: JList[Long] = null

  /**
   * 是否免打扰。注意：这个值不会保存在数据库中，而是通过muteNotif计算而来。以不同的用户视角观察这个值，结果是不同的。
   */
  @Transient
  var muted: Boolean = false

  /**
   * 是否置顶。和muted字段类似
   */
  @Transient
  var pinned: Boolean = false

  /**
   * 非空conversation所对应的targetId
   */
  @Transient
  var targetId: Long = 0
}

object Conversation {
  //  val fdParticipants = "participants"
  val fdFingerprint = "fingerprint"
  val fdMuteNotif = "muteNotif"
  val fdId = "id"

  def apply(userA: Long, userB: Long): Conversation = {
    val result = new Conversation
    val l = Seq(userA, userB).sorted
    result.id = new ObjectId()
    result.fingerprint = s"${l.head}.${l.last}"
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

