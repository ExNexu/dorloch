package us.bleibinha.dorloch.data

import akka.util.ByteString
import play.api.Play.current
import play.api.libs.concurrent.Akka.system
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import redis.{ByteStringFormatter, RedisClient}
import us.bleibinha.dorloch.model.{Id, Model}

import scala.concurrent.Future
import scala.pickling._
import scala.pickling.json._

object Redis {
  private val client = RedisClient()(system)
  private val applicationString = "dorloch"

  def save[T <: Model[T] : FastTypeTag : SPickler : Unpickler](obj: T): Future[T] = {
    val objWithId: T = obj.id.fold {
      val newId = java.util.UUID.randomUUID.toString
      obj.withId(newId)
    } {
      _ => obj
    }
    val resultFuture = client.set(s"$applicationString:${objWithId.id}", objWithId)
    resultFuture map (_ => objWithId)
  }

  def get[T <: Model[T] : FastTypeTag : SPickler : Unpickler](id: Id): Future[Option[T]] =
    client.get(s"$applicationString:$id")

  implicit private def genericByteStringFormatter[T <: Model[T] : FastTypeTag : SPickler : Unpickler]: ByteStringFormatter[T] =
    new ByteStringFormatter[T] {
      override def deserialize(bs: ByteString): T = JSONPickle(bs.utf8String).unpickle[T]

      override def serialize(data: T): ByteString = ByteString(data.pickle.value)
    }
}
