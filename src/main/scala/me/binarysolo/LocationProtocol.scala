package me.binarysolo.locations

import java.util.Date

object LocationProtocol {
  sealed trait LocationEvent

  case class LocationUpserted(location: Location) extends LocationEvent
  case class LocationDeleted(location: Location) extends LocationEvent

  case class LocationIndexed(location: Location) extends LocationEvent
  case class LocationDeindex(id: String) extends LocationEvent
}
