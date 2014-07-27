package us.bleibinha.dorloch.model

case class Project(id: Id, title: String, description: Option[String], tasks: List[Task])
