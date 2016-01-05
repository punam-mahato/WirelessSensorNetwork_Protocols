Algorithm Implementation:
Gossip based group communication(Gossip)


Network Topologies:
i)Line ii) 3DGrid

Gossip.scala is the main program that starts the protocol.
Sink.scala is the master actor that instantiates all the participating nodes(actors), and initializes their neighborsList.
		and keeps track of the messages count.
Node.scala implements the nodes participating in the Gossip Protocol.


To run the program:
cd to the directory from the terminal.

>cd Gossip

>sbt

>run numberOfNodes Topology algorithm

numberOfNodes can be: 100, 1000 etc.

Topology can be: Line, 3DGrid

algorithm: gossip

Example: run 1000 3DGrid gossip
