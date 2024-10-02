name := """play-scala-2.3"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "com.appoptics.agent.java" % "appoptics-sdk" % "6.0.0"
  ,"com.typesafe.akka" %% "akka-actor" % "2.3.13"
  //,"com.typesafe.akka" %% "akka-remote" % "2.3.13"
  ,"com.typesafe.akka" %% "akka-stream-experimental" % "1.0"
  ,"org.slf4j"          % "slf4j-api"          % "1.7.+"
  ,"com.typesafe.akka"   %%  "akka-actor"    % "2.4.1"
  ,"io.kamon" %% "kamon-core" % "0.4.0"
  ,"io.kamon" %% "kamon-scala" % "0.4.0"
  ,"io.kamon" %% "kamon-play" % "0.4.0"
  ,"io.kamon" %% "kamon-akka" % "0.4.0"
  ,"io.kamon" %% "kamon-log-reporter" % "0.4.0"
)

resolvers += Resolver.mavenLocal