package controllers

import play.api.mvc._

object Application extends Controller {

  def index = Action {
    Ok("")
  }

  def getUserDetails(userId: Long) = Ok("safdf").as("application/json")

//  def test() = Action {
//    val ds = MorphiaFactory.getDatastore("default")
//    val obj = new ScalaObject()
//    obj.name = "Haizi"
//    obj.age = 22
//    ds.save[ScalaObject](obj)
//    Ok("")
//  }
}