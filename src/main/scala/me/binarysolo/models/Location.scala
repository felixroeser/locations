package me.binarysolo.locations

case class ImportLocation(id: Option[String], ownerId: String, address: Address, databag: Option[Map[String, String]] = None)
case class Location(id: String, ownerId: String, address: Address, databag: Option[Map[String, String]] = None)
