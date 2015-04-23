import java.lang.reflect.Modifier

import core.mio.{GetuiService, MongoStorage, RedisMessaging}
import core.{Chat, User}
import models.{Conversation, HedyRedis, Message, MorphiaFactory}
import org.bson.types.ObjectId
import org.junit.runner.RunWith
import org.specs2.mutable._
import org.specs2.runner.JUnitRunner
import play.api.test.WithApplication

/**
 * Created by zephyre on 4/20/15.
 */
@RunWith(classOf[JUnitRunner])
class ChatSpec extends Specification {
  "Chat core functionality" should {
    "Lookup peer-to-peer conversations" in new WithApplication() {
      val userA: Long = 903
      val userB: Long = 901
      val fp = s"$userB.$userA"
      try {

        val c = Chat.singleConversation(userA, userB).get
        c.getCreateTime must beEqualTo(c.getUpdateTime)

        c.getFingerprint must beEqualTo(fp)
        c.getMsgCounter must beEqualTo(0)
      } finally {
        val ds = MorphiaFactory.getDatastore()
        val query = ds.createQuery(classOf[Conversation]).field(Conversation.FD_FINGERPRINT).equal(fp)
        ds.delete(query)
      }
    }

    "Handle message IDs" in new WithApplication() {
      val cid = new ObjectId()
      try {
        val msgId1 = Chat.msgId(cid)
        val msgId2 = Chat.generateMsgId(cid)
        val msgId3 = Chat.msgId(cid)

        msgId1 must beEqualTo(None)
        msgId2.get must beEqualTo(1)
        msgId3.get must beEqualTo(1)
      } finally {
        HedyRedis.client.del(s"$cid.msgId")
      }
    }

    "Send messages" in new WithApplication() {
      val sender = 901
      val receiver = 903

      val c = Chat.singleConversation(sender, receiver).get
      val msg = Chat.buildMessage(1, "Test", c.getId, sender)

      try {
        User.login(receiver, "064d067e917222969111548c83f6664d", None)

        val inspectMsg = (method: String, msg: Message) => classOf[Message].getMethod(method).invoke(msg)
        val checkedFields = classOf[Message].getDeclaredMethods.filter(m =>
          m.getName.startsWith("get") && Modifier.isPublic(m.getModifiers)).map(_.getName)

        RedisMessaging.sendMessage(msg, Seq(receiver))
        MongoStorage.sendMessage(msg, Seq(receiver))
        GetuiService.sendMessage(msg, Seq(receiver))

        val mongoMsgList = MongoStorage.fetchMessages(Seq(msg.getId))
        val redisMsgList = RedisMessaging.fetchMessages(receiver)

        redisMsgList.length must beEqualTo(1)
        checkedFields.foreach(key => inspectMsg(key, redisMsgList(0)) must beEqualTo(inspectMsg(key, msg)))

        mongoMsgList.length must beEqualTo(1)
        checkedFields.foreach(key => inspectMsg(key, mongoMsgList(0)))
      } finally {
        Chat.destroyConversation(c.getId)
        MongoStorage.destroyMessage(Seq(msg.getId))
        RedisMessaging.destroyFetchSets(Seq(receiver))
        User.destroyUser(receiver)
      }
    }
  }
}
