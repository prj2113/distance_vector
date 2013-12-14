import java.net.*;
import java.util.*;
import java.lang.*;
import java.io.*;
import java.text.*;

public class Send_update extends TimerTask
{
	ByteArrayOutputStream byos;
	ObjectOutputStream oos;
	byte buf[]= new byte[bfclient.MAX_MESSAGE_SIZE];
	DatagramPacket packet;

	public void send_route_update()
	{
		try
		{
			Message m = new Message("routeupdate",bfclient.rup);
			InetAddress addr;
			int port;
			byos = new ByteArrayOutputStream();
		    oos = new ObjectOutputStream(byos);
			oos.writeObject(m);
			oos.flush();
			buf = byos.toByteArray();																// writes object to byte array

			for (String key : bfclient.neighbours.keySet()) 
			{
				if(bfclient.neighbours.get(key).up_status == 1)										// send only to those neighbours whose link is cuurently up.
				{
					addr = bfclient.neighbours.get(key).addr;
					port = bfclient.neighbours.get(key).port;
					packet = new DatagramPacket(buf, buf.length, addr, port);	
					bfclient.send_update_socket.send(packet);
				}
				//testing statement;
				System.out.println("packet sent to neighboour: " + key);
			}
		}
		catch(NotSerializableException e){}

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

	public void run()
	{
		Send_update su = new Send_update();
		su.send_route_update();
	}
}