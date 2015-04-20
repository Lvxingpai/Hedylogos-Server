import core.GetuiService
import org.specs2.mutable._
import play.api.cache.Cache
import play.api.test.WithApplication

/**
 * Created by zephyre on 4/20/15.
 */
class ChatSpec extends Specification {
  "Chats" should {
    "send message" in new WithApplication() {
      Cache.set("1001.regId", "064d067e917222969111548c83f6664d")
      Cache.set("1001.dt", "7df520926aa129a190424f2c41dc5928cdfda580697f613cdeb9be71bc1e3c33")

      val dt = "7df520926aa129a190424f2c41dc5928cdfda580697f613cdeb9be71bc1e3c33"

      GetuiService.sendNotification("test", dt)
    }
  }
}
