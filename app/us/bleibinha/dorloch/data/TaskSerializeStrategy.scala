package us.bleibinha.dorloch.data

import us.bleibinha.dorloch.model._

class TaskSerializeStrategy(projectSerializeStrategy: ProjectSerializeStrategy) extends SerializeStrategy[Task] {
  override def indexUpdates(obj: Task): List[(Key, Boolean)] = {
    val projectId = obj.projectId

    (projectSerializeStrategy.tasksKey(projectId), false) ::
      Nil
  }

  override def parents(obj: Task): List[Id] = List(obj.projectId)

}
