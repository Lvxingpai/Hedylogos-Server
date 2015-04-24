package controllers

import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._

object Application extends Controller {

  def index = Action {
    Ok("")
  }

  def computation(count: Int): Int = {
    for (i <- 0 until count) {
      Logger.info(s"#$i")
      Thread.sleep(800)
    }
    count
  }

  def intensiveComputation(): Int = 2

  def someAsyncAction = Action.async {
    val futureInt = scala.concurrent.Future {
      intensiveComputation()
    }
    futureInt.map(i => Ok("Got result: " + i))
  }

  def test() = Action.async {
    val f1 = scala.concurrent.Future {
      computation(6)
    }
    f1.map(i => Ok("Got result: " + i))
  }
}