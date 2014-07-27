package controllers

import play.api._
import play.api.mvc._
import us.bleibinha.dorloch.model.Task

object Application extends Controller {

  def index = Action {
    Ok(views.html.index(List(Task(1L, "Task 1", None))))
  }

}
