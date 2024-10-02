name := """test-play-scala-2.5"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  specs2 % Test,
  "com.appoptics.agent.java" % "appoptics-sdk" % "6.0.0"
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

resolvers += Resolver.mavenLocal

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

//javaOptions in run += "-agentlib:TakipiAgent -Dtakipi.silent=false"
//javaOptions in run += "-javaagent:C:/Progra~1/AppNeta/TraceView/java/tracelyticsagent.jar"
