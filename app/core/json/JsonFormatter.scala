package core.json

import models.AbstractEntity
import play.api.libs.json.JsValue

/**
 * Created by zephyre on 4/23/15.
 */
trait JsonFormatter {
  def format(item: AbstractEntity): JsValue
}
