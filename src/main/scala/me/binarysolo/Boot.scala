package me.binarysolo.locations

import java.util.Date
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import akka.persistence.Update

object Boot extends App {
  import me.binarysolo.locations._
  import me.binarysolo.locations.LocationsActor._

  println("Booting up...")

  val system = ActorSystem("location-es")
  val locations = system.actorOf(LocationsActor.props, "locations")
  val locationsView = system.actorOf(Props(classOf[LocationsView]), "locations-view")
  import system.dispatcher
  implicit val timeout = Timeout(5 seconds)

  // seed some hardcoded locations
  val seedLocations = List(
    ("rl1", "r1", Address("de", "koeln", "50825", "iltisstr 1", Some(50.957566), Some(6.918909))),
    ("rl2", "r1", Address("de", "koeln", "50733", "neusser str 254", Some(50.963500), Some(6.953900))),
    ("rl3", "r1", Address("de", "koeln", "50668", "eigelstein 80", Some(50.948222), Some(6.956997) ))
  ).map { l =>
    Location(l._1, l._2, l._3)
  }.foreach { location =>
    locations ! UpsertLocation(location)
  }

  locationsView ! Update(await = true)

  // send some queries
  val future = locationsView ? QueryAll
  val results = Await.result(future, timeout.duration)
  println(s"<------ queryAll ------ $results")

  val queryIdFuture = locationsView ? QueryId("rl2")
  val queryIdResult = Await.result(queryIdFuture, timeout.duration)
  println(s"<------ queryId ------ $queryIdResult")

  val queryOwnerIdFuture = locationsView ? QueryOwnerId("r1")
  val queryOwnerIdResults = Await.result(queryOwnerIdFuture, timeout.duration)
  println(s"<------ queryOwnerId ------ $queryOwnerIdResults")

  //Thread.sleep(1000)
  system.shutdown()
}
