package models

import org.bson.types.ObjectId
import org.mongodb.morphia.annotations.{ Entity, Id }

import scala.beans.BeanProperty

/**
 * Created by pengyt on 2015/7/8.
 */
@Entity
class AbstractEntiry {
  @BeanProperty
  @Id
  var id: ObjectId = null
}
object AbstractEntiry {
  val fdId = "id"
}