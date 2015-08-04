package core.formatter.serializer

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.{ JsonSerializer, SerializerProvider }
import models.Message

import scala.language.postfixOps

/**
 * Message对象对应的Json serializer
 *
 */
class MessageSerializer[T <: Message](val routingKey: String) extends JsonSerializer[T] {

  override def serialize(message: T, gen: JsonGenerator, serializers: SerializerProvider): Unit = {
    gen.writeStartObject()

    // 可以不启用routingKey机制
    if (routingKey nonEmpty) {
      gen.writeStringField("routingKey", routingKey)
      gen.writeObjectFieldStart("message")
    }

    gen.writeStringField("id", message.id.toString)
    gen.writeStringField("chatType", message.chatType)
    gen.writeNumberField("msgId", message.msgId)
    gen.writeNumberField("msgType", message.msgType)
    gen.writeStringField("conversation", message.conversation.toString)
    gen.writeStringField("contents", message.contents.toString)
    gen.writeNumberField("senderId", message.senderId)
    gen.writeStringField("abbrev", Option(message.abbrev) getOrElse "")
    gen.writeNumberField("timestamp", message.timestamp)

    if (message.chatType == "group")
      gen.writeNumberField("groupId", message.receiverId)
    else
      gen.writeNumberField("receiverId", message.receiverId)

    if (routingKey nonEmpty)
      gen.writeEndObject()

    gen.writeEndObject()
  }
}

object MessageSerializer {
  def apply[T <: Message](routingKey: String = "") = new MessageSerializer[T](routingKey)
}
