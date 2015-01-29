package me.binarysolo.locations.api

import scala.concurrent.duration._

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout

import spray.routing._
import spray.http.StatusCodes._
import spray.json._
import spray.httpx.SprayJsonSupport._
import DefaultJsonProtocol._
import spray.http._
import spray.http.MediaTypes._

import me.binarysolo.locations._
import LocationsJsonProtocol._
import SearchJsonProtocol._
import LocationsActor._

trait Q extends HttpService {
  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val locationsView: ActorRef
  implicit val ApiType: MediaType
  implicit val timeout: Timeout

  def qRoutesGet = {
    get {
      pathPrefix("locations") {
        respondWithMediaType(ApiType) {
          pathEndOrSingleSlash {
            complete {
              for {
                results <- locationsView ? QueryAll
              } yield (OK, results.asInstanceOf[List[Location]])
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

  def qRoutesPost = {
    post {
      path("locations" / "search") {
        respondWithMediaType(ApiType) {
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

  val queryRoutes = qRoutesGet ~ qRoutesPost
}
