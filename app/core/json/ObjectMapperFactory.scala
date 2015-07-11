package core.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.{ SerializerProvider, JsonSerializer, ObjectMapper }
import models.AbstractEntiry
import org.bson.types.ObjectId

/**
 * Created by zephyre on 7/11/15.
 */
class ObjectMapperFactory {
  private val mapper = new ObjectMapper()
  private val module = new SimpleModule()
  module.addSerializer(classOf[ObjectId], new ObjectIdSerializer())

  /**
   * 默认的ObjectId序列化器
   */
  class ObjectIdSerializer extends JsonSerializer[ObjectId] {
    override def serialize(value: ObjectId, gen: JsonGenerator, serializers: SerializerProvider): Unit =
      gen.writeString(value.toString)
  }

  def addSerializer[T <: AbstractEntiry](clazz: Class[T], ser: JsonSerializer[T]): ObjectMapperFactory = {
    module.addSerializer(clazz, ser)
    this
  }

  def build() = {
    mapper.registerModule(module)
    mapper
  }
}

object ObjectMapperFactory {
  def apply() = new ObjectMapperFactory()
}
