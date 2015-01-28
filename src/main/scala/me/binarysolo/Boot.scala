package me.binarysolo.locations

import java.util.Date
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import akka.persistence.Update
import akka.io.IO
import akka.event.Logging
import com.typesafe.config.ConfigFactory

import spray.can.Http

object Settings {

  def configName = {
    if (sys.env.getOrElse("MONGOHQ_URL", sys.env.get("MONGO_URL")) != None) {
      "mongo"
    } else {
      "leveldb"
    }
  }

  val port = sys.env.getOrElse("PORT", "9000").asInstanceOf[String].toInt
  val bind = sys.env.getOrElse("BIND", "0.0.0.0").asInstanceOf[String]
}

object Boot extends App {
  import me.binarysolo.locations.LocationsActor._

  // http://doc.akka.io/docs/akka/2.1.4/general/configuration.html
  // http://doc.akka.io/docs/akka/2.1.4/scala/extending-akka.html#extending-akka-scala-settings
  val config = ConfigFactory.load(Settings.configName)

  implicit val system = ActorSystem("location-es", config)
  implicit val timeout = Timeout(5 seconds)
  val log = Logging(system, getClass)
  log.info("Booting...")

  val locations = system.actorOf(LocationsActor.props, "locations")
  val locationsView = system.actorOf(Props(classOf[LocationsView]), "locations-view")
  import system.dispatcher

  // setup and start the api
  val someContext = new SomeContext {
    override val l: ActorRef = locations
    override val lv: ActorRef = locationsView
  }

  // FIXME wait with api start until locationsView is ready / restored
  val api = system.actorOf(Props( new LocationsApiActor(someContext)), "locations-api")
  IO(Http) ? Http.Bind(api, interface = Settings.bind, port = Settings.port)
}
