package controllers

import akka.actor._


/**
 * @author pluk
 */
class LocalActor extends Actor {

  // create the remote actor
  val remote = context.actorSelection("akka.tcp://HelloRemoteSystem@127.0.0.1:5150/user/RemoteActor")
  var counter = 0

  def receive = {
    case "START" => 
        remote ! "Hello from the LocalActor"
    case msg: String => 
        println(s"LocalActor received message: '$msg'")
        if (counter < 5) {
            sender ! "Hello back to you"
            counter += 1
        }
  }
}

object LocalActor {
  implicit val system = ActorSystem("LocalSystem")
  val localActor = system.actorOf(Props[LocalActor], name = "LocalActor")  // the local actor
  def test() = {
    localActor ! "START"
  }
}
