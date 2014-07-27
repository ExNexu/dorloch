package us.bleibinha.dorloch.model

case class Task(id: Option[Id], title: String, description: Option[String]) extends Model[Task] {
  override def withId(id: Id): Task = copy(id = Some(id))
}
