val akkaVersion = "2.3.8"
val sprayV = "1.3.2"

resolvers += "krasserm at bintray" at "http://dl.bintray.com/krasserm/maven"

fork in console := true
fork in run := true

val project = Project(
  id = "locations",
  base = file("."),
  settings = Project.defaultSettings ++ Seq(
    name := "locations",
    version := "1.0",
    scalaVersion := "2.11.4",
    libraryDependencies ++= Seq(
      "com.typesafe.akka"        %% "akka-contrib" % akkaVersion,
      "com.github.michaelpisula" %% "akka-persistence-inmemory" % "0.2.1",
      "io.spray"                 %% "spray-can"     % sprayV,
      "io.spray"                 %% "spray-routing" % sprayV,
      "io.spray"                 %% "spray-json"    % "1.3.1",
      "org.scalatest"            %% "scalatest" % "2.1.6" % "test",
      "commons-io"               % "commons-io" % "2.4" % "test")
  )
)
