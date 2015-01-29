package me.binarysolo.locations

import scala.concurrent.duration._

import akka.actor._
import akka.util.Timeout
import akka.pattern.ask
import akka.event.Logging
import akka.event.Logging._
import akka.actor.ActorLogging
import spray.routing._
import spray.http._
import MediaTypes._
import StatusCodes._

import LocationsJsonProtocol._
import SearchJsonProtocol._
import api._

class ApiActor(someContext: SomeContext) extends Actor with Api {
  implicit def actorRefFactory = context
  override val locations = someContext.l
  override val locationsView = someContext.lv

  def receive = runRoute(apiRoute)
}

trait Api extends HttpService with WellKnown with C with Q {
  import LocationsActor._

  val locations: ActorRef
  val locationsView: ActorRef

  implicit def executionContext = actorRefFactory.dispatcher
  implicit val timeout: Timeout = Timeout(30.seconds)

  // http://boldradius.com/blog-post/VAXI9i4AADMA4lXn/using-http-header-to-version-a-restful-api-in-spray
  // http://stackoverflow.com/questions/19131488/spray-routing-how-to-match-specific-accept-headers
  val ApiType = register(
    MediaType.custom(
      mainType = "application",
      subType = "vnd.locations.v1+json",
      compressible = true))

  val apiRoute = cRoutesPost ~ cRoutesDelete ~ qRoutesPost ~ qRoutesGet ~ wellKnownRoutes
}
