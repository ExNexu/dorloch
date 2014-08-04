package controllers

import play.api.data.Forms._
import play.api.data._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import data.Redis
import model.{Id, Project, Task}

import scala.concurrent.Future
import scala.pickling._
import scala.pickling.json._

object Application extends Controller {

  case class ProjectUi(title: String, description: Option[String])
  val projectForm = Form(
    mapping(
      "title" -> nonEmptyText,
      "description" -> optional(text)
    )(ProjectUi.apply)(ProjectUi.unapply)
  )
  case class TaskUi(title: String, description: Option[String])
  val taskForm = Form(
    mapping(
      "title" -> nonEmptyText,
      "description" -> optional(text)
    )(TaskUi.apply)(TaskUi.unapply)
  )

  private val redirectToIndex = Redirect("/")

  def index = Action.async {
    Redis.getAll[Project] map { projects =>
      Ok(views.html.index(projects, projectForm))
    }
  }

  def project(id: Id) = Action.async {
    Redis.get[Project](id) flatMap { projectOption =>
      projectOption.fold(
        Future.successful(redirectToIndex)
      )(project =>
        project.tasks map { tasks =>
          Ok(views.html.viewProject(project, tasks, taskForm))
        }
        )
    }
  }

  def projectPost = Action.async { implicit request =>
    projectForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.addProject(formWithErrors)))
      },
      projectUi => {
        Redis.save(Project(projectUi.title, projectUi.description)) map { project =>
          Ok(views.html.viewProject(project, Nil, taskForm))
        }
      }
    )
  }

  def task(id: Id) = Action.async {
    Redis.get[Task](id) map { taskOption =>
      taskOption.fold(
        redirectToIndex
      )(task =>
        Ok(views.html.viewTask(task))
        )
    }
  }

  def taskPost(projectId: Id) = Action.async { implicit request =>
    taskForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.addTask(projectId, formWithErrors)))
      },
      taskUi => {
        Redis.save(Task(projectId, taskUi.title, taskUi.description)) map { task =>
          Ok(views.html.viewTask(task))
        }
      }
    )
  }
}
