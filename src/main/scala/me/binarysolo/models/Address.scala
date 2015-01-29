package me.binarysolo.locations

case class Address(country: String, state: Option[String] = None, zipcode: String, city: String, street: String, lat: Option[Double], long: Option[Double], distance: Option[Double] = None) {
  // FIXME use lazy val
  def latLong = (lat, long) match {
    case (Some(lat), Some(long)) => Some(lat, long)
    case _ => None
  }
}
