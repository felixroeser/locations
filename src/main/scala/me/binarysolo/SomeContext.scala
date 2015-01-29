package me.binarysolo.locations

import akka.actor.ActorRef

trait SomeContext {
  val l: ActorRef
  val lv: ActorRef
}
