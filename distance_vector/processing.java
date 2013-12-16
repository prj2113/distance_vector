import java.net.*;
import java.util.*;
import java.lang.*;
import java.io.*;
import java.text.*;

public class processing extends bfclient
{
	static void routeupdate(Message m_received)
	{
		String k = m_received.rup.own_ip + ":" +  m_received.rup.own_port;
		int isnei = 0;
		double cost =0.0;
		String tmp="";
		double min_cost;
		String new_link;


		nt.put(k, new Neighbour_timeout(new Date(), m_received.rup.own_timeout));						// put a new entry or update previous entry in nt

		for(String key: neighbours.keySet())
		{
			if( k.equals(key))
			{
				isnei = 1;
				break;
			}
		}

		tmp = (rup.own_ip + ":" + rup.own_port);

		// if not already a neighbour , then add to neighbours list
		if(isnei == 0)
		{	
			for(String k_new : m_received.rup.route_table.keySet())
			{	
				
				if(k_new.equals(tmp))
				{
					cost = m_received.rup.route_table.get(k_new).cost;
					break;
				}
			}
			
			// add to neighbours list
			neighbours.put(k,new Neighbours(m_received.rup.own_ip, m_received.rup.own_port, cost, m_received.rup.own_timeout, 1, m_received.rup.route_table));
			// add to key array
			key[++tuples] = k;

			// update its routing table
			rt.put(k,new Cost_and_link_to_node(cost, k));
		}
		else
		{
			for(String existing_neighbour: neighbours.keySet())
			{
				if(existing_neighbour.equals(k))
				{		
					// mark it as up
					neighbours.get(existing_neighbour).up_status = 1;
					// updates its dv
					neighbours.get(existing_neighbour).neighbour_dv = m_received.rup.route_table;
					// update its timeout value
					neighbours.get(existing_neighbour).timeout = m_received.rup.own_timeout;
					break;
				}
			}
		}

		// check each the rt of message for any new dest .. if any add them to the rt of this node and put their link as the key of message
		int exists = 0;
		for( String new_m_key : m_received.rup.route_table.keySet())
		{
			for(String rup_k : rup.route_table.keySet())
			{
				if(!(new_m_key.equals(tmp)))
				{
					if(new_m_key.equals(rup_k))
					{
						exists = 1;	
					}
				}
			}
			if( exists == 0 && !new_m_key.equals(tmp))
			{
				key[++tuples] = new_m_key;
				if(rup.route_table.get(k).cost == INFINITY)
				{
					rt.put(new_m_key,new Cost_and_link_to_node(INFINITY,"none"));
				}
				else
				{
					cost = rup.route_table.get(k).cost + m_received.rup.route_table.get(new_m_key).cost;
					rt.put(new_m_key,new Cost_and_link_to_node(cost,k));
				}
			}
			exists = 0;
		}


		int changed = 0;

		// update the routing table using bellman ford 
		int found = 0;
		for( String node: rup.route_table.keySet())
		{
			min_cost = rup.route_table.get(node).cost;
			new_link = rup.route_table.get(node).link;

			for( String neighbour: neighbours.keySet())
			{
				for(String dest: neighbours.get(neighbour).neighbour_dv.keySet())
				{
					if(dest.equals(node) && (neighbours.get(neighbour).up_status == 1) ) 					// consider only those neighbours which are up
					{

						// if originally a neighbour and is currently up and no other path, then restore original values
						if((neighbours.get(neighbour).neighbour_dv.get(dest).cost == INFINITY) && (neighbours.get(dest) != null) && (neighbours.get(dest).up_status == 1))
						{
							min_cost = neighbours.get(dest).weight;	
							new_link = dest;
							changed = 1;
						}
						else
						{
							// find min from all neighbours
							double temp_cost = neighbours.get(neighbour).neighbour_dv.get(dest).cost + rup.route_table.get(neighbour).cost;
							if(temp_cost < min_cost)
							{
								min_cost = temp_cost;
								new_link = neighbour;
								changed = 1;
							}
						}
						found = 1;
					}

				}
			}

			// if no path found and its a neighbour and up, then restore original value
			if( (found  == 0) && (neighbours.get(node) != null ))
			{
				if( neighbours.get(node).up_status == 1)
				{
					rup.route_table.get(node).cost = neighbours.get(node).weight;
					rup.route_table.get(node).link = node;
				}
				else
				{
					rup.route_table.get(node).cost = INFINITY;
					rup.route_table.get(node).link ="none";
				}
			}
			else
			{
				rup.route_table.get(node).cost = min_cost;
				rup.route_table.get(node).link = new_link;
			}
		}

		if(changed == 1)
		{
			//System.out.println("reseted timer due to routeupdate");
			// as soon as distance vector changes, reset timer and send update message
			send_update.send_route_update();
			t.cancel();
			t = new Timer();
			t.schedule(new Send_update(),(long)timeout*1000,(long)timeout*1000);
		}

	}

