package core

import play.api.Play
import play.api.Play.current

/**
 * Created by zephyre on 4/17/15.
 */
object GlobalConfig {
  implicit val playConf = Play.configuration
}