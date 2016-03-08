package models

import java.util.Date
import javax.validation.constraints.{ Min, NotNull }

import org.bson.types.ObjectId
import org.mongodb.morphia.annotations.{ Id, Entity }

/**
 * Created by zephyre on 1/25/16.
 */
@Entity
class ConversationView {
  @NotNull
  @Id
  var id: ObjectId = _

  /**
   * 属于哪个用户
   */
  @Min(value = 0)
  var userId: Long = _

  /**
   * 属于哪个会话
   */
  @NotNull
  var conversationId: ObjectId = _

  @NotNull /**
    * 更新日期
    */
  var updateTime: Date = _

  /**
   * 是否有新消息通知
   */
  var notifyFlag: Boolean = _

  /**
   * 有多少条未读信息
   */
  @Min(value = 0)
  var unreadCnt: Int = _

  /**
   * 最后一条消息
   */
  var lastMessage: String = _
}
