name := """play-java-2.3"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
  javaWs,
  "com.appoptics.agent.java" % "appoptics-sdk" % "6.0.0"
)

resolvers += Resolver.mavenLocal