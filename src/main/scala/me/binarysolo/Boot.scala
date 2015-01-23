package me.binarysolo.locations

import java.util.Date
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import akka.persistence.Update
import akka.io.IO
import com.typesafe.config.ConfigFactory

import spray.can.Http

object Boot extends App {
  import me.binarysolo.locations._
  import me.binarysolo.locations.LocationsActor._

  // http://doc.akka.io/docs/akka/2.1.4/general/configuration.html
  // http://doc.akka.io/docs/akka/2.1.4/scala/extending-akka.html#extending-akka-scala-settings
  def configName = {
    if (sys.env.getOrElse("MONGOHQ_URL", sys.env.get("MONGO_URL")) != None) {
      "mongo"
    } else {
      "leveldb"
    }
  }
  val config = ConfigFactory.load(configName)

  implicit val system = ActorSystem("location-es", config)
  implicit val timeout = Timeout(5 seconds)

  val locations = system.actorOf(LocationsActor.props, "locations")
  val locationsView = system.actorOf(Props(classOf[LocationsView]), "locations-view")
  import system.dispatcher

  // setup and start the api
  val someContext = new SomeContext {
    override val l: ActorRef = locations
    override val lv: ActorRef = locationsView
  }

  // FIXME wait with api start until locationsView is ready / restored
  val port = sys.env.getOrElse("PORT", "9000").asInstanceOf[String].toInt
  val api = system.actorOf(Props( new LocationsApiActor(someContext)), "locations-api")
  IO(Http) ? Http.Bind(api, interface = "localhost", port = port)
}
