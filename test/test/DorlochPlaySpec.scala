package test

import org.scalatestplus.play.PlaySpec

import scala.concurrent._
import scala.concurrent.duration._

abstract class DorlochPlaySpec extends PlaySpec {
  protected def awaitFuture[T](future: Future[T]) = Await.result(future, 2 seconds)
}
