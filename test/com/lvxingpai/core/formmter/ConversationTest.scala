package com.lvxingpai.core.formmter

import com.fasterxml.jackson.databind.JsonNode
import core.formatter.serializer.{ ConversationSerializer, ObjectMapperFactory }
import models.Conversation
import org.bson.types.ObjectId
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{ GivenWhenThen, ShouldMatchers, FeatureSpec }

/**
 * Created by pengyt on 2015/9/23.
 */
class ConversationTest extends FeatureSpec with ShouldMatchers with GivenWhenThen with GeneratorDrivenPropertyChecks {

  implicit override val generatorDrivenConfig = PropertyCheckConfig(minSuccessful = 50, maxDiscarded = Int.MaxValue / 2 - 1)

  feature("ConversationSerializer can serialize conversation object to json") {
    scenario("conversation is valid") {
      val mapper = ObjectMapperFactory().addSerializer(classOf[Conversation], ConversationSerializer[Conversation]()).build()

      forAll(minSuccessful(generatorDrivenConfig.minSuccessful), maxDiscarded(generatorDrivenConfig.maxDiscarded)) {
        (chatGroupId: Long, muted: Boolean, pinned: Boolean, targetId: Long) =>
          val objectId = new ObjectId()
          whenever(objectId != null && chatGroupId > 0 && targetId > 0) {
            val conv = Conversation(objectId, chatGroupId)
            conv.muted = muted
            conv.pinned = pinned
            conv.targetId = targetId
            val node = mapper.valueToTree[JsonNode](conv)
            node.get("id").asText() should be(objectId.toString)
            node.get("muted").asBoolean() should be(muted)
            node.get("pinned").asBoolean() should be(pinned)
            node.get("targetId").asLong() should be(targetId)
            node.size() should be(4)

            //            // 取Gen的值第一种方式
            //            val mutedValue = Gen.oneOf(true, false)
            //            for {
            //              v <- mutedValue
            //            } yield {
            //              v
            //            }
            //            // 取Gen的值第二种方式
            //            forAll(mutedValue) {
            //              m => {
            //                conv.muted = m
            //              }
            //            }
          }
      }
    }
  }
}
