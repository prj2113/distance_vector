<u>This folder contains the following programs</u>
<ul>
	bfclient.java					--> main program , all other classes are called through this
	Check_aliveness.java			--> keeps track of neighbours status
	Read_message.java				--> handles all commands received by the node		
	Send_update.java				--> sends periodic updates to each active neighbour
	User_input.java					--> handles all user commands like linkdown, linkup, close and showrt
	processing.java					--> contains functions to implement linkup,linkdown,routeupdate messages recieved by the node
	Cost_and_link_to_node.java		--> cost and link to each corresponding destination
	Route_update.java				--> Own node informtation and its routing table
	Message.java					--> encapsulates the type of message and its route_update packet
	Neighbour_timeout.java			--> contains timestamp and timeout values corresponding to each neighbour
	Neighbours.java					--> keeps all information related to each neighbour
	Makefile						--> compiles all these files
</ul>
	
*** NOTE ***
	All nodes should use different ports and for ip address please use the host address and not localhost or 127.0.0.1.
	All user commands should be entered in CAPITAL Letters only
	Please enter same weights for example, a->b = 5 then b->a = 5
	INFINITY is represented as 999999.

*** PROGRAM EXECUTION *** 
	1. Run make 
	2. open one terminal for each node and run ----> java bfclient localport timeout [ipaddress1 port1 weight1 ...]
	3. Run any user command as directed in the cmd prompt --> just follow the instructions carefully on the cmd prompt

*** EXAMPLE *** 
	(replace the ip accoringing to machine on which the program is being executed and weights can be entered in decimal as well)

	make
	java bfclient 5000 2 160.39.207.151 6000 3 160.39.207.151 8000 7
	java bfclient 6000 4 160.39.207.151 5000 3 160.39.207.151 7000 2
	java bfclient 7000 6 160.39.207.151 6000 2 160.39.207.151 8000 1
	java bfclient 8000 8 160.39.207.151 5000 7 160.39.207.151 7000 1 160.39.207.151 9000 4
	java bfclient 9000 2 160.39.207.151 8000 4


*** INFORMATION MAINTAINENCE DETAILS ***
	I have implemented distance vector routing protocol using object oriented programming. All information is mainly stored in Hashmaps using socket address ( IP:PORT ) as key
	Maintainece of various information is done:
		1. 	Local informtion about the node
			This information is maintained in the route_update object. 
			It contains its ipaddress, portno, timeout and a HashMap for maintaining the routing table.
			The routing table contains the tuples --> {destination, cost_to_the destination, first_hop_node} 
		2.	Information about each neighbour
			One object of this class is created for each neighbour.
			It stores information like ipaddr, port, weight, timeout value, up_status and the routing table of each neighbour
		3.	Information to keep track of the aliveness of each neighbour
			A hashmap neighbour_time using socket address as the key keeps 2 information --> time of last received route update packet from that neighbour and its timeout
		4. 	Message trasnferred between 2 nodes
			This object encapsulates the message type and route_update object. 
			The various message types are : linkup, linkdown and routeupdate.
			Nodes communicate using this message object passed over non-blocking I/O datagram channels. The non-blocking is achieved using selector.
			Each node has one selector for the read_socket and one selector for write_socket.

*** IMPLEMENTATION *** 
	- Initially, when a node is started it stores it own information and the information of neighbours directly mentioned in the commandline. 
	- It also, creates an initial routing table
	- A timer is set to its timeout value. Thus, periodically a routeupdate is sent to each active neighbour.
	- Also, whenever any changes take place in the routing table, the routeupdate is immediately sent and the timer is reset.
	- A seperate thread is started for handling user commands.
		The various user commands are:
			1.	LINKDOWN IP PORT
				The link to this neighbour is marked as down and the routing table is updated. 
				For updation of the routing table, the Bellman ford equation is used with all the distance vectors of neighbouring nodes maintained in the Neighbours object.
				Then, the a "linkdown" message is sent to the corresponding node -> indicating that it has broken the link using non-blocking output channel.
			2.	LINKUP IP PORT
				The link to this neighbour is marked as up and its entry in the routing table is updated to the original value which can be retrieved from the Neighbours object.
				Then, the a "linkup" message is sent to the corresponding node -> indicating that it has made the link again using non-blocking output channel.
			3.  SHOWRT
				It displays the routing table of this node. It displays information only about its neighbours. 
			4. 	CLOSE
				It just shuts down the node.
	- A second thread is started for reading messages arriving on the non-blocking i/o port
		The commands are read using selector and processed sequentially. For each command corresponding fucntion in the class Proccessing is called.	
			1. 	linkdown
				It checks who sent the message and marks that socket as down. It also updates the routing table accordingly.
			2.	linkup
				It checks who sent the message and marks that socket as up. It also updates the routing table entry to the original value which can be retrieved from the Neighbours object.
			3. 	routeupdates
				It first updates or makes an entry about the time of last received route update packet from that neighbour in the neighbour_time object.
				It then checks if the node already exists in its neighbour, if no then a new entry is added to the neighbour object. Also, the corresponding key is made.
				if exists, then updates his status to 1 and updates his routing table.
				Then, it checks the routing table of this node to find any new destinations, if found then it adds them to the key list and routing table with link as this given node.
				Now, fianlly the bellman ford equation is applied to update the entire routing table to get minimum cost for each destination.
				If any changes occur then the route update is sent immediately and timer is reset.

	- A check_aliveness thread is started to continously monitor the status of each neighbour.
		A hashmap of <key, (last_received_update_timestamp, timeout)> is maintained which contains information about timeout of all neighbouring nodes.
		Continously, each of the key is checked to see if ( currentTime - last_received_update_timestamp > 3*timeout ), if true then the corresponding node is marked as down and the routing table is updated. The route update is sent immediately and timer is reset.



	
	