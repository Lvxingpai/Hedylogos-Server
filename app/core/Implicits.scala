package core

import com.twitter.{ util => twitter }
import models.Message.{ ChatType, MessageType }

import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.language.implicitConversions
import scala.util.{ Failure, Success, Try }

/**
 * Created by zephyre on 7/10/15.
 */
object Implicits {
  implicit def messageType2Int(t: MessageType.Value): Int = t.id

  implicit def int2MessageType(v: Int): MessageType.Value = MessageType(v)

  implicit def chatType2String(t: ChatType.Value): String = t.toString

  implicit def string2ChatType(v: String): ChatType.Value = ChatType.withName(v)

  object TwitterConverter {
    implicit def scalaToTwitterTry[T](t: Try[T]): twitter.Try[T] = t match {
      case Success(r) => twitter.Return(r)
      case Failure(ex) => twitter.Throw(ex)
    }

    implicit def twitterToScalaTry[T](t: twitter.Try[T]): Try[T] = t match {
      case twitter.Return(r) => Success(r)
      case twitter.Throw(ex) => Failure(ex)
    }

    implicit def scalaToTwitterFuture[T](f: Future[T])(implicit ec: ExecutionContext): twitter.Future[T] = {
      val promise = twitter.Promise[T]()
      f.onComplete(promise update _)
      promise
    }

    implicit def twitterToScalaFuture[T](f: twitter.Future[T]): Future[T] = {
      val promise = Promise[T]()
      f.respond(promise complete _)
      promise.future
    }
  }

}
