name          := """test-akka-http-scala"""
version       := "1.0.0"
scalaVersion  := "2.12.8"

libraryDependencies ++= {
  val akkaHttpV      = "10.1.8"
  val akkaStreamV    = "2.5.19"
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