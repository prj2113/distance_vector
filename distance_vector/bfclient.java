import java.net.*;
import java.util.*;
import java.lang.*;
import java.io.*;
import java.text.*;
import java.nio.*;
import java.nio.channels.*;



public class bfclient
{

	static Route_update rup;
	static DatagramChannel send_update_socket;									// this channel with selector is used to send route updates as well likndown,linkup messages
	static Selector selector;
	static SelectionKey updateKey;
	static InetAddress ip;
	static Timer t; 
	static Send_update send_update ;
	static Map<String, Neighbours> neighbours;									// Keeps all information about each neighbour
	static Map<String, Cost_and_link_to_node> ndv;								// keeps the distance vector of each node
	static Map<String,Cost_and_link_to_node> rt;								// it keeps the routing table of this node
	static Map<String,Neighbour_timeout> nt; 									// it keeps the timeout values and last_received packet timestamp for each node ---> to implement 3*timeout feature
	static Cost_and_link_to_node col;											// cost to corresponding node
	static int port;	
	static int tuples;															// number of neighbours
	static int timeout;	
	static long default_neighbour_timeout;
	static final int MAX_NODE = 20;												// number of maxium nodes in the network	
	static final double INFINITY = 999999;										// it implies infinity
	static final int MAX_MESSAGE_SIZE = 1024;
	static String key[]=new String[MAX_NODE];									// combination from ipaddress:portno


	public static void attachShutDownHook()
	{
  		Runtime.getRuntime().addShutdownHook(new Thread() 
  		{
   			public void run() {
   			System.out.println("Node is now shutdown");
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
			System.out.println("The localport should only be between 1024 to 65535");
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
					System.out.println("The port of neighbour " +i+ " should only be between 1024 to 65535");
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
				long n_timeout;																			// neighbour timeout
				int n_up_status;																		// neighbour status
				
				selector = Selector.open(); 
				send_update_socket = DatagramChannel.open();											// add other neighbouring nodes to the nodes list
				send_update_socket.configureBlocking(false);
				updateKey = send_update_socket.register(selector, SelectionKey.OP_WRITE);


				neighbours = new HashMap<String, Neighbours>();											// Hashmap of keys(IP:PORT) and corresponding neighbour object
				
				
				rt = new HashMap<String,Cost_and_link_to_node>();
				ndv = new HashMap<String,Cost_and_link_to_node>();
				nt  = new HashMap<String,Neighbour_timeout>();
				timeout = Integer.parseInt(argv[1]);													// timeout for this node



				for( int i = 1 ; i <=tuples ; i++ )
				{
					// set initial parameters for the route_update object, later only nodes and dv will be updated

					default_neighbour_timeout = timeout;												// set the default neighbour timeout until real timeout is received in the route update 
				
					n_addr = InetAddress.getByName(argv[++j]);
					n_port = Integer.parseInt(argv[++j]);
					n_weight = Double.parseDouble(argv[++j]);
					n_timeout = default_neighbour_timeout;
					n_up_status = 1;
					key[i] = n_addr + ":" + n_port;
					neighbours.put(key[i],new Neighbours(n_addr , n_port , n_weight , n_timeout , n_up_status, ndv));	// add other neighbouring nodes to the neighbours hashmap
					nt.put(key[i],new Neighbour_timeout(new Date(),(long)n_timeout));
					col = new Cost_and_link_to_node(n_weight,key[i]);									// update cost of node and its link through which shortest path can be found
					rt.put(key[i],col); 																// whenever a new node is added -> put it in rt
				}

				rup = new Route_update(rt);																// Route_update object --> this object is sent to neighbouring nodes
				
				String tmp_ip = InetAddress.getLocalHost().getHostAddress();
				String s[] = tmp_ip.split("/");
				tmp_ip = s[0];
				rup.own_ip = ip = InetAddress.getByName(tmp_ip);										// ip address of this nodes

				rup.own_port = port = Integer.parseInt(argv[0]);										// port of this node
				rup.own_timeout = timeout;																// timeout for this node
				key[0] = ip+":"+port;
				col = new Cost_and_link_to_node(0,key[0]);
				rt.put(key[0],col);																		// first entry in the routing table is always its own

				// call the send_update thread
				t = new Timer();
				send_update = new Send_update();
       			t.schedule(send_update,0,(long)timeout*1000);											// This class sends route_update packet to all active nodes periodically

       			//start user_input thread
       			User_input ui = new User_input();														// This thread is responsible for handling user_input
       			Thread t1 = new Thread(ui);	
       			t1.start();

       			// start read_thread
       			Read_message rm = new Read_message();													// This class reads all packets received but this node like linkup, linkdown, routeupdate
       			Thread t2 = new Thread(rm);
       			t2.start();
       		
       			Thread.sleep(1000);
       			Check_aliveness check_aliveness = new Check_aliveness();								// This thread keeps checking its neighbouring nodes whether its alive or not.
       			Thread t3 = new Thread(check_aliveness);												// if a node doesnt send an update within 3*timeout, then its considered down
       			t3.start();	


			}
			else
			{
				System.exit(0);
			}

		}
		catch(SocketException e)
		{
			System.out.println("\nSocket disconnected");
		}
		catch(IOException e)
		{
			System.out.println(e); 
		}
		catch(Exception e)
		{
		 	System.out.println(e);
		}
	}
}