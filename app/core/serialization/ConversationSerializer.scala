package core.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.{ SerializerProvider, JsonSerializer }
import models.Conversation

/**
 * Created by zephyre on 7/11/15.
 */
class ConversationSerializer[T <: Conversation] extends JsonSerializer[T] {
  override def serialize(value: T, gen: JsonGenerator, serializers: SerializerProvider): Unit = {
    gen.writeStartObject()

    gen.writeStringField("id", value.id.toString)
    gen.writeBooleanField("muted", value.muted)
    gen.writeBooleanField("pinned", value.pinned)
    gen.writeNumberField("targetId", value.targetId)
    gen.writeEndObject()
  }
}

object ConversationSerializer {
  def apply[T <: Conversation]() = new ConversationSerializer[T]()
}
