package core

import com.mongodb.DuplicateKeyException
import models._
import org.bson.types.ObjectId

/**
 * Created by zephyre on 4/20/15.
 */
object Chat {
  /**
   * 获得一个单聊的Conversation
   *
   * @param create  如果该conversation不存在，是否新建？
   * @return
   */
  def singleConversation(userA: Long, userB: Long, create: Boolean = true): Option[Conversation] = {
    assert(userA > 0 && userB > 0 && userA != userB, s"Invalid users: $userA, $userB")

    val ds = MorphiaFactory.getDatastore()
    val c: Conversation = Conversation.create(userA, userB)

    try {
      ds.save[Conversation](c)
      Some(c)
    } catch {
      case e: DuplicateKeyException =>
        val result = ds.find(classOf[Conversation], Conversation.FD_FINGERPRINT, c.getFingerprint).get
        if (result != null) Some(result) else None
    }
  }

  /**
   * 将指定conversation的msgIdCounter自增，并返回，作为新的msgId
   *
   * @param cid Converastion的Id
   * @return
   */
  def generateMsgId(cid: ObjectId): Option[Long] = HedyRedis.client.incr(s"$cid.msgId")

  /**
   * 获得指定conversation的当前msgId
   *
   * @param cid Conversation的Id
   * @return
   */
  def msgId(cid: ObjectId): Option[Long] = {
    val result: Option[String] = HedyRedis.client.get(s"$cid.msgId")
    if (result != None) Some(result.get.toLong) else None
  }

  def groupConversation(fingerprint: String): Option[Conversation] = {
    val ds = MorphiaFactory.getDatastore()
    val result = ds.find(classOf[Conversation], Conversation.FD_FINGERPRINT, fingerprint).get
    if (result != null) Some(result) else None
  }

  def sendMessage(msgType: Int, contents: String, conversation: ObjectId, sender: Long): Message = {
    val msg = new Message()
    msg.setId(new ObjectId())
    msg.setContents(contents)
    msg.setMsgType(msgType)
    msg.setTimestamp(System.currentTimeMillis)
    msg.setConversation(conversation)
    msg.setMsgId(generateMsgId(conversation).get)
    MorphiaFactory.getDatastore().save[Message](msg)
    msg
  }

  def sendMessage(msgType: Int, contents: String, receiver: Long, sender: Long): Message = {
    val c = Chat.singleConversation(sender, receiver).get
    sendMessage(msgType, contents, c.getId, sender)
  }

  def fetchMessage(userId: Long): Seq[Message] = {
    null
  }

  def readMessage(userId: Long, start: Long, end: Long): Unit = {

  }

  def readMessage(userId: Long, msgIdList: Seq[Long]): Unit = {

  }
}
