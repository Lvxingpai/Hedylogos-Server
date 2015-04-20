package models

import org.bson.types.ObjectId
import org.mongodb.morphia.annotations.{Id, Entity}

/**
 * Created by zephyre on 4/20/15.
 */
@Entity
class Conversation {
  @Id var id:ObjectId=null
  var country:Long=0

}
