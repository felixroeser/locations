package me.binarysolo.locations

import me.binarysolo.locations._
import spray.json._
import DefaultJsonProtocol._

object SearchJsonProtocol extends DefaultJsonProtocol {
  implicit val searchFormat = jsonFormat5(Search.apply)
}
