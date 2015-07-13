package core.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.{ JsonSerializer, SerializerProvider }
import models.Message

/**
 * Message对象对应的Json serializer
 *
 */
class MessageSerializer[T <: Message](val routingKey: String) extends JsonSerializer[T] {

  override def serialize(message: T, gen: JsonGenerator, serializers: SerializerProvider): Unit = {
    gen.writeStartObject()

    gen.writeStringField("routingKey", routingKey)

    gen.writeObjectFieldStart("message")

    gen.writeStringField("id", message.id.toString)
    gen.writeStringField("chatType", message.chatType)
    gen.writeNumberField("msgId", message.msgId)
    gen.writeStringField("conversation", message.conversation.toString)
    gen.writeStringField("contents", message.contents.toString)
    gen.writeNumberField("senderId", message.senderId)
    gen.writeStringField("chatType", message.chatType)
    gen.writeNumberField("timestamp", message.timestamp)

    if (message.chatType == "group")
      gen.writeNumberField("groupId", message.receiverId)
    else
      gen.writeNumberField("receiverId", message.receiverId)

    gen.writeEndObject()

    gen.writeEndObject()
  }
}

object MessageSerializer {
  def apply[T <: Message](routingKey: String) = new MessageSerializer[T](routingKey)
}
