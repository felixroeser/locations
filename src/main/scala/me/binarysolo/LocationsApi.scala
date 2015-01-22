package me.binarysolo.locations

import scala.concurrent.duration._

import akka.actor._
import akka.util.Timeout
import akka.pattern.ask
import java.util.UUID

import spray.can.Http
import spray.routing._
import spray.http._
import spray.json._
import spray.httpx.SprayJsonSupport._
import DefaultJsonProtocol._
import MediaTypes._
import StatusCodes._

import me.binarysolo.locations._
import LocationsJsonProtocol._
import SearchJsonProtocol._

trait SomeContext {
  val l: ActorRef
  val lv: ActorRef
}

class LocationsApiActor(c: SomeContext) extends Actor with LocationsApi {
  implicit def actorRefFactory = context
  def receive = runRoute(myRoute)

  override val someContext = c
  override val locations = someContext.l
  override val locationsView = someContext.lv
}

trait LocationsApi extends HttpService { this: LocationsApiActor =>
  import LocationsActor._

  val someContext: SomeContext
  val locations: ActorRef
  val locationsView: ActorRef

  implicit def executionContext = actorRefFactory.dispatcher
  implicit val system = context.system
  implicit val timeout: Timeout = Timeout(30.seconds)

  val myRoute = {
    import spray.json.DefaultJsonProtocol._
    get {
      pathSingleSlash { complete("welcome stanger!") } ~
      pathPrefix(".well-known") {
        path("ping") { complete { (OK, "PONG") } } ~
        path("snap") {
          complete {
            locationsView ! "snap"
            (Accepted, "will do that")
          }
        }
      } ~
      pathPrefix("v0") {
        pathPrefix("locations") {
          pathSingleSlash {
            respondWithMediaType(`application/json`) {
              complete {
                for {
                  results <- locationsView ? QueryAll
                } yield {
                  (OK, results.asInstanceOf[List[Location]].toJson.prettyPrint)
                }
              }
            }
          } ~
          path(Segment) { (id) =>
            respondWithMediaType(`application/json`) {
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
    } ~
    post {
      pathPrefix("v0") {
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
            respondWithMediaType(`application/json`) {
              complete { (Accepted, location.toJson.prettyPrint) }
            }
          }
        } ~
        path("locations" / Segment / "databag") { (id) =>
          entity(as[Map[String, String]]) { databag =>

            locations ! UpsertLocationDatabag(id, databag)
            complete {
              (Accepted, "will do that")
            }
          }
        } ~
        path("locations" / "search") {
          entity(as[Search]) { search =>
            respondWithMediaType(`application/json`) {
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
    } ~
    delete {
      pathPrefix("v0") {
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
}
