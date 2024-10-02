name := """test-spray-http-client-min"""

version := "1.0"

scalaVersion := "2.10.5"

val sprayV = "1.1.3"
val akkaV = "2.1.4"

// Change this to another test framework if you prefer
libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "2.2.4" % "test",
    "com.tracelytics.joboe" % "api" % "0.0.1-SNAPSHOT",
    "io.spray"            %  "spray-routing" % sprayV,
    "io.spray"            %  "spray-can"     % sprayV,
    "io.spray"            %  "spray-testkit" % sprayV  % "test",
    "io.spray"            %  "spray-caching" % sprayV,
    "io.spray"            %  "spray-client" % sprayV,
    "com.typesafe.akka"   %%  "akka-actor"    % akkaV,
    "com.typesafe.akka"   %%  "akka-testkit"  % akkaV   % "test",
    "org.specs2"          %%  "specs2-core"   % "2.3.11" % "test")

// Uncomment to use Akka
//libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.11"

resolvers += Resolver.mavenLocal