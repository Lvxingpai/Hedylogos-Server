package core

import models.{ Message, Conversation }
import models.Message.{ ChatType, MessageType }
import org.bson.types.ObjectId

import scala.collection.Map
import scala.concurrent.Future

/**
 * Created by pengyt on 2015/9/23.
 */
trait Chat {

  def getSingleConversation(userA: Long, userB: Long): Future[Conversation]

  def getChatGroupConversation(chatGroupId: Long): Future[Conversation]

  def generateMsgId(cid: ObjectId): Future[Option[Long]]

  def buildMessage(msgType: MessageType.Value, contents: String, cid: ObjectId, receiver: Long, sender: Long, chatType: ChatType.Value): Future[Message]

  def sendMessageToConv(msg: Message, conversation: Conversation, includes: Seq[Long], excludes: Seq[Long]): Future[Message]

  def buildMessageAbbrev(msg: Message): Future[Option[String]]

  def sendMessage(msgType: MessageType.Value, contents: String, receiver: Long, sender: Long, chatType: ChatType.Value, includes: Seq[Long], excludes: Seq[Long]): Future[Message]

  def fetchAndAckMessage(userId: Long, purgeBefore: Long): Future[Seq[Message]]

  def opConversationProperty(uid: Long, cid: ObjectId, settings: Map[String, Boolean]): Future[Unit]

  def opConversationProperty(uid: Long, targetId: Long, settings: Map[String, Boolean]): Future[Unit]

  def getConversation(cid: ObjectId): Future[Option[Conversation]]

  def getConversation(uid: Long, targetId: Long): Future[Option[Conversation]]

  def opMuteNotif(userId: Long, convId: ObjectId, mute: Boolean): Future[Unit]

  def opMuteNotif(userId: Long, targetId: Long, mute: Boolean): Future[Unit]
}
