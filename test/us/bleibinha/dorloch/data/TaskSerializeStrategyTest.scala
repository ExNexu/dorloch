package us.bleibinha.dorloch.data

import org.scalatest.concurrent.Eventually
import org.scalatestplus.play.OneAppPerSuite
import us.bleibinha.dorloch.model.{Id, Project, Task}
import us.bleibinha.dorloch.test.DorlochPlaySpec

import scala.pickling._
import scala.pickling.json._

class TaskSerializeStrategyTest extends DorlochPlaySpec with OneAppPerSuite with Eventually {

  lazy val testRedis = new TestRedis

  "A task" should {

    "be serializable, retrievable and deletable" in {
      withProject { projectId =>
        val taskTitle = "Testtask"
        val task1 = awaitFuture(testRedis.save(Task(projectId, taskTitle)))
        task1.id must be('defined)

        val retrievedTask1Option = awaitFuture(testRedis.get[Task](task1.id.get))
        retrievedTask1Option must be('defined)
        val retrievedTask1 = retrievedTask1Option.get
        retrievedTask1.title must be(taskTitle)

        awaitFuture(testRedis.delete(retrievedTask1)) must be(true)
        val retrievedTask1Option2 = awaitFuture(testRedis.get[Task](task1.id.get))
        retrievedTask1Option2 must be(None)
      }
    }

    "automatically create and delete indices" in {
      withProject { projectId =>
        val task1 = awaitFuture(testRedis.save(Task(projectId, "")))
        val taskId = task1.id.get

        eventually {
          val projectTaskIdsLongKey = s"${testRedis.testApplicationString}:$projectId:tasks"
          val projectTaskIds = awaitFuture(testRedis.redisClient.smembers[String](projectTaskIdsLongKey))
          projectTaskIds must be(Seq(taskId))
        }

        awaitFuture(testRedis.delete(task1))
      }
    }

    "automatically register itself as child of project" in {
      withProject { projectId =>
        val task1 = awaitFuture(testRedis.save(Task(projectId, "")))
        val taskId = task1.id.get

        eventually {
          val projectTaskIdsLongKey = s"${testRedis.testApplicationString}:$projectId:children"
          val projectTaskIds = awaitFuture(testRedis.redisClient.smembers[String](projectTaskIdsLongKey))
          projectTaskIds must be(Seq(taskId))
        }

        awaitFuture(testRedis.delete(task1))
      }
    }
  }

  private def withProject[T](test: (Id) => T): Boolean = {
    val project = awaitFuture(testRedis.save(Project("")))
    val projectId = project.id.get
    test(projectId)
    awaitFuture(testRedis.delete(project))
  }
}
