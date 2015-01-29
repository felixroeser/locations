package me.binarysolo.locations

import com.typesafe.config.ConfigFactory

object Settings {
  val port = sys.env.getOrElse("PORT", "9000").asInstanceOf[String].toInt
  val bind = sys.env.getOrElse("BIND", "0.0.0.0").asInstanceOf[String]
  val configName = {
    if (sys.env.getOrElse("MONGOHQ_URL", sys.env.get("MONGO_URL")) != None) {
      "mongo"
    } else {
      "leveldb"
    }
  }
  // http://doc.akka.io/docs/akka/2.1.4/general/configuration.html
  def config = ConfigFactory.load(configName)
}
