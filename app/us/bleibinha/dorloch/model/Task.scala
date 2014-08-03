package us.bleibinha.dorloch.model

case class Task(id: Option[Id], projectId: Id, title: String, description: Option[String]) extends Model[Task] {
  override def withId(id: Id): Task = copy(id = Some(id))
}

object Task {
  def apply(projectId: Id, title: String, description: Option[String] = None): Task =
    Task(None, projectId, title, description)
}
