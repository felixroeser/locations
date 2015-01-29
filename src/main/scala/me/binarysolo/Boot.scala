package me.binarysolo.locations

import scala.concurrent.duration._
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import akka.io.IO
import akka.event.Logging

import spray.can.Http

object Boot extends App {
  import me.binarysolo.locations.LocationsActor._

  implicit val system = ActorSystem("location-es", Settings.config)
  implicit val timeout = Timeout(5 seconds)

  val log = Logging(system, getClass)
  log.info("Booting up locations...")

  // actor setup
  val locations = system.actorOf(LocationsActor.props, "locations")
  val locationsView = system.actorOf(Props(classOf[LocationsView]), "locations-view")
  import system.dispatcher

  val someContext = new SomeContext {
    override val l: ActorRef = locations
    override val lv: ActorRef = locationsView
  }

  // api setup
  // FIXME wait with api start until locationsView is ready / restored
  val api = system.actorOf(Props(new ApiActor(someContext)), "locations-api")
  IO(Http) ? Http.Bind(api, interface = Settings.bind, port = Settings.port)
}
