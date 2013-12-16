import java.net.*;
import java.util.*;
import java.lang.*;
import java.io.*;
import java.text.*;
import java.nio.*;
import java.nio.channels.*;

public class Read_message extends bfclient implements Runnable
{

    public class Con 
    {
    	ByteBuffer bb;
        public Con() 
        {
        	bb = ByteBuffer.allocate(MAX_MESSAGE_SIZE);     
        }
    }

	public void read(SelectionKey key) throws IOException, ClassNotFoundException
	{
		DatagramChannel chan = (DatagramChannel)key.channel();
        Con con = (Con)key.attachment();
        chan.receive(con.bb);
        InputStream baos = new ByteArrayInputStream(con.bb.array(),0,con.bb.limit());
	    ObjectInputStream oos = new ObjectInputStream(baos);
		Message m_received = (Message)oos.readObject();											// read object
		
        // System.out.println("Message type: " + m_received.message_name);
        switch(m_received.message_name)															// depending on the message name, call the corresponding functions in the class processing
        {
        	case "routeupdate":
        	{
        		processing.routeupdate(m_received);
        		break;
        	}
        	case "linkdown":
        	{
        		processing.linkdown(m_received);
        		break;
        	}
        	case "linkup":
        	{
        		processing.linkup(m_received);
        		break;
        	}
        }
 
       	con.bb.clear();
	}

	public void run()
	{
		try
		{
			Selector selector = Selector.open();
		    DatagramChannel channel = DatagramChannel.open();
		    InetSocketAddress isa = new InetSocketAddress(rup.own_port);
		    channel.socket().bind(isa);														// bind the port mentioned in cmd line --> all nodes sent messgaes to this node on this port
		    channel.configureBlocking(false);												// makes the channel non-blocking
		    SelectionKey neighbourKey = channel.register(selector, SelectionKey.OP_READ);
		    neighbourKey.attach(new Con());
		    while (true) 
		    {
		        try 
		        {
		            selector.select();
		            Iterator selectedKeys = selector.selectedKeys().iterator();
		            while (selectedKeys.hasNext()) 
		            {
		                try 
		                {
		                	SelectionKey key = (SelectionKey) selectedKeys.next();
							selectedKeys.remove();
							if (!key.isValid()) 
							{
		                        continue;
		                    }

		                    if (key.isReadable()) 
		                    {
		                        read(key);
		                    } 
		                }
		                catch (IOException e) 
		                {
		                    System.err.println("ERROR: " + (e.getMessage()!=null?e.getMessage():""));
		                }
		                catch(ClassNotFoundException e)
		                {
		                	System.out.println("ERROR: " + (e.getMessage()!=null?e.getMessage():""));
		                }
		            }
		        } 
		        catch (IOException e) 
		        {
		            System.err.println("ERROR: " +(e.getMessage()!=null?e.getMessage():""));
		        }
	  		}
	    } 
	    catch (IOException e) 
	    {
	            System.err.println("network error: " + (e.getMessage()!=null?e.getMessage():""));
	    }
	}
}
