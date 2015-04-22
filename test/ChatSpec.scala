import core.Chat
import models.{Conversation, HedyRedis, Message, MorphiaFactory}
import org.bson.types.ObjectId
import org.specs2.mutable._
import play.api.test.WithApplication

/**
 * Created by zephyre on 4/20/15.
 */
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
      val msg = Chat.sendMessage(1, "Test", receiver, sender)

      val ds = MorphiaFactory.getDatastore()
      val query = ds.createQuery(classOf[Message]).field("id").equal(msg.getId)
      ds.delete(query)
    }
  }
}
