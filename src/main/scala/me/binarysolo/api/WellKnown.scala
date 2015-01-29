package me.binarysolo.locations.api

import akka.actor.ActorRef
import spray.routing._
import spray.http.StatusCodes._
import spray.json._
import spray.httpx.SprayJsonSupport._
import DefaultJsonProtocol._

trait WellKnown extends HttpService {
  implicit val locationsView: ActorRef

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
}
