name := """test-play-scala-2.7"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)
//lazy val root = (project in file(".")).enablePlugins(PlayScala, PlayNettyServer).disablePlugins(PlayAkkaHttpServer)

scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  jdbc,
  guice,
  ws,
  specs2 % Test,
  "com.h2database" % "h2" % "1.4.193",
  "com.appneta.agent.java" % "appneta-api" % "5.0.6"
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

resolvers += Resolver.mavenLocal

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

//javaOptions in run += "-agentlib:TakipiAgent -Dtakipi.silent=false"
//javaOptions in run += "-javaagent:C:/Progra~1/AppNeta/TraceView/java/tracelyticsagent.jar"
