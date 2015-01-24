package me.binarysolo.locations

import akka.testkit.TestKit
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.scalatest.Matchers
import akka.testkit.ImplicitSender
import org.scalatest.BeforeAndAfterAll
import org.scalatest.WordSpecLike
import java.util.Date

class LocationsActorSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {
  def this() = this(
    ActorSystem("TestActorSystem", ConfigFactory.parseString(
      """
      |akka.loglevel = "DEBUG"
      |akka.persistence.journal.plugin = "in-memory-journal"
      |akka.actor.debug {
        |   receive = on
        |   autoreceive = on
        |   lifecycle = on
        |}
        """.stripMargin)))

        override def afterAll() {
          TestKit.shutdownActorSystem(system)
        }

        import LocationsActor._
        import LocationProtocol._

        //"country":"de", "long":6.952221, "street":"Leonhard-Tietz-Strae 1", "lat":50.934009}*/

        //case class Address(country: String, state: Option[String] = None, zipcode: String, city: String, street: String, lat: Option[Double], long: Option[Double], distance: Option[Double] = None) {
        //case class Location(id: String, ownerId: String, address: Address, databag: Option[Map[String, String]] = None)

        val address1 = Address("de", None, "50676", "Cologne", "Leonhard-Tietz-Strasse 1", Some(50.934009), Some(6.952221) )
        val location1 = Location("l1", "me", address1, Some(Map("foo" -> "bar")))
        val upsertLocationCmd = UpsertLocation(location1)

        s"receiving '$upsertLocationCmd'" should {
          s"return the location" in {
            val locationsActor = _system.actorOf(LocationsActor.props)
            locationsActor ! upsertLocationCmd
            expectMsg(LocationUpserted(location1))
          }
        }


      }
