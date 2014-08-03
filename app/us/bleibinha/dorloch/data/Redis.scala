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

  def save[T <: Model[T] : SerializeStrategy : FastTypeTag : SPickler : Unpickler](obj: T): Future[T] = {
    val serializeStrategy = implicitly[SerializeStrategy[T]]

    val objWithId: T = obj.id.fold {
      val newId = java.util.UUID.randomUUID.toString
      obj.withId(newId)
    } {
      _ => obj
    }
    val id = objWithId.id.get

    val saveResult = client.set(s"$applicationString:$id", objWithId)

    // First save object then update indices
    saveResult map { _ =>
      serializeStrategy.indexUpdates(obj) map {
        case (key, remove) => updateIndex(key, id, remove)
      }
      serializeStrategy.parents(obj).map(updateDependency(_, id))
    }

    saveResult map (_ => objWithId)
  }

  def get[T <: Model[T] : SerializeStrategy : FastTypeTag : SPickler : Unpickler](id: Id): Future[Option[T]] = {
    val serializeStrategy = implicitly[SerializeStrategy[T]]

    val objFuture: Future[Option[T]] = client.get(s"$applicationString:$id")

    objFuture map (_ map serializeStrategy.enrichObject)
  }

  def delete[T <: Model[T] : SerializeStrategy](obj: T): Future[Boolean] = {
    val serializeStrategy = implicitly[SerializeStrategy[T]]

    obj.id.fold {
      Future.successful(false)
    } { id =>
      // First remove from indices, then remove children, then remove object
      val indicesRemoval =
        Future.sequence(
          serializeStrategy.removalIndices(obj) map {
            case (key, Some(id)) => updateIndex(key, id, remove = true)
            case (key, None) => client.del(longIndexKey(key))
          }
        )

      indicesRemoval flatMap { _ =>
        getIdsFromIndex(childrenKey(id)) map {
          _ map {
            childId => client.del(childId)
          }
        }

        serializeStrategy.parents(obj) map (updateDependency(_, id, remove = true))

        client.del(id) map { keysRemoved =>
          if (keysRemoved > 0) true else false
        }
      }
    }
  }

  def getIdsFromIndex(indexKey: Key): Future[List[Id]] =
    client.smembers[String](s"$applicationString:$indexKey") map (_.toList)

  implicit private def genericByteStringFormatter[T <: Model[T] : FastTypeTag : SPickler : Unpickler]: ByteStringFormatter[T] =
    new ByteStringFormatter[T] {
      override def deserialize(bs: ByteString): T = JSONPickle(bs.utf8String).unpickle[T]

      override def serialize(data: T): ByteString = ByteString(data.pickle.value)
    }

  private def updateIndex(indexKey: Key, id: Id, remove: Boolean): Future[Long] =
    if (remove)
      client.srem(longIndexKey(indexKey), id)
    else
      client.sadd(longIndexKey(indexKey), id)

  private def longIndexKey(indexKey: Key) = s"$applicationString:$indexKey"

  private def updateDependency(parent: Id, id: Id, remove: Boolean = false): Future[Long] = {
    val longKey = s"$applicationString:${childrenKey(parent)}"

    if (remove)
      client.srem(longKey, id)
    else
      client.sadd(longKey, id)
  }

  private def childrenKey(parentId: Id): Key = s"$parentId:children"
}
