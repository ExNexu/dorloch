package data

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import model._

import scala.concurrent.Future
import scala.pickling._
import scala.pickling.json._

object ProjectSerializeStrategy extends SerializeStrategy[Project] {

  override def removalIndices(obj: Project): List[(Key, Option[Id])] =
    (tasksKey(obj.id.get), None) :: Nil

  override def postSerialize(obj: Project, redis: Redis): Project = enrichProject(obj, redis)

  override def postDeserialize(obj: Project, redis: Redis): Project = enrichProject(obj, redis)

  def tasksKey(projectId: Id): Key = s"$projectId:tasks"

  private def enrichProject(proj: Project, redis: Redis): Project = {
    val id = proj.id.get

    def tasksFn: Future[List[Task]] = {
      val taskIds = redis.getIdsFromIndex(tasksKey(id))
      taskIds flatMap { taskIds =>
        val taskOptions = Future.traverse(taskIds) { taskId =>
          redis.get[Task](taskId)
        }
        taskOptions map (_.flatten)
      }
    }

    def activeTasksFn: Future[List[Task]] = Future.successful(Nil)

    Project(proj, tasksFn, activeTasksFn)
  }
}
