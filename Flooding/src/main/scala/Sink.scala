import akka.actor.{ActorRef, ActorSystem, Props, Actor, Inbox}
import akka.util.Timeout
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.ArrayBuffer
import scala.math
import scala.concurrent.{Future, blocking}
import akka.pattern.ask
import com.typesafe.config.ConfigFactory



case class CreateTopology(sink_actor:ActorRef)
case class StartFlooding(floodingMessage:String)
case class IncrementMessageCounter(index: Int)

object Sink {
	
	def props(acsys: ActorSystem, numNodes: Int, topology: String):Props =
    Props(classOf[Sink], acsys, numNodes, topology)
}



class Sink(acsys: ActorSystem, numNodes: Int, topology: String) extends Actor{

	
	var ActorsList: ArrayBuffer[ActorRef] = new ArrayBuffer[ActorRef]
	var dimension:Int=0
	var ActorsIn3DGrid = Array.ofDim[ActorRef](dimension, dimension, dimension)
	var TOTALMESSAGESCOUNT = 0
	var start_time: Long = 0
	var Nodes_WithUpdatedData = new ArrayBuffer[Int]

	

	def receive ={
		case CreateTopology(sink_actor:ActorRef)=> setUpNodes(sink_actor,numNodes,topology)

		
		
		case StartFlooding(floodingMessage)=>
			var len = ActorsList.length
			println ("length: "+len)
			for (i<-0 until len){
				Nodes_WithUpdatedData += 0
			}
			Nodes_WithUpdatedData.toArray

			start_time = System.currentTimeMillis 
			val r = (scala.util.Random).nextInt(ActorsList.length)
			println("\n"+"Start gossip from " + r +"th node in the ActorsList!" +"\n")
			println(ActorsList.length)
			//println(Nodes_WithUpdatedData)
			ActorsList(r) ! BeginFlooding(floodingMessage)	



		case IncrementMessageCounter(index:Int) =>
			TOTALMESSAGESCOUNT += 1
			Nodes_WithUpdatedData(index) = 1
			println("Did all nodes receive data(1 represents yes)??: "+ Nodes_WithUpdatedData)
			if (!(Nodes_WithUpdatedData.contains(0))){
				/*for (i<-0 until ActorsList.length){
					ActorsList(i) ! STOP()
				}*/
				println("\n\n****************************************************")
				println("\nALL NODES RECEIVED UPDATED DATA!!")
				println("\nTotal number of messages passed: "+ TOTALMESSAGESCOUNT)
				val end_time: Long = System.currentTimeMillis
				val time_taken: Long = end_time - start_time
				println("\nTotal time taken: "+ time_taken)
				println("\nBandwidth required: " + (((TOTALMESSAGESCOUNT*1000).toDouble)/time_taken)+ "\n\n")

				context.system.shutdown()
				sys.exit(0)	
			}


	}


	def setUpNodes(sink_actor:ActorRef, numNodes:Int, topology:String) = {
		//Actors arranged in Line: Linear array
		if(topology == "Line"){
			for (i<-0 until numNodes){
				var index:Int =i
				val acref =  context.actorOf(Props(classOf[Node], sink_actor,i) )
				ActorsList+=acref
				//println("index: "+index)
			}
			
		}



		//Actors arranged in 3D Grid: 3D array
		if (topology == "3DGrid"){
			println("\nTopology: "+ topology)
			dimension = scala.math.ceil(scala.math.cbrt(numNodes)).toInt
			
			ActorsIn3DGrid = Array.ofDim[ActorRef](dimension, dimension, dimension)
			val newnumNodes:Int= scala.math.pow(dimension, 3).toInt

			//creating actors
			for (i<-0 until newnumNodes){

				var index:Int =i
				val acref =  context.actorOf(Props(classOf[Node], sink_actor, i) )
				ActorsList+=acref
				//println("index: "+index)
			}
			
			println("\nNew number of nodes: " + ActorsList.length)
			//println("\n"+ActorsList)			
			println("\nDimension of the Grid: " + dimension+ "*" +dimension+ "*"+dimension)
			

			//arranging actors into 3DGrid
			var x:Int =0			
			for (i<-0 until dimension){					
					for (j<-0 until dimension){
						for (k<-0 until dimension){
							//println("\ni:"+i+" j:"+j+" k:"+k)
							ActorsIn3DGrid(i)(j)(k)= ActorsList(x)
							//println(ActorsList(x))
							x +=1
						}

					}
				}

			println("----------------------------------------------------------")
		

		}		
		

		//Creating neighbours list according to topology 
		topology match{


			case "Line" => {
				for (i <- 0 until numNodes) {
		          var neighboursList: ArrayBuffer[ActorRef] = new ArrayBuffer[ActorRef]
		          if(i == 0) {
		            neighboursList += ActorsList(i + 1)
		          }
		          else if(i == (numNodes - 1)) {
		            neighboursList += ActorsList(i - 1)
		          }
		          else {
		            neighboursList += ActorsList(i - 1)
		            neighboursList += ActorsList(i + 1)
		          }

		          ActorsList(i) ! SetYourNeighboursList(neighboursList)
				}
			println("Neighbours List Set")				
			}
			
			case "3DGrid" => {
				makeNeighboursList(ActorsIn3DGrid, dimension, topology)				
			}


		}

		//return 0
	}

	def makeNeighboursList(ActorsIn3DGrid:Array[Array[Array[ActorRef]]], dimension:Int, topology:String){
				
		for (i<-0 until dimension){
			for (j<-0 until dimension){
				for (k<-0 until dimension){
					var includeLeft:Boolean = true
					var includeRight:Boolean = true
					var includeBottom:Boolean = true
					var includeTop:Boolean = true
					var includeFront:Boolean = true
					var includeBack:Boolean = true					

					var neighboursList: ArrayBuffer[ActorRef] = new ArrayBuffer[ActorRef]

					if(i==0){includeLeft = false}
					if(i==(dimension-1)){includeRight = false}
					if(j==0){includeBottom = false}
					if(j==(dimension-1)){includeTop = false}
					if(k==0){includeFront = false}
					if(k==(dimension-1)){includeBack = false}

					if(includeLeft){neighboursList += ActorsIn3DGrid((i-1))(j)(k)}
					if(includeRight){neighboursList += ActorsIn3DGrid((i+1))(j)(k)}
					if(includeBottom){neighboursList += ActorsIn3DGrid(i)((j-1))(k)}
					if(includeTop){neighboursList += ActorsIn3DGrid(i)((j+1))(k)}
					if(includeFront){neighboursList += ActorsIn3DGrid(i)(j)((k-1))}
					if(includeBack){neighboursList += ActorsIn3DGrid(i)(j)((k+1))}

					/*
					println("\nActor at node, i:"+i+" j:"+j+" k:"+k)
					println("My neighboursList is: " + neighboursList)
					for (z<-0 until neighboursList.length){
							for (i<-0 until dimension){
								for (j<-0 until dimension){
									for (k<-0 until dimension){
										if(ActorsIn3DGrid(i)(j)(k) == neighboursList(z))
											println("Neighbour's Index: i:"+i+" j:"+j+" k:"+k)
											}}}
					}*/

					ActorsIn3DGrid(i)(j)(k) ! SetYourNeighboursList(neighboursList)

				}

			}
		}
		println("Neighbours List Set")

		
	}

}
