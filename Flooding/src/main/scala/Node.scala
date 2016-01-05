import akka.actor.{ActorRef, ActorSystem, Props, Actor, Inbox}
import akka.actor._ 

import scala.collection.mutable.ListBuffer
import scala.collection.TraversableOnce
import scala.util.control._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory



case class SetYourNeighboursList(neighboursList:ArrayBuffer[ActorRef])
case class BeginFlooding(floodingMessage:String)
case class ForwardMessage(floodingMessage:String, nodeID:Int)
case class STOP()




class Node(sink_actor:ActorRef, index:Int) extends Actor{


	var myNeighboursList:ArrayBuffer[ActorRef] = new ArrayBuffer[ActorRef]
	

	def receive = {
		case SetYourNeighboursList(neighboursList) =>				
			myNeighboursList= neighboursList

		case BeginFlooding(floodingMessage) =>	
			sink_actor ! IncrementMessageCounter(index)		
			
			println("\nFlooding started & passing message to all neighbours.\n")
			for (i<-0 until myNeighboursList.length){
				myNeighboursList(i) ! ForwardMessage(floodingMessage, index)
			}
			


		case ForwardMessage(floodingMessage, nodeID) =>
			sink_actor ! IncrementMessageCounter(index)

			println("\nNode: "+index +" got message from node: " + nodeID + " \t\t Passing message to all neighbors. ")			
			
						
			for (i<-0 until myNeighboursList.length){
				myNeighboursList(i) ! ForwardMessage(floodingMessage, index)
			}

		case STOP() =>
			context.stop(self)
	
	
	}
}
