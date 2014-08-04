package data

import model._

object TaskSerializeStrategy extends SerializeStrategy[Task] {
  override def indexUpdates(obj: Task): List[(Key, Boolean)] = {
    val projectId = obj.projectId

    (ProjectSerializeStrategy.tasksKey(projectId), false) ::
      Nil
  }

  override def parents(obj: Task): List[Id] = List(obj.projectId)

}
