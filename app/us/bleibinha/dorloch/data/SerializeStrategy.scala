package us.bleibinha.dorloch.data

import us.bleibinha.dorloch.model._

trait SerializeStrategy[T <: Model[T]] {
  def indexUpdates(obj: T): List[(Key, Boolean)] = Nil

  def removalIndices(obj: T): List[(Key, Option[Id])] =
    indexUpdates(obj) map {
      case (key, _) => (key, Some(obj.id.get))
    }

  def parents(obj: T): List[Id] = Nil

  def preSerialize(obj: T, redis: Redis) = obj

  def postDeserialize(obj: T, redis: Redis) = obj
}




