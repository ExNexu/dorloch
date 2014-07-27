package us.bleibinha.dorloch.model

case class Project(id: Option[Id], title: String, description: Option[String], tasks: List[Id]) extends Model[Project] {
  override def withId(id: Id): Project = copy(id = Some(id))
}
