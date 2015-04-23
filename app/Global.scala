import com.mongodb.ServerAddress
import models.{HedyRedis, MorphiaFactory}
import play.api.{Logger, Application, GlobalSettings}

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    Logger.info("Application has started")

    MorphiaFactory.initialize(Array(new ServerAddress("119.254.100.93", 4001)))

    HedyRedis.init()
  }

  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
  }
}