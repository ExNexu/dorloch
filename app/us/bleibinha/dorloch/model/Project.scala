package us.bleibinha.dorloch.model

import scala.concurrent.Future

case class Project(id: Option[Id], title: String, description: Option[String]) extends Model[Project] {
  override def withId(id: Id): Project = copy(id = Some(id))

  lazy val tasks: Future[List[Task]] = Future.successful(Nil)
  lazy val activeTasks: Future[List[Task]] = Future.successful(Nil)
}

object Project {

  def apply(title: String, description: Option[String] = None): Project =
    new Project(None, title, description)

  def apply(
             project: Project,
             tasksFn: => Future[List[Task]],
             activeTasksFn: => Future[List[Task]]
             ): Project =
    new Project(project.id, project.title, project.description) {
      override lazy val tasks: Future[List[Task]] = tasksFn
      override lazy val activeTasks: Future[List[Task]] = activeTasksFn
    }

}
