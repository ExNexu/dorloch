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

trait Redis {
  protected implicit val akkaSystem = system
  protected val applicationString = "dorloch"
  protected val client = RedisClient()

  def save[T <: Model[T] : SerializeStrategy : FastTypeTag : SPickler : Unpickler](obj: T): Future[T] = {
    val typeTag = implicitly[FastTypeTag[T]]
    val typeFullName = typeTag.tpe.typeSymbol.fullName
    val serializeStrategy = implicitly[SerializeStrategy[T]]

    val objWithId: T = obj.id.fold {
      val newId = java.util.UUID.randomUUID.toString
      obj.withId(newId)
    } {
      _ => obj
    }
    val id = objWithId.id.get

    val saveResult = client.set(longKey(id), objWithId)

    // First save object, then update indices, then add to all list
    saveResult map { _ =>
      serializeStrategy.indexUpdates(obj) map {
        case (key, remove) => updateIndex(key, id, remove)
      }
      serializeStrategy.parents(obj).map(updateDependency(_, id))
      updateIndex(allKey(typeFullName), id)
    }

    saveResult map (_ => serializeStrategy.postSerialize(objWithId, this))
  }

  def get[T <: Model[T] : SerializeStrategy : FastTypeTag : SPickler : Unpickler](id: Id): Future[Option[T]] = {
    val serializeStrategy = implicitly[SerializeStrategy[T]]

    val objFuture: Future[Option[T]] = client.get(longKey(id))

    objFuture map (_ map (serializeStrategy.postDeserialize(_, this)))
  }

  def getAll[T <: Model[T] : SerializeStrategy : FastTypeTag : SPickler : Unpickler]: Future[List[T]] = {
    val typeTag = implicitly[FastTypeTag[T]]
    val typeFullName = typeTag.tpe.typeSymbol.fullName
    val serializeStrategy = implicitly[SerializeStrategy[T]]

    val allIds = getIdsFromIndex(allKey(typeFullName))
    allIds flatMap { allIds =>
      val objOptions = Future.traverse(allIds) { id =>
        get[T](id) map (_ map (serializeStrategy.postDeserialize(_, this)))
      }
      objOptions map (_.flatten)
    }
  }

  def delete[T <: Model[T] : SerializeStrategy : FastTypeTag](obj: T): Future[Boolean] = {
    val typeTag = implicitly[FastTypeTag[T]]
    val typeFullName = typeTag.tpe.typeSymbol.fullName
    val serializeStrategy = implicitly[SerializeStrategy[T]]

    obj.id match {
      case None =>
        Future.successful(false)
      case Some(id) =>
        // First remove from indices, then remove children, then remove object
        val indicesRemoval =
          Future.sequence(
            serializeStrategy.removalIndices(obj) map {
              case (key, Some(removalId)) => updateIndex(key, removalId, remove = true)
              case (key, None) => client.del(longKey(key))
            }
          )
        val allRemoval = updateIndex(allKey(typeFullName), id, remove = true)

        indicesRemoval flatMap { _ =>
          allRemoval flatMap { _ =>
            getIdsFromIndex(childrenKey(id)) map {
              _ map {
                childId =>
                  client.del(longKey(childId)) map { _ =>
                    updateDependency(id, childId, remove = true)
                  }
              }
            }

            serializeStrategy.parents(obj) map (updateDependency(_, id, remove = true))

            client.del(longKey(id)) map { keysRemoved =>
              if (keysRemoved > 0) true else false
            }
          }
        }
    }
  }

  def getIdsFromIndex(indexKey: Key): Future[List[Id]] =
    client.smembers[String](longKey(indexKey)) map (_.toList)

  implicit private def genericByteStringFormatter[T <: Model[T] : FastTypeTag : SPickler : Unpickler]: ByteStringFormatter[T] =
    new ByteStringFormatter[T] {
      override def deserialize(bs: ByteString): T = JSONPickle(bs.utf8String).unpickle[T]

      override def serialize(data: T): ByteString = ByteString(data.pickle.value)
    }

  private def updateIndex(indexKey: Key, id: Id, remove: Boolean = false): Future[Long] =
    if (remove)
      client.srem(longKey(indexKey), id)
    else
      client.sadd(longKey(indexKey), id)

  private def updateDependency(parent: Id, id: Id, remove: Boolean = false): Future[Long] =
    if (remove)
      client.srem(longKey(childrenKey(parent)), id)
    else
      client.sadd(longKey(childrenKey(parent)), id)

  private def longKey(key: Key) = s"$applicationString:$key"

  private def childrenKey(parentId: Id): Key = s"$parentId:children"

  private def allKey(className: String): Key = s"$className:all"
}

object Redis extends Redis
