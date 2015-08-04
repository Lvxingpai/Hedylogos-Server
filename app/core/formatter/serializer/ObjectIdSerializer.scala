package core.formatter.serializer

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.{ SerializerProvider, JsonSerializer }
import org.bson.types.ObjectId

/**
 * 默认的ObjectId序列化器
 */
class ObjectIdSerializer extends JsonSerializer[ObjectId] {
  override def serialize(value: ObjectId, gen: JsonGenerator, serializers: SerializerProvider): Unit =
    gen.writeString(value.toString)
}
