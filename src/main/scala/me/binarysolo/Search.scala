package me.binarysolo.locations

import misc._

case class Search(ownerId: Option[String], lat: Option[Double], long: Option[Double], maxDistance: Option[Double], limit: Option[Int])

object Search {
  // FIXME pretty naive and slow
  def run(search: Search, locations: Iterable[Location] ): Iterable[Location] = {
    val limitOrMax = search.limit.getOrElse( locations.size )
    val latLong = (search.lat, search.long) match {
      case (Some(lat), Some(long)) => Some(lat, long)
      case _ => None
    }

    locations.filter { location =>
      search.ownerId match {
        case Some(ownerId) => location.ownerId == ownerId
        case _ => true
      }
    }.map { location =>
      latLong match {
        case Some(latLong) => {
          (GeoTools.distanceOrNone(Some(latLong), location.address.latLong), location)
        }
        case _ => (None, location)
      }
    }.filter { distanceAndLocation =>
      (search.maxDistance, distanceAndLocation._1, distanceAndLocation._2 ) match {
        case (Some(maxDistance), Some(distance), location) => distance < maxDistance
        case (Some(maxDistance), None, _) => false
        case _ => true
      }
    }.toList.sortBy{ _._1 }.map{ distanceAndLocation =>
      (distanceAndLocation._1, distanceAndLocation._2) match {
        case (Some(distance), location) => location.copy(address = location.address.copy(distance = Some(distance)) )
        case (None, location) => location
      }
    }.take(limitOrMax)
  }

}
