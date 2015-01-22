package me.binarysolo.locations

import java.util.Date
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import akka.persistence.Update
import akka.io.IO

import spray.can.Http

object Boot extends App {
  import me.binarysolo.locations._
  import me.binarysolo.locations.LocationsActor._

  println("Booting up...")

  implicit val system = ActorSystem("location-es")
  implicit val timeout = Timeout(5 seconds)

  val locations = system.actorOf(LocationsActor.props, "locations")
  val locationsView = system.actorOf(Props(classOf[LocationsView]), "locations-view")
  import system.dispatcher

  // setup and start the api
  val someContext = new SomeContext {
    override val l: ActorRef = locations
    override val lv: ActorRef = locationsView
  }

  val api = system.actorOf(Props( new LocationsApiActor(someContext)), "locations-api")
  IO(Http) ? Http.Bind(api, interface = "localhost", port = 9000)
}
