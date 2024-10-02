name := """test-akka-http-java-2.0"""

version := "1.0"

scalaVersion := "2.10.6"

libraryDependencies ++= {
  val akkaStreamV      = "2.0"
  Seq(
    "com.typesafe.akka"  %% "akka-stream-experimental"             % akkaStreamV,
    "com.typesafe.akka"  %% "akka-http-core-experimental"          % akkaStreamV,
    "com.typesafe.akka"  %% "akka-http-spray-json-experimental"    % akkaStreamV,
    "org.slf4j"          %  "slf4j-nop"                            % "1.6.4"
  )
}

mainClass in (Compile,run) := Some("com.example.StartServer")




  
