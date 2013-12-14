import java.net.*;
import java.util.*;
import java.lang.*;
import java.io.*;
import java.text.*;

public class processing extends bfclient
{
	static void linkdown_calculation(String k)
	{
		Queue<String> q = new LinkedList<String>();
		q.add(k);
		double min_cost;
		int isn = 0;
		String new_link ="";

		while(q.peek() != null)
		{
			String k2 = q.remove();
			
			for(String k1: rup.route_table.keySet())
			{
				if(rup.route_table.get(k1).link != null)
				{
					if(rup.route_table.get(k1).link  == k2)
					{

						// check if shortest path exists
						min_cost  = rup.route_table.get(k1).cost;
						new_link = rup.route_table.get(k1).link;
						for(String key : neighbours.keySet())
						{
							if(key == k1)
							{
								isn = 1;
								break;
							}
							else if(neighbours.get(key).up_status==1)
							{
								if(neighbours.get(key).neighbour_dv.isEmpty() != true)
								{
									for(String dest: neighbours.get(key).neighbour_dv.keySet())
									{
										if(dest == k1)
										{
											double tmp = neighbours.get(key).neighbour_dv.get(dest).cost + rup.route_table.get(key).cost;
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

						if(min_cost == rup.route_table.get(k1).cost && isn == 1)				// no change and direct path exists
						{
							rup.route_table.get(k1).cost = neighbours.get(k1).weight;
							rup.route_table.get(k1).link = k1;
				
						}
						else if(min_cost != rup.route_table.get(k1).cost)						// if shortest path is found
						{
							rup.route_table.get(k1).cost = min_cost;
							rup.route_table.get(k1).link = new_link;
						}	
						else																	// if no change but not a neighbour
						{
							if(User_input.uicalled == 1)
							{
								rup.route_table.get(k1).cost = MAX_COST;
								rup.route_table.get(k1).link = null;	
							}
						}
						q.add(k1);
					}
				}
			}
			isn = 0;
		}

	}
}