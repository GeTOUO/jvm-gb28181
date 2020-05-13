package com.getouo.gb.scl.sip

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.LoggerOps
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }

object SipHello {

  final case class Greet(info: String, replyTo: ActorRef[Pong])
  final case class Pong(info: String, replyTo: ActorRef[Greet])

  def apply(): Behavior[Greet] = Behaviors.receive { (context, message) =>
    context.log.info("Hello {}!", message.info)
    message.replyTo ! Pong(message.info, context.self)
    Behaviors.same
  }
}
