package us.bleibinha.dorloch.model

trait Model[T] {
  def id: Option[Id]
  def withId(id: Id): T
}
