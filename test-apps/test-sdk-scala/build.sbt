name := """test-sdk-scala"""

version := "1.1-SNAPSHOT"

scalaVersion := "2.11.6"

resolvers += "Local Maven" at Path.userHome.asFile.toURI.toURL + ".m2/repository"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.11",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.11" % "test",
  "com.typesafe.akka" %% "akka-remote" % "2.3.11",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "com.appoptics.agent.java" % "appoptics-sdk" % "6.9.0",
  "com.typesafe.akka"          %%  "akka-stream-experimental" % "0.10",
  "com.typesafe.play"          %%  "play-json" % "2.4.0")
  
  
  
