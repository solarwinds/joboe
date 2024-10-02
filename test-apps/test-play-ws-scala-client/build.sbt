name := """test-play-ws-scala-client"""

version := "1.0"

scalaVersion := "2.11.7"

// Change this to another test framework if you prefer
libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % "test"

// Uncomment to use Akka
//libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.11"

// https://mvnrepository.com/artifact/com.typesafe.play/play-ws_2.11
libraryDependencies += "com.typesafe.play" % "play-ws_2.11" % "2.5.4"

// https://mvnrepository.com/artifact/com.appneta.agent.java/appneta-api
libraryDependencies += "com.appoptics.agent.java" % "appoptics-sdk" % "6.0.0"

