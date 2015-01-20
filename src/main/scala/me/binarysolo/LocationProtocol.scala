package me.binarysolo.locations

import java.util.Date

object LocationProtocol {
  sealed trait LocationEvent

  case class LocationUpserted(location: Location) extends LocationEvent
  case class LocationDeleted(id: String) extends LocationEvent
}
