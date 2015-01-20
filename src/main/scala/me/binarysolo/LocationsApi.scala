package me.binarysolo.locations

import scala.concurrent.duration._

import akka.actor._
import akka.util.Timeout
import akka.pattern.ask

import spray.can.Http
import spray.routing._
import spray.http._
import spray.json._
import spray.httpx.SprayJsonSupport._
import DefaultJsonProtocol._
import MediaTypes._
import StatusCodes._

import LocationsJsonProtocol._

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
      pathPrefix("api") {
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
          } ~
          path("owner" / Segment) { (ownerId) =>
            respondWithMediaType(`application/json`) {
              complete {
                for {
                  results <- locationsView ? QueryOwnerId(ownerId)
                } yield {
                  (OK, results.asInstanceOf[List[Location]].toJson.prettyPrint)
                }
              }
            }
          }
        }
      }
    } ~
    post {
      pathPrefix("api") {
        path("locations") {
          entity(as[Location]) { location =>
            locations ! UpsertLocation(location)
            complete { (Accepted, "will do that") }
          }
        }
      }
    }
  }
}
