package us.bleibinha.dorloch.controller

import play.api._
import play.api.mvc._
import us.bleibinha.dorloch.data.Redis
import us.bleibinha.dorloch.model.{Id, Project, Task}
import scala.pickling._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.pickling.json._

object Application extends Controller {

  def index = Action {
    Ok(views.html.index(Nil))
  }

  def project(id: Id) = Action.async {
    Redis.get[Project](id) map { projectOption =>
      projectOption.fold(
        Ok(views.html.index(Nil))
      )(project =>
        Ok(views.html.viewProject(project))
        )
    }
  }

  def task(id: Id) = Action.async {
    Redis.get[Task](id) map { taskOption =>
      taskOption.fold(
        Ok(views.html.index(Nil))
      )(task =>
        Ok(views.html.viewTask(task))
        )
    }
  }

}
