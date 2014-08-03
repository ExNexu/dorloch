package us.bleibinha.dorloch.data

import org.scalatest.concurrent.Eventually
import org.scalatestplus.play.OneAppPerSuite
import us.bleibinha.dorloch.model.{Task, Project}
import us.bleibinha.dorloch.test.DorlochPlaySpec

import scala.pickling._
import scala.pickling.json._

class ProjectSerializeStrategyTest extends DorlochPlaySpec with TestSerializeStrategy with OneAppPerSuite with Eventually {

  override lazy val testRedis = new TestRedis

  "A project" should {

    "be serializable, retrievable and deletable" in {
      val projectTitle = "Testproject"
      val project1 = awaitFuture(testRedis.save(Project(projectTitle)))
      project1.id must be('defined)

      val retrievedProject1Option = awaitFuture(testRedis.get[Project](project1.id.get))
      retrievedProject1Option must be('defined)
      val retrievedProject1 = retrievedProject1Option.get
      retrievedProject1.title must be(projectTitle)

      awaitFuture(testRedis.delete(retrievedProject1)) must be(true)
      val retrievedProject1Option2 = awaitFuture(testRedis.get[Project](project1.id.get))
      retrievedProject1Option2 must be(None)
    }

    "delete dependant tasks when deleted" in {
      val project = awaitFuture(testRedis.save(Project("")))
      val projectId = project.id.get
      val task = awaitFuture(testRedis.save(Task(projectId, "")))
      val taskId = task.id.get

      // wait for children index
      eventually {
        val projectTaskIdsLongKey = s"${testRedis.testApplicationString}:$projectId:children"
        val projectTaskIds = awaitFuture(testRedis.redisClient.smembers[String](projectTaskIdsLongKey))
        projectTaskIds must be(Seq(taskId))
      }

      awaitFuture(testRedis.delete(project))
      eventually {
        val retrievedTaskOption = awaitFuture(testRedis.get[Task](taskId))
        retrievedTaskOption must be(None)
        val projectTaskIdsLongKey = s"${testRedis.testApplicationString}:$projectId:children"
        val projectTaskIds = awaitFuture(testRedis.redisClient.smembers[String](projectTaskIdsLongKey))
        projectTaskIds must be(Seq())
      }
    }

    "have access to its tasks" in {
      val project = awaitFuture(testRedis.save(Project("")))
      val projectId = project.id.get
      val task = awaitFuture(testRedis.save(Task(projectId, "")))
      val taskId = task.id.get

      eventually {
        val retrievedProject = awaitFuture(testRedis.get[Project](projectId)).get
        val tasks = awaitFuture(retrievedProject.tasks)
        tasks must contain(task)
      }

      awaitFuture(testRedis.delete(project))
    }
  }
}
