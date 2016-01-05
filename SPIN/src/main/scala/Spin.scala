import akka.actor.{ActorRef, ActorSystem, Props, Actor, Inbox}

import akka.util.Timeout
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.ArrayBuffer
import scala.math
import com.typesafe.config.ConfigFactory
import scala.concurrent.{Future, blocking}
import akka.pattern.ask


object Spin {
	
	def main(args: Array[String]){
		

		if (args.length==2){
			val sinksystem = ActorSystem("SinkActorSystem", ConfigFactory.load().getConfig("masterSystem"))
			val nodes = args(0).toInt
			val sink_actor = sinksystem.actorOf(Sink.props(sinksystem, nodes, args(1)), name = "SinkActor")
			
			sink_actor ! CreateTopology(sink_actor)
			Thread.sleep(3000)
			sink_actor ! StartSPIN()

		}

		else {
			println("Provide 3 arguments: numNodes, topology:<Line|3DGrid>")
		}
	}
}
