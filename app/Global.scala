import play.api.{ Application, GlobalSettings, Logger }
import play.api.mvc.RequestHeader

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    Logger.info("Application has started")
  }

  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
  }

  override def onRequestCompletion(request: RequestHeader): Unit = {
  }
}