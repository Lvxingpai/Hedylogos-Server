package models

import java.util.{ List => JList }

/**
 * Created by zephyre on 1/25/16.
 */
trait Trackable {
  /**
   * 被哪些人阅读过
   */
  var read: JList[Long] = _
}
