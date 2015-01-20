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

  // seed some hardcoded locations
  // these will be loaded from the event journal / snapshots anyways if started again
  // delete target/example for a fresh start
  val seedLocations = List(
    ("rl1", "r1", Address("de", None, "koeln", "50825", "launplatz 1", Some(50.957566), Some(6.918909))),
    ("rl2", "r1", Address("de", None, "koeln", "50733", "neusser str 254", Some(50.963500), Some(6.953900))),
    ("rl3", "r1", Address("de", None, "koeln", "50668", "eigelstein 80", Some(50.948222), Some(6.956997))),
    ("rl_to_be_deleted", "r1", Address("de", None, "koeln", "50668", "eigelstein 80", Some(50.948222), Some(6.956997)))
  ).map { l =>
    Location(l._1, l._2, l._3, None)
  }.foreach { location =>
    println(s"Posting $location")
    locations ! UpsertLocation(location)
  }

  locations ! DeleteLocation("rl_to_be_deleted")

  locationsView ! Update(await = true)

  // send some queries
  val future = locationsView ? QueryAll
  val results = Await.result(future, timeout.duration)
  println(s"<------ queryAll ------ $results")

  val queryIdFuture = locationsView ? QueryId("rl2")
  val queryIdResult = Await.result(queryIdFuture, timeout.duration)
  println(s"<------ queryId ------ $queryIdResult")

  // a rather lame way to stop the api...
  Thread.sleep(125000)
  system.shutdown()
}
