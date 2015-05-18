import com.mongodb.ServerAddress
import core.GlobalConfig
import core.connector.MorphiaFactory
import play.api.{Application, GlobalSettings, Logger}

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    Logger.info("Application has started")

    val conf = GlobalConfig.playConf
    val host = conf.getString("mongo.host").get
    val port = conf.getInt("mongo.port").get
    MorphiaFactory.initialize(Array(new ServerAddress(host, port)))
  }

  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
  }


}