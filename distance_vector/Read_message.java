/*
	- listen on socket
	- use select
	- selectchannels
	- poll the select
	- record activities
	- process activities
	- type of activities - routeupdate, linkdown, linkup

	# linkdown
		- update routing table to make the sender node as infinity and up_status = 0;
		- mark rup.changed = 1
	# linkup
		- update routing table to original valur and send up_status = 1;
		- mark rup.changed = 1
	# route update
		if not already in neighbours then
			update routing table entry ( based on min cost )
			rup.route_table.get(k).cost
			rup.route_table.get(k).link
			rup.changed_status = 1;
		else
			create new node
				add to array key
				put the node in neighbours
				new cost_and_link_to_node object
				put in route_table_rt
				overwrite route_update
				rup.changed_status = 1;

	# handle 3*timeout
		- create hashmap <key,(last_received_timestamp,3*timeout)>
			- attach a timer to each neighbour
			- if current_time - last_received_timestamp > 3*timeout then mark link as down and cost infinite
*/			
