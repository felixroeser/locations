package me.binarysolo.locations

import scala.concurrent.duration._

import akka.actor._
import akka.util.Timeout
import akka.pattern.ask
import akka.event.Logging
import akka.event.Logging._
import akka.actor.ActorLogging
import java.util.UUID
import spray.can.Http
import spray.routing._
import spray.http._
import spray.json._
import spray.httpx.SprayJsonSupport._
import spray.routing.directives.{ DirectoryListing, LogEntry }
import DefaultJsonProtocol._
import MediaTypes._
import StatusCodes._

import me.binarysolo.locations._
import LocationsJsonProtocol._
import SearchJsonProtocol._

class LocationsApiActor(c: SomeContext) extends Actor with LocationsApi {
  implicit def actorRefFactory = context
  def receive = runRoute(apiRoute)

  override val someContext = c
  override val locations = someContext.l
  override val locationsView = someContext.lv
}

trait LocationsApi extends HttpService {
  import LocationsActor._

  val someContext: SomeContext
  val locations: ActorRef
  val locationsView: ActorRef

  implicit def executionContext = actorRefFactory.dispatcher
  implicit val timeout: Timeout = Timeout(30.seconds)

  // http://boldradius.com/blog-post/VAXI9i4AADMA4lXn/using-http-header-to-version-a-restful-api-in-spray
  // http://stackoverflow.com/questions/19131488/spray-routing-how-to-match-specific-accept-headers
  val LocationsApiType = register(
    MediaType.custom(
      mainType = "application",
      subType = "vnd.locations.v1+json",
      compressible = true))

  def wellKnownRoutes = {
    import spray.json.DefaultJsonProtocol._

    get {
      pathSingleSlash { complete("welcome stanger!") } ~
      pathPrefix(".well-known") {
        path("status") {
          complete { (OK, Map("status" -> "ok")) }
        } ~
        path("ping") {
          complete { (OK, Map("ping" ->"pong")) }
        } ~
        // FIXME this one doesn't belong here
        path("snap") {
          complete {
            locationsView ! "snap"
            (Accepted, "will do that")
          }
        }
      }
    }
  }


  def queryRoutesGet = {
    import spray.json.DefaultJsonProtocol._

    get {
      pathPrefix("locations") {
        respondWithMediaType(LocationsApiType) {

          pathEndOrSingleSlash {

            complete {
              for {
                results <- locationsView ? QueryAll
              } yield {
                (OK, results.asInstanceOf[List[Location]])
              }
            }
          } ~
          path(Segment) { (id) =>
            complete {
              for {
                result <- locationsView ? QueryId(id)
              } yield {
                result match {
                  case Some(l: Location) => (OK, l.toJson.prettyPrint)
                  case None => (NotFound, s"$id doesnt exist!")
                }
              }
            }
          }
        }
      }
    }
  }

  def queryRoutesPost = {
    post {
      path("locations" / "search") {
        respondWithMediaType(LocationsApiType) {
          entity(as[Search]) { search =>
            complete {
              println(s"GOT search: $search")
              // FIXME return a GET location and not the result itself
              for {
                results <- locationsView ? QuerySearch(search)
              } yield {
                (OK, results.asInstanceOf[List[Location]].toJson.prettyPrint)
              }
            }
          }
        }
      }
    }
  }

  def commandRoutesPost = {
    import spray.json.DefaultJsonProtocol._
    respondWithMediaType(LocationsApiType) {
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


  def commandRoutesDelete = {
    respondWithMediaType(LocationsApiType) {
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

  val apiRoute = queryRoutesGet ~ queryRoutesPost ~ wellKnownRoutes  ~ commandRoutesPost ~ commandRoutesDelete
}
