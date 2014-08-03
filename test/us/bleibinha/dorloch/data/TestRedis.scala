package us.bleibinha.dorloch.data

class TestRedis extends Redis {
  override protected val applicationString = "dorloch:test"
  val testApplicationString = applicationString
  val redisClient = client
}

trait TestSerializeStrategy {
  def testRedis: Redis
  implicit lazy val projectSerializeStrategy = new ProjectSerializeStrategy(testRedis)
  implicit lazy val taskSerializeStrategy = new TaskSerializeStrategy(projectSerializeStrategy)
}
