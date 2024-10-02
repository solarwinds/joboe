name := """test-akka-http-java"""

version := "1.0"

scalaVersion := "2.12.8"

libraryDependencies ++= {
  val akkaHttpV      = "10.1.8"
  val akkaStreamV    = "2.5.19"
  Seq(
    "com.typesafe.akka"  %% "akka-stream"             % akkaStreamV,
    "com.typesafe.akka"  %% "akka-http"          % akkaHttpV,
    "org.slf4j"          %  "slf4j-nop"                            % "1.6.4"
  )
}

resolvers += Resolver.mavenLocal

mainClass in (Compile,run) := Some("com.example.StartServer")




  
