name := """Hedylogos"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  "org.mongodb" % "mongo-java-driver" % "3.0.0",
  "org.mongodb.morphia" % "morphia" % "0.111",
  "org.mongodb.morphia" % "morphia-validation" % "0.111",
  "com.google.protobuf" % "protobuf-java" % "2.6.1",
  "org.codehaus.jackson" % "jackson-mapper-asl" % "1.9.13",
  "org.codehaus.jackson" % "jackson-core-asl" % "1.9.13",
  "commons-logging" % "commons-logging" % "1.2",
  "commons-httpclient" % "commons-httpclient" % "3.1",
  "commons-codec" % "commons-codec" % "1.10",
  "commons-collections" % "commons-collections" % "3.2.1",
  "net.debasishg" %% "redisclient" % "2.15",
  jdbc,
  anorm,
  cache,
  ws
)
