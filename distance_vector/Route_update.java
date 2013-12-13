import java.net.*;
import java.util.*;

public class Route_update extends bfclient
{
	InetAddress own_ip;
	int own_port;
	int own_timeout;
	Map<String,Cost_of_link> route_table;

	Route_update(Map<String,Cost_of_link> route_table)
	{
		this.route_table = route_table;	
	}
}

