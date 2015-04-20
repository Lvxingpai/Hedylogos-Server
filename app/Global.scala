import com.mongodb.ServerAddress
import models.MorphiaFactory
import play.api.Play.current
import play.api._
import play.api.cache.Cache

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    Logger.info("Application has started")
    MorphiaFactory.initialize(Array(new ServerAddress("119.254.100.93", 4001)))

    // Liang
    Cache.set("1001.regId", "064d067e917222969111548c83f6664d")
    Cache.set("1001.dt", "7df520926aa129a190424f2c41dc5928cdfda580697f613cdeb9be71bc1e3c33")
  }

  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
  }
}