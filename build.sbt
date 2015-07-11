import com.typesafe.sbt.SbtAspectj.Aspectj
import com.typesafe.sbt.SbtAspectj.AspectjKeys._

name := """Hedylogos"""

organization := "com.lvxingpai"

version := "1.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala, ScroogeSBT)

scalaVersion := "2.10.4"

val finagleVersion = "6.25.0"

val springVersion = "3.2.2.RELEASE"

libraryDependencies ++= Seq(
  "org.mongodb" % "mongo-java-driver" % "3.0.0",
  "org.mongodb.morphia" % "morphia" % "0.111",
  "org.mongodb.morphia" % "morphia-validation" % "0.111",
  "com.google.protobuf" % "protobuf-java" % "2.6.1",
//  "org.codehaus.jackson" % "jackson-mapper-asl" % "1.9.13",
//  "org.codehaus.jackson" % "jackson-core-asl" % "1.9.13",
  "commons-logging" % "commons-logging" % "1.2",
  "commons-httpclient" % "commons-httpclient" % "3.1",
  "commons-codec" % "commons-codec" % "1.10",
  "commons-collections" % "commons-collections" % "3.2.1",
  "net.debasishg" %% "redisclient" % "2.15",
  "com.qiniu" % "qiniu-java-sdk" % "7.0.0",
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.2",
  "com.lvxingpai" %% "appconfig" % "0.2.1-SNAPSHOT",
  "org.apache.thrift" % "libthrift" % "0.9.2",
  "com.twitter" %% "scrooge-core" % "3.18.1",
  "com.twitter" %% "finagle-thrift" % finagleVersion,
  "com.twitter" %% "finagle-core" % finagleVersion,
  "com.twitter" %% "finagle-thrift" % finagleVersion,
  "com.twitter" %% "finagle-thriftmux" % finagleVersion,
  "org.springframework" % "spring-aspects" % springVersion,
  "org.springframework" % "spring-aop" % springVersion,
  "org.springframework" % "spring-tx" % springVersion,
  "javax.persistence" % "persistence-api" % "1.0.2",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  jdbc,
  anorm,
  cache,
  ws
)

publishTo := {
  val nexus = "http://nexus.lvxingpai.com/content/repositories/"
  if (isSnapshot.value)
    Some("publishSnapshots" at nexus + "snapshots")
  else
    Some("publishReleases" at nexus + "releases")
}

scalariformSettings

// AspectJ settings start

aspectjSettings

AspectjKeys.showWeaveInfo in Aspectj := false


inputs in Aspectj <+= compiledClasses

binaries in Aspectj <++= update map { report =>
  report.matching(
    moduleFilter(organization = "org.springframework", name = "spring-aspects")
  )
}

binaries in Aspectj <++= update map { report =>
  report.matching(
    moduleFilter(organization = "org.springframework", name = "spring-aop")
  )
}

binaries in Aspectj <++= update map { report =>
  report.matching(
    moduleFilter(organization = "org.springframework", name = "spring-tx")
  )
}

products in Compile <<= products in Aspectj

products in Runtime <<= products in Compile

// AspectJ settings end