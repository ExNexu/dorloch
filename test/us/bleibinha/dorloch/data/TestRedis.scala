package us.bleibinha.dorloch.data

class TestRedis extends Redis {
  override protected val applicationString = "dorloch:test"
  val testApplicationString = applicationString
  val redisClient = client
}
