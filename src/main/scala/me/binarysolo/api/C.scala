package me.binarysolo.locations.api

import scala.concurrent.duration._

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout

import spray.routing._
import spray.http.StatusCodes._
import spray.json._
import spray.httpx.SprayJsonSupport._
import spray.http._
import spray.http.MediaTypes._

import me.binarysolo.locations._
import LocationsJsonProtocol._
import LocationsActor._

trait C extends HttpService {
  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val locations: ActorRef
  implicit val ApiType: MediaType
  implicit val timeout: Timeout

  def cRoutesPost = {
    import spray.json.DefaultJsonProtocol._
    respondWithMediaType(ApiType) {
      post {
        path("locations") {
          entity(as[ImportLocation]) { importLocation =>
            // ensure that id is being set or generate a uuid
            // FIXME find a nicer way to copy from one case class to a another
            val location = Location(
              id = importLocation.id.getOrElse( java.util.UUID.randomUUID().toString ),
              ownerId = importLocation.ownerId,
              address = importLocation.address,
              databag = importLocation.databag
            )

            locations ! UpsertLocation(location)
            complete { (Accepted, location.toJson.prettyPrint) }
          }
        } ~
        path("locations" / Segment / "databag") { (id) =>
          entity(as[Map[String, String]]) { databag =>

            locations ! UpsertLocationDatabag(id, databag)
            complete {
              (Accepted, "will do that")
            }
          }
        }
      }
    }
  }

  def cRoutesDelete = {
    respondWithMediaType(ApiType) {
      delete {
        path("locations" / Segment) { (id) =>
          locations ! DeleteLocation(id)
          complete { (Accepted, "will do that") }
        } ~
        path("locations" / Segment / "databag" / Segment) { (id, k) =>
          locations ! DeleteLocationDatabagItem(id, k)
          complete { (Accepted, "will do that") }
        }
      }
    }
  }

  val commandRoutes = cRoutesPost ~ cRoutesDelete
}
