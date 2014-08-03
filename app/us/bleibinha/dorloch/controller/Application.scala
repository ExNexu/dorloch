package us.bleibinha.dorloch.controller

import play.api._
import play.api.mvc._
import us.bleibinha.dorloch.data.Redis
import us.bleibinha.dorloch.model.{Id, Project, Task}
import scala.pickling._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.pickling.json._

object Application extends Controller {

  val project1Base = Project("Project 1")
  val project1 = Redis.save(project1Base)
  project1 map { project1 =>
    val task1 = Task(project1.id.get, "Task 1", Some("Some description"))
    Redis.save(task1) map { _ =>
      val proj = Redis.get[Project](project1.id.get)
      proj map println
      proj map (_.map(_.tasks map println))
    }
  }

  def index = Action {
    Ok(views.html.index(Nil))
  }

  def project(id: Id) =  Action {
    //Ok(views.html.viewProject(null))
    Ok(views.html.index(Nil))
  }
  def task(id: Id) = Action {
    //Ok(views.html.viewTask(null))
    Ok(views.html.index(Nil))
  }

}
