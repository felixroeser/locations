fork in console := true
fork in run := true

resolvers ++= Seq(
  Classpaths.typesafeResolver,
  "krasserm at bintray" at "http://dl.bintray.com/krasserm/maven",
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/releases",
  "spray repo" at "http://repo.spray.io"
)

val akkaVersion = "2.3.8"
val sprayV = "1.3.2"

name := "locations"
version := "1.0"
scalaVersion := "2.11.4"
libraryDependencies ++= Seq(
  "com.typesafe.akka"        %% "akka-contrib" % akkaVersion,
  "com.github.michaelpisula" %% "akka-persistence-inmemory" % "0.2.1",
  "com.github.ironfish"      %% "akka-persistence-mongo-casbah"  % "0.7.5" % "compile",
  "io.spray"                 %% "spray-can"     % sprayV,
  "io.spray"                 %% "spray-routing" % sprayV,
  "io.spray"                 %% "spray-json"    % "1.3.1", // there is no 1.3.2 yet
  "io.spray"                 %% "spray-testkit" % sprayV % "test",
  "org.scalatest"            %% "scalatest" % "2.1.6" % "test",
  "commons-io"               %  "commons-io" % "2.4" % "test")

Revolver.settings
enablePlugins(JavaAppPackaging)
