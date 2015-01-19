package me.binarysolo.locations

import java.util.Date
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import akka.actor.ActorSystem
import akka.actor.actorRef2Scala
import akka.pattern.ask
import akka.util.Timeout
import akka.persistence.Update

object Boot extends App {
  import me.binarysolo.locations._
  import me.binarysolo.locations.LocationsActor._

  println("Booting up...")

  val system = ActorSystem("location-es")
  val locations = system.actorOf(LocationsActor.props, "locations")

  val seedLocations = List(
    ("rl1", "r1", Address("de", "koeln", "50825", "iltisstr 1", Some(50.957566), Some(6.918909))),
    ("rl2", "r1", Address("de", "koeln", "50733", "neusser str 254", Some(50.963500), Some(6.953900)))
  ).map { l =>
    Location(l._1, l._2, l._3)
  }.foreach { location =>
    locations ! UpsertLocation(location)
  }

  implicit val timeout = Timeout(5 seconds)

  val future = locations ? QueryAll
  val results = Await.result(future, timeout.duration)
  println(s"<------ lookup ------ $results")

  val queryIdFuture = locations ? QueryId("rl2")
  val queryIdResult = Await.result(queryIdFuture, timeout.duration)
  println(s"<------ queryid ------ $queryIdResult")

  val queryOwnerIdFuture = locations ? QueryOwnerId("r1")
  val queryOwnerIdResults = Await.result(queryOwnerIdFuture, timeout.duration)
  println(s"<------ queryid ------ $queryOwnerIdResults")

  //Thread.sleep(1000)
  system.shutdown()
}
