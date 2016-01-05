import akka.actor.{ActorRef, ActorSystem, Props, Actor, Inbox}
import akka.actor._ 

import scala.collection.mutable.ListBuffer
import scala.collection.TraversableOnce
import scala.util.control._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory



case class SetYourNeighboursList(neighboursList:ArrayBuffer[ActorRef])
case class BeginGossip(gossipMessage:String)
case class PassMessage(gossipMessage:String, nodeID:Int)
case class BeginPushSum(startSum:Double, startWeight:Double)
case class PassSum(receivedSum:Double, receivedWeight:Double)
case class STOP()




class Node(sink_actor:ActorRef, index:Int) extends Actor{


	var myNeighboursList:ArrayBuffer[ActorRef] = new ArrayBuffer[ActorRef]
	

	def receive = {
		case SetYourNeighboursList(neighboursList) =>				
			myNeighboursList= neighboursList

		case BeginGossip(gossipMessage) =>	
			sink_actor ! IncrementMessageCounter(index)		
			val r = (scala.util.Random).nextInt(myNeighboursList.length)
			println("\nGossip started & passing message to neighbour: " + r +"\n")
			myNeighboursList(r) ! PassMessage(gossipMessage, index)


		case PassMessage(gossipMessage, nodeID) =>
			sink_actor ! IncrementMessageCounter(index)

			println("\nNode: "+index +" got message from node: " + nodeID)
			
			val r = (scala.util.Random).nextInt(myNeighboursList.length)
			println("\nNode: "+index +" passing message to neighbour: " + r)
			//println("MygossipMessageCount: " + gossipMessageCount)
			myNeighboursList(r) ! PassMessage(gossipMessage,index)

		case STOP() =>
			context.stop(self)
	
	
	}
}
