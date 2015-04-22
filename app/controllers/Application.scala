package controllers

import play.api.mvc._

object Application extends Controller {

  def index = Action {
    Ok("")
  }

  def getUserDetails(userId: Long) = Ok("safdf").as("application/json")

  def test() = Action {
    Ok("")
  }
}