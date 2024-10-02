name := """test-akka-http-java-min"""

version := "1.0"

scalaVersion := "2.12.0"

libraryDependencies ++= {
  val akkaHttpV      = "10.0.0"
  val akkaStreamV    = "2.4.14"
  Seq(
    "com.typesafe.akka"  %% "akka-stream"             % akkaStreamV,
    "com.typesafe.akka"  %% "akka-http"          % akkaHttpV,
    "org.slf4j"          %  "slf4j-nop"                            % "1.6.4"
  )
}

resolvers += Resolver.mavenLocal

mainClass in (Compile,run) := Some("com.example.StartServer")




  
