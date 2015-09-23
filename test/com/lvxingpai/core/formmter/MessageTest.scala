package com.lvxingpai.core.formmter

import com.fasterxml.jackson.databind.JsonNode
import core.formatter.serializer.{ ConversationSerializer, ObjectMapperFactory }
import models.Message.{ ChatType, MessageType }
import models.{ Message, Conversation }
import org.bson.types.ObjectId
import org.scalacheck.Gen
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{ GivenWhenThen, ShouldMatchers, FeatureSpec }

/**
 * Created by pengyt on 2015/9/23.
 */
class MessageTest extends FeatureSpec with ShouldMatchers with GivenWhenThen with GeneratorDrivenPropertyChecks {

  implicit override val generatorDrivenConfig = PropertyCheckConfig(minSuccessful = 50, maxDiscarded = Int.MaxValue / 2 - 1)

  feature("ConversationSerializer can serialize conversation object to json") {
    scenario("conversation is valid") {
      val mapper = ObjectMapperFactory().addSerializer(classOf[Conversation], ConversationSerializer[Conversation]()).build()
      forAll(minSuccessful(generatorDrivenConfig.minSuccessful), maxDiscarded(generatorDrivenConfig.maxDiscarded)) {
        val genAbbrev = Gen.option(Gen.alphaStr)
        val genMsgType = Gen.oneOf(MessageType.values.toSeq)
        val genChatType = Gen.oneOf(ChatType.values.toSeq)
        // 最多能有六个随机变量, 再多的话就要用Gen或者Ar
        (contents: String, msgId: Long, receiver: Long, sender: Long) => {
          val cid = new ObjectId()
          whenever(cid != null && msgId > 0 && receiver > 0 && sender > 0) {
            for {
              abbrev <- genAbbrev
              msgType <- genMsgType
              chatType <- genChatType
            } yield {
              val message = Message(msgType, contents, cid, msgId, receiver, sender, chatType)
              message.abbrev = abbrev getOrElse ""

              val node = mapper.valueToTree[JsonNode](message)
              node.get("id").asText() should be(message.id.toString)
              node.get("chatType").asText() should be(chatType)
              node.get("msgId").asLong() should be(msgId)
              node.get("msgType").asInt() should be(msgType)
              node.get("conversation").asText() should be(cid.toString)
              node.get("contents").asText() should be(contents)
              node.get("senderId").asLong() should be(sender)
              node.get("abbrev").asText() should be(receiver)
              node.get("chatType").asText() should be(chatType)
              node.get("abbrev").asText() should be(abbrev.getOrElse(""))
              node.get("timestamp").asLong() should be(message.timestamp)
              if (message.chatType == "group")
                node.get("groupId").asText() should be(receiver)
              else
                node.get("receiverId").asText() should be(receiver)
              node.size() should be(12)
            }
          }
        }
      }
    }
  }
}
