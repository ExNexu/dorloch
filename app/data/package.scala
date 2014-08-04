package object data {
  type Key = String

  implicit val projectSerializeStrategy = ProjectSerializeStrategy
  implicit val taskSerializeStrategy = TaskSerializeStrategy
}
