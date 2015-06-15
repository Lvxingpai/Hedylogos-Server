import core.GlobalConfig
import core.connector.MorphiaFactory
import models.Conversation
import play.api.{Application, GlobalSettings, Logger}

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    Logger.info("Application has started")

    val conf = GlobalConfig.playConf

    val ds = MorphiaFactory.datastore

    val conv = new Conversation()

    ds.save[Conversation](conv)
  }

  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
  }
}