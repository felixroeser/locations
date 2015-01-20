package me.binarysolo.locations

import akka.persistence.PersistentActor
import akka.actor.ActorLogging
import java.util.Date
import akka.persistence._
import akka.actor.Props

case class Address(country: String, state: Option[String], zipcode: String, city: String, street: String, lat: Option[Double], long: Option[Double], distance: Option[Double]) {
  // FIXME use lazy val
  def latLong = (lat, long) match {
    case (Some(lat), Some(long)) => Some(lat, long)
    case _ => None
  }
}

case class ImportLocation(id: Option[String], ownerId: String, address: Address)
case class Location(id: String, ownerId: String, address: Address)

object LocationsActor {
  import LocationProtocol._

  sealed trait Command
  case class UpsertLocation(location: Location) extends Command
  case class DeleteLocation(id: String) extends Command

  sealed trait Query
  case object QueryAll extends Query
  case class QueryId(id: String) extends Query
  case class QuerySearch(search: Search) extends Query

  def props = Props(new LocationsActor)

  case class LocationsState(locations: Map[String, Location] = Map.empty) {

    def updated(event: LocationEvent): LocationsState = event match {
      case LocationUpserted(location) => copy( locations + ( location.id -> location ) )
      case LocationDeleted(id) => copy( locations - id )
      case _ => this
    }
  }
}

class LocationsActor() extends PersistentActor with ActorLogging {
  import LocationsActor._
  import LocationProtocol._

  def persistenceId = "Locations"

  var state : LocationsState = new LocationsState

  def updateState(event: LocationEvent) = state = state.updated(event)

  val receiveRecover: Receive = {
    case event: LocationEvent => updateState(event)
    case _ => println("receiveRecover!")
  }

  val receiveCommand: Receive = {
    case UpsertLocation(location) => {
      persist(LocationUpserted(location)) {
        println(s"Upserted $location")
        event => updateState(event)
        sender ! event
      }
    }
    case DeleteLocation(id) => {
      persist(LocationDeleted(id)) {
        println(s"Deleted $id")
        event => updateState(event)
        sender ! event
      }
    }
  }
}


class LocationsView extends PersistentView {
  import LocationsActor._
  import LocationProtocol._

  override def persistenceId = "Locations"
  override def viewId = "Locations-view"

  var state : LocationsState = new LocationsState
  def updateState(event: LocationEvent) = state = state.updated(event)

  def receive = {
    case "snap" =>
      println(s"view saving snapshot")
      saveSnapshot(state)
    case SnapshotOffer(_, snapshot: LocationsState) => {
      state = snapshot
    }
    case SaveSnapshotSuccess(metadata) =>
      println(s"view saved snapshot (metadata = ${metadata})")
    case SaveSnapshotFailure(metadata, reason) =>
      println(s"view snapshot failure (metadata = ${metadata}), caused by ${reason}")
    case event if isPersistent => {
      updateState(event.asInstanceOf[LocationEvent])
      println(s"Got $event")
    }

    case QueryAll => sender ! state.locations.values.toList
    case QueryId(id) => {
      sender ! state.locations.get(id)
    }
    case QuerySearch(search) => {
      sender ! search.run(state.locations.values)
    }
  }
}
