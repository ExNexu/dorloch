package us.bleibinha.dorloch.controller

import play.api._
import play.api.mvc._
import us.bleibinha.dorloch.model.{Id, Project, Task}

object Application extends Controller {

  val task1 = Task(1L, "Task 1", Some("Some description"))
  val project1 = Project(1L, "Project 1", None, List(task1))

  def index = Action {
    Ok(views.html.index(List(project1)))
  }

  def project(id: Id) =  Action {
    Ok(views.html.viewProject(project1))
  }
  def task(id: Id) = Action {
    Ok(views.html.viewTask(task1))
  }

}
