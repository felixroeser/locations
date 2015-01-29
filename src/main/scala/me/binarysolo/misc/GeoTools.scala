package me.binarysolo.locations.misc

object GeoTools {

  // TODO Selecting points within a bounding circle
  // see http://www.movable-type.co.uk/scripts/latlong-db.html

  def distanceOrNone(pointA: Option[(Double, Double)], pointB: Option[(Double, Double)] ): Option[Double] = {
    (pointA, pointB) match {
      case (Some(pointA), Some(pointB)) => Some(haversineDistance(pointA, pointB))
      case _ => None
    }
  }

  // source http://blog.davidkeen.com/2013/10/calculating-distance-with-scalas-foldleft/
  def haversineDistance(pointA: (Double, Double), pointB: (Double, Double)): Double = {
    val deltaLat = math.toRadians(pointB._1 - pointA._1)
    val deltaLong = math.toRadians(pointB._2 - pointA._2)
    val a = math.pow(math.sin(deltaLat / 2), 2) + math.cos(math.toRadians(pointA._1)) * math.cos(math.toRadians(pointB._1)) * math.pow(math.sin(deltaLong / 2), 2)
    val greatCircleDistance = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    6378.137 * greatCircleDistance // miles: 3958.761
  }

}
