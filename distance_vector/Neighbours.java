import java.net.*;

public class Neighbours 																// one object is created for each neighbour
{

	InetAddress addr;
	int port;
	double weight;
	int timeout;
	int up_status;																// keeps track of whether the link is up or down

	Neighbours(InetAddress addr, int port, double weight, int timeout, int up_status)
	{
		this.addr = addr;
		this.port = port;
		this.weight = weight;
		this.timeout = timeout;
		this.up_status = up_status;
	}
}