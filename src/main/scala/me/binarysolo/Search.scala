package me.binarysolo.locations

import GeoTools._

case class Search(ownerId: Option[String], lat: Option[Double], long: Option[Double], maxDistance: Option[Double], limit: Option[Int]) {

  // FIXME pretty naive and slow
  def run(locations: Iterable[Location] ): Iterable[Location] = {
    val limitOrMax = limit.getOrElse( locations.size )

    val latLong = (lat, long) match {
      case (Some(lat), Some(long)) => Some(lat, long)
      case _ => None
    }

    locations.filter { location =>

      ownerId match {
        case Some(ownerId) => location.ownerId == ownerId
        case _ => true
      }

    }.map { location =>

      latLong match {
        case Some(latLong) => {
          val distance = distanceOrNone(Some(latLong), location.address.latLong)
          println(s"Dist: $distance")
          (distance, location)
        }
        case _ => (None, location)
      }

    }.filter { distanceAndLocation =>
      val maybeDistance = distanceAndLocation._1
      val location = distanceAndLocation._2

      (maxDistance, maybeDistance, location ) match {
        case (Some(maxDistance), Some(distance), location) => {
          distance < maxDistance
        }
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
