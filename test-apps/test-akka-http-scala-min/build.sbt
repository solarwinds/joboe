name          := """test-akka-http-scala-min"""
version       := "1.0.0"
scalaVersion  := "2.12.0"

libraryDependencies ++= {
  val akkaHttpV      = "10.0.0"
  val akkaStreamV    = "2.4.14"
  Seq(
    "com.typesafe.akka"  %% "akka-stream"             % akkaStreamV,
    "com.typesafe.akka"  %% "akka-http"          % akkaHttpV,
    "org.slf4j"          %  "slf4j-nop"                            % "1.6.4"
  )
}


//fork in run := true

//javaOptions in run ++= Seq(
//    "-javaagent:C:\\Users\\pluk\\git\\joboe\\core\\target\\tracelyticsagent_proguard_base.jar=config=C:\\Users\\pluk\\git\\joboe\\core\\conf\\javaagent.json,sample_rate=1000000"
//  )