	static void linkup(Message m_received)
	{
		String k = m_received.rup.own_ip + ":" +  m_received.rup.own_port;
		
		//restore original values
		rup.route_table.get(k).cost = neighbours.get(k).weight;
		rup.route_table.get(k).link = k;

		// mark as up
		neighbours.get(k).up_status = 1;

		//System.out.println("reseted timer due to linkup received");
		// as soon as distance vector changes, reset timer and send update message
		send_update.send_route_update();
		t.cancel();
		t = new Timer();
		t.schedule(new Send_update(),(long)timeout*1000,(long)timeout*1000);
	}

	static void linkdown(Message m_received)
	{
		String key = m_received.rup.own_ip + ":" +  m_received.rup.own_port;
		neighbours.get(key).up_status = 0;						// mark as down

		processing.linkdown_calculation(key);					// update routing table
		
		//System.out.println("reseted timer due to linkdown received");
		// as soon as distance vector changes, reset timer and send update message
		send_update.send_route_update();
		t.cancel();
		t = new Timer();
		t.schedule(new Send_update(),(long)timeout*1000,(long)timeout*1000);

	}

	// for broken link to node b , set the correct distance for all other linked nodes
	static void linkdown_calculation(String k)																				
	{	
		Queue<String> q = new LinkedList<String>();
		double min_cost;
		int isn = 0;
		String new_link ="";

		rup.route_table.get(k).cost = INFINITY;
		rup.route_table.get(k).link = "none";

		Map<String,Integer> visited = new HashMap<String,Integer>();

		for(int i = 1; i < rt.size() ; i++ )
		{
			visited.put(key[i],new Integer(0));
		}

		q.add(k);


		while(q.peek() != null)
		{
			String k2 = q.remove();
			visited.put(k2,new Integer(1));
			for(String k1: rup.route_table.keySet())
			{
				if( !((rup.route_table.get(k1).link).equals("none")) )
				{
				
					if( (rup.route_table.get(k1).link).equals(k2) )
					{

						// check if shortest path exists
						min_cost = rup.route_table.get(k1).cost;
						new_link = rup.route_table.get(k1).link;
						for(String key : neighbours.keySet())
						{
							if( !key.equals(k) )
							{
								if(key.equals(k1))
								{
									isn = 1;
								}
								if( (neighbours.get(key).up_status) == 1)													// consider only up neighbours
								{
									if( (neighbours.get(key).neighbour_dv).isEmpty() != true )											// check if neighbour's dv exists
									{
										for(String dest: neighbours.get(key).neighbour_dv.keySet() )									// check wrt to the dv of all neighbours whose up_status == 1
										{
											if(dest.equals(k1))
											{
												double tmp = neighbours.get(key).neighbour_dv.get(dest).cost + rup.route_table.get(key).cost;	// get minimum cost
												if(tmp < min_cost)
												{
													min_cost = tmp;
													new_link = key;
												}
											}
										}
									}
								}
							}

						}

						if( (min_cost == rup.route_table.get(k1).cost) && (isn == 1) )			// no change and direct path exists   --> set to original k1 is up
						{
							if(neighbours.get(k1).up_status == 1)
							{
								rup.route_table.get(k1).cost = neighbours.get(k1).weight;
								rup.route_table.get(k1).link = k1;
							}
				
						}
						else if( min_cost != rup.route_table.get(k1).cost )						// if shortest path is found	--> update its cost and link
						{
							
							rup.route_table.get(k1).cost = min_cost;
							rup.route_table.get(k1).link = new_link;
							
						}	
						else																	// if no change but not a neighbour --> not reachable thru anythg except k , then set to infinite
						{
							rup.route_table.get(k1).cost = INFINITY;
							rup.route_table.get(k1).link = "none";	
						}

						if( visited.get(k1).intValue() != 1)
						{
							q.add(k1);
						}
					}
				}
				isn = 0;
			}
			
		}

	}

}