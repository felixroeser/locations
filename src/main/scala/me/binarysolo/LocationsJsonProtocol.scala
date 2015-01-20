package me.binarysolo.locations

import spray.json._
import DefaultJsonProtocol._

object LocationsJsonProtocol extends DefaultJsonProtocol {

  implicit val addressFormat = jsonFormat8(Address.apply)
  implicit val ImportLocationFormat = jsonFormat3(ImportLocation.apply)
  implicit val locationFormat = jsonFormat3(Location.apply)
  implicit object locationsFormat extends RootJsonWriter[List[Location]] {
    def write(list: List[Location]) = JsArray(list.map(_.toJson).toVector)
  }

}
