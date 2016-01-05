import akka.actor.{ActorRef, ActorSystem, Props, Actor, Inbox}
import akka.actor._ 
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import scala.collection.mutable.ListBuffer
import scala.collection.TraversableOnce
import scala.util.control._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import java.io._



case class SetYourNeighboursList(neighboursList:ArrayBuffer[ActorRef])
case class SenseData()
case class ADV(nodeID:Int, sensingTime_metaData:Long)
case class REQ(nodeID:Int, sensingTime_metaData:Long)
case class DATA(nodeID:Int, updatedData:Int, sensingTime_metaData:Long)
case class STOP()





class Node(sink_actor:ActorRef, index:Int) extends Actor{
	//val index:Int = index
	
	var lastDataCollection_Time: Long =0
	//var sensingTime_metaData: Long =0
	var newData = 0
	var newCPUTemperatureData= ""

	var myNeighboursList:ArrayBuffer[ActorRef] = new ArrayBuffer[ActorRef]
	

	def receive = {
		case SetYourNeighboursList(neighboursList) =>				
			myNeighboursList= neighboursList
			//println("Node "+index+ " , neighboursList: " + myNeighboursList)
		case SenseData() =>
			sink_actor ! NodeReceivedData(index)
			var r=Runtime.getRuntime()
			var p=r.exec("sensors")
			var temp = ""
			println(p)
			var pin=new BufferedReader(new InputStreamReader(p.getInputStream()))
			println(pin)
			val newCPUTemperatureData = Stream.continually(pin.readLine()).takeWhile(_ != null)
			println(newCPUTemperatureData)
			/*
			while((temp=pin.readLine())!=null){
				//println("-----"+temp)
				newCPUTemperatureData += temp
				}
			*/
			newData = (scala.util.Random).nextInt(100000)
			println("\nNode: "+index+" : Collected new data and passing ADV packets to all neighbours.")
			var neighbour = null
			//metadata
			var sensingTime_metaData: Long = System.currentTimeMillis
			println ("Sensing Time : " + sensingTime_metaData)
			lastDataCollection_Time = sensingTime_metaData			
			for (neighbour <- myNeighboursList){				
				neighbour ! ADV(index, sensingTime_metaData)	
						
			}




		case ADV(nodeID, sensingTime_metaData) =>
			
			sink_actor ! IncrementControlMessageCounter()
			//println("\nNode: "+index + "---"+ lastDataCollection_Time+ "---"+ sensingTime_metaData+"------"+ nodeID)
			if ((sensingTime_metaData - lastDataCollection_Time ) > 0.0 ){
				println("\nNode: "+index+" : Received ADV packet from node: " + nodeID)
				sender ! REQ(index, sensingTime_metaData)
			}
			else {
				println("\nNode: "+index+" : Received ADV packet from node "+ nodeID+". But have updated data already!")
			}

		case REQ(nodeID, sensingTime_metaData) =>
			sink_actor ! IncrementControlMessageCounter()
			println("\nNode: "+index+" : Received REQ packet from node "+ nodeID)
			sender ! DATA(index, newData, sensingTime_metaData)

		case DATA(nodeID, updatedData, sensingTime_metaData) =>
			sink_actor ! NodeReceivedData(index)
			sink_actor ! IncrementDataMessageCounter()
			lastDataCollection_Time = sensingTime_metaData
			newData = updatedData
			var neighbour = null
			for (neighbour <- myNeighboursList){
				println("\nNode: "+index+" : New data received from node "+ nodeID+" and passing ADV packets to all neighbours.")
				neighbour ! ADV(index, sensingTime_metaData)				
			}

		case STOP() =>
			context.stop(self)
	
	}
}
