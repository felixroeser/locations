package me.binarysolo.locations

import akka.actor.ActorSystem
import akka.testkit._
import scala.concurrent.future
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FreeSpec
import org.scalatest.Matchers
import spray.http.StatusCodes._
import spray.testkit.ScalatestRouteTest
import com.typesafe.config.ConfigFactory

// http://doc.akka.io/docs/akka/snapshot/scala/testing.html
class LocationsApiSpec extends FreeSpec with LocationsApi with ScalatestRouteTest with Matchers {

  import LocationsActor._

  def actorRefFactory = system

  val probeC = TestProbe()
  val probeQ = TestProbe()

  override val someContext = new SomeContext {
    override val l =  probeC.ref  // TestActorRef[LocationsActor]
    override val lv = probeQ.ref // TestActorRef[LocationsView]
  }

  override val locations = someContext.l
  override val locationsView = someContext.lv

  "The LocationsAPI" - {
    ".well-known endpoints" - {
      "when requesting GET ping it should return pong" - {
        Get("/.well-known/ping") ~> apiRoute ~> check {
          status should equal(OK)
          entity.toString should include(""""ping": "pong"""")
        }
      }

      "when requesting GET ping it should return ok" - {
        Get("/.well-known/status") ~> apiRoute ~> check {
          status should equal(OK)
          entity.toString should include(""""status": "ok"""")
        }
      }
    }

    "commands" - {
      "when requesting DELETE locations/:id" - {
        "should return 202" in {
          Delete("/locations/123") ~> apiRoute ~> check {
            probeC.expectMsg( DeleteLocation("123") )
            status should equal(Accepted)
          }
        }
      }

      "when requesting DELETE locations/:id/databag/:k" - {
        "should return 202" in {
          Delete("/locations/123/databag/foo") ~> apiRoute ~> check {
            probeC.expectMsg( DeleteLocationDatabagItem("123", "foo") )
            status should equal(Accepted)
          }
        }
      }

    }

  }
}
