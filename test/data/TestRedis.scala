package data

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class TestRedis extends Redis {
  override protected val applicationString = "dorloch:test"
  val testApplicationString = applicationString
  val redisClient = client

  //remove all previous test keys
  Await.result(client.keys(s"$applicationString*") flatMap client.del, 2 seconds)
}
