import java.net.*;
import java.util.*;
import java.lang.*;
import java.io.*;
import java.text.*;

public class Send_update extends TimerTask
{
	static ByteArrayOutputStream byos;
	static ObjectOutputStream oos;
	static byte buf[]= new byte[bfclient.MAX_MESSAGE_SIZE];
	static DatagramPacket packet;

	public void run()
	{
		try
		{
			Message m = new Message("ROUTE_UPDATE",bfclient.rup);
			InetAddress addr;
			int port;
			byos = new ByteArrayOutputStream();
		    oos = new ObjectOutputStream(byos);
			oos.writeObject(m);
			oos.flush();
			buf = byos.toByteArray();						// writes object to byte array

			for (String key : bfclient.neighbours.keySet()) 
			{
				addr = bfclient.neighbours.get(key).addr;
				port = bfclient.neighbours.get(key).port;
				packet = new DatagramPacket(buf, buf.length, addr, port);	
				bfclient.send_update_socket.send(packet);

				//testing statement;
				System.out.println("packet sent to neighboour: " + key);
			}
		}
		catch(SocketException e)
		{
			System.out.println("\nSocket disconnected");
		}
		catch(NotSerializableException e){}
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