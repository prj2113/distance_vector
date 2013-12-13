import java.io.*;
import java.util.*;
import java.net.*;




public class bfclient
{

	static int sockets_open = 0;
	static Route_update rup;
	static DatagramSocket read_socket;
	static DatagramSocket write_socket;
	static InetAddress ip;
	static Map<String, Neighbours> neighbours;
	static int port;	
	static int tuples;															// number of neighbours
	static int timeout;	
	static int default_neighbour_timeout;
	static final int MAX_NODE = 20;												// number of maxium nodes in the network	
	static final double MAX_COST = 9999;										// it implies infinity
	static String key[]=new String[MAX_NODE];									// combination from ipaddress:portno


	public static void attachShutDownHook()
	{
  		Runtime.getRuntime().addShutdownHook(new Thread() 
  		{
   			public void run() {
    		//System.out.println("Inside Add Shutdown Hook");
    		try
    		{
	    		if(sockets_open == 1)
	    		{
	    			read_socket.close();
	    			write_socket.close();
	    		}
    		}
    		catch(Exception e)
    		{
    			System.out.println(e);
    		}
   		}
  	});
  	}

    static int validateInput(String argv[])
  	{

  		int count = 0;
  		tuples = (argv.length - 2) / 3;
  		int j=1;

  		if( !(argv[0].matches("^[0-9]+$")) )														// allows only numbers
		{
			System.out.println("localport can contain only numbers\n");
		}
  		else if( !(Integer.parseInt(argv[0])>=1024 && Integer.parseInt(argv[0])<=65535) )			// localport should be only between 1024 to 65535
		{
			System.out.println("The localport should only be between 0 to 65535");
		}
		else if( !(argv[1].matches("^[0-9]+$")) )													// timeout should contain only numbers
		{
			System.out.println("timeout can contain only numbers");
		}
		else
		{
	  		for( int i=0; i < tuples ; i++)
	  		{
				if( !(argv[++j].matches("^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}$")) )   				//allows v.x.y.z or localhost
				{
					System.out.println("ipaddress of neighbour " +i+ " should be an ethernet address in format v.x.y.z where v,x,y,z can only be numbers < 256\n");
					break;
				} 
		  		else if( !(argv[++j].matches("^[0-9]+$")) )											// allows only numbers
				{
					System.out.println("port of neighbour " +i+ " can contain only numbers\n");
					break;
				}
		  		else if( !(Integer.parseInt(argv[j])>=1024 && Integer.parseInt(argv[j])<=65535) )	// port should be only between 1024 to 65535
				{
					System.out.println("The port of neighbour " +i+ " should only be between 0 to 65535");
					break;
				}
				else if( !(argv[++j].matches("^[0-9]+[.]{1}[0-9]+$|^[0-9]+$")) )					// weight can be only integer or float
				{
					System.out.println("weight of neighbour " +i+ " can contain only numbers in format xyx or xy.z");
					break;
				}
				else
				{
					count++;
				}
			}
		}

		if( count == tuples )
		{
  			return 1;
  		}
  		else
  		{
  			return 0;
  		}
  	}

  	// display routing table for this node
  	public static void showrt()
  	{
  		for( int i = 0 ; i < tuples ; i++ )
		{
			System.out.println("Destination = " + key[i] + ", cost = " + rup.route_table.get(key[i]).cost +" , Link = " + rup.route_table.get(key[i]).link);	
		}
  	}

	public static void main(String argv[])
	{
		try
		{
			bfclient bf = new bfclient(); 
			bfclient.attachShutDownHook();															// handles abnormal termination

			int valid = 0;

			if(argv.length < 5 || ( (argv.length - 2 ) % 3) != 0)
			{
				System.out.println("Incorrect number of arguments");
				System.out.println("Correct usage: java bfclient localport timeout [ipaddress1 port1 weight1 ...]");
			}
			else
			{
				valid =	bfclient.validateInput(argv);
			}

			if( valid == 1 )
			{

				int j = 1;
				InetAddress n_addr;																		// neighbours address
				int n_port;																				// neighbpurs port
				double n_weight;																		// neighbours port
				int n_timeout;																			// neighbour timeout
				int n_up_status;																		// neighbour status
				String socket;
				

				//read_socket = new DatagramSocket(Integer.parseInt(argv[0])); 							// socket where all Input messages will be read (ROUTEUPDATE, LINKDOWN, LINKUP, SHOWRT)
				//write_socket = new DatagramSocket();													// socket from where all output messages will be sent (ROUTEUPDATE, LINKDOWN, LINKUP, CLOSE)
				//sockets_open = 1;																		// close sockets only if sockets are opened


				neighbours = new HashMap<String, Neighbours>();											// Hashmap of keys(IP:PORT) and corresponding neighbour object
				
				Cost_of_link col;
				Map<String,Cost_of_link> rt = new HashMap<String,Cost_of_link>();

				timeout = Integer.parseInt(argv[1]);													// timeout for this node

				for( int i = 0 ; i < tuples ; i++ )
				{
					// set initial parameters for the route_update object, later only nodes and dv will be updated

					default_neighbour_timeout = timeout;											// set the default neighbour timeout until real timeout is received in the route update 
				
					n_addr = InetAddress.getByName(argv[++j]);
					n_port = Integer.parseInt(argv[++j]);
					n_weight = Double.parseDouble(argv[++j]);
					n_timeout = default_neighbour_timeout;
					n_up_status = 1;
					key[i] = n_addr.getHostAddress()+":"+n_port;										// add other neighbouring nodes to the nodes list

					neighbours.put(key[i],new Neighbours(n_addr , n_port , n_weight , n_timeout , n_up_status));	// add other neighbouring nodes to the neighbours hashmap

					col = new Cost_of_link(n_weight,key[i]);
					rt.put(key[i],col);
					// update the routing_update object
				}

				rup = new Route_update(rt);																// Route_update object --> this object is sent to neighbouring nodes
				rup.own_ip = ip = InetAddress.getLocalHost();										// ip address of this nodes
				rup.own_port = port = Integer.parseInt(argv[0]);									// port of this node
				rup.own_timeout = timeout;								// timeout for this node


				// just for testing
				showrt();

			}
			else
			{
				System.exit(0);
			}

		}
		catch(Exception e)
		{
			System.out.println("Exception caught: " + e);
		}
	}
}