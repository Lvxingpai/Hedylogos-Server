import java.lang.reflect.Modifier

import core.connector.{HedyRedis, MorphiaFactory}
import core.mio.{GetuiService, MongoStorage, RedisMessaging}
import core.{Chat, User}
import models.{Conversation, Message}
import org.bson.types.ObjectId
import org.junit.runner.RunWith
import org.specs2.mutable._
import org.specs2.runner.JUnitRunner
import play.api.test.WithApplication

/**
 * Created by zephyre on 4/20/15.
 */
@RunWith(classOf[JUnitRunner])
class ChatSpec extends Specification with SyncInvoke {
  "Chat core functionality" should {
//    "Lookup peer-to-peer conversations" in new WithApplication() {
//      val userA: Long = 903
//      val userB: Long = 901
//      val fp = s"$userB.$userA"
//      try {
//        val c = syncInvoke[Option[Conversation]](Chat.singleConversation(userA, userB)).get
//        c.getCreateTime must beEqualTo(c.getUpdateTime)
//
//        c.getFingerprint must beEqualTo(fp)
//        c.getMsgCounter must beEqualTo(0)
//      } finally {
////        val ds = MorphiaFactory.getDatastore()
////        val query = ds.createQuery(classOf[Conversation]).field(Conversation.FD_FINGERPRINT).equal(fp)
////        ds.delete(query)
//      }
//    }

    "Handle message IDs" in new WithApplication() {
      val cid = new ObjectId
      try {
        val msgId1 = syncInvoke[Option[Long]](Chat.msgId(cid))
        val msgId2 = syncInvoke[Option[Long]](Chat.generateMsgId(cid))
        val msgId3 = syncInvoke[Option[Long]](Chat.msgId(cid))

        msgId1 must beEqualTo(None)
        msgId2.get must beEqualTo(1)
        msgId3.get must beEqualTo(1)
      } finally {
        HedyRedis.clients.withClient(_.del(s"$cid.msgId"))
      }
    }

    "Send messages" in new WithApplication() {
      val sender = 901
      val receiver = 903

      val conv = for {
        optConv <- Chat.singleConversation(sender, receiver)
      } yield optConv.get

      val msg = syncInvoke[Message](for {
        c <- conv
        m <- Chat.buildMessage(1, "Test", c.getId, receiver, sender, "single")
      } yield m)

      val c = msg.getConversation

      try {
        syncInvoke[Unit](User.login(receiver, "064d067e917222969111548c83f6664d", None))

        val inspectMsg = (method: String, msg: Message) => classOf[Message].getMethod(method).invoke(msg)
        val checkedFields = classOf[Message].getDeclaredMethods.filter(m =>
          m.getName.startsWith("get") && Modifier.isPublic(m.getModifiers)).map(_.getName)

        syncInvoke(RedisMessaging.sendMessage(msg, Seq(receiver)))
        syncInvoke(MongoStorage.sendMessage(msg, Seq(receiver)))
        syncInvoke(GetuiService.sendMessage(msg, Seq(receiver)))

        val mongoMsgList = syncInvoke[Seq[Message]](MongoStorage.fetchMessages(Seq(msg.getId)))
        val redisMsgList = syncInvoke[Seq[Message]](RedisMessaging.fetchMessages(receiver))

        redisMsgList.length must beEqualTo(1)
        checkedFields.foreach(key => inspectMsg(key, redisMsgList(0)) must beEqualTo(inspectMsg(key, msg)))

        mongoMsgList.length must beEqualTo(1)
        checkedFields.foreach(key => inspectMsg(key, mongoMsgList(0)))
      } finally {
        Chat.destroyConversation(c)
        MongoStorage.destroyMessage(Seq(msg.getId))
        RedisMessaging.destroyFetchSets(Seq(receiver))
        User.destroyUser(receiver)
      }
    }
  }
}
