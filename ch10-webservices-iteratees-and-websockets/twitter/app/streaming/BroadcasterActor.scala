package streaming

import akka.actor.{ Actor, actorRef2Scala }
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.{ Concurrent, Enumerator, Iteratee }
import play.api.libs.json.JsValue

case object Subscribe
case class Unsubscribe(enumerator: Enumerator[JsValue])
case class Broadcast(message: JsValue)

class BroadcasterActor extends Actor {

  // Prefer a var with immutable object over a val with mutable object.
  // We can safely send a reference to this list out of our actor, for example
  // in a response to a 'report' message, since the list itself is immutable.
  var subscriberCount = 0
  val (enumerator, channel) = Concurrent.broadcast[JsValue]

  def receive = {
    case Subscribe => {
      val iteratee = Iteratee.foreach[JsValue](_ => ()).map(_ =>
        self ! Unsubscribe(enumerator))
      subscriberCount = subscriberCount + 1
      Logger.info("Someone subscribed. We now have %s subscribers." format subscriberCount)
      sender ! (iteratee, enumerator)
    }
    case Unsubscribe(subscriber) => {
      subscriberCount = subscriberCount - 1
      Logger.info("Someone unsubscribed. We now have %s subscribers." format subscriberCount)
    }
    case Broadcast(message) => channel.push(message)
  }

}
