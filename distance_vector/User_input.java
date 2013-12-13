import java.net.*;
import java.util.*;
import java.lang.*;
import java.io.*;
import java.text.*;

public class User_input extends bfclient implements Runnable
{
	String s[] = new String[3];
	int iperror = 0;
	int porterror = 0;
	int formaterror = 0;
	int ret = -1;

	int validate()
	{
		ret = -1;
		if( !(s[1].matches("^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}$")) )   				//allows v.x.y.z or localhost
		{
			iperror = 1;
		} 
		else if( !(s[2].matches("^[0-9]+$")) )											// allows only numbers
		{
			porterror = 1;
		}
  		else if( !(Integer.parseInt(s[2]) >=1024 && Integer.parseInt(s[2]) <=65535) )	// port should be only between 1024 to 65535
		{
			porterror = 1;
		}
		else
		{
			ret = 0;
		}

		if( ret != 0)
		{
			if(iperror == 1)
			{
				System.out.println("ipaddress should be in format v.x.y.z where v,x,y,z can only be numbers < 256\n");			
			}
			else if(porterror == 1)
			{
				System.out.println("The port should only be between 1024 to 65535 and can contain only numbers");
			}
		}
		return ret;
	}


	void linkdown()
	{

	}
 	
 	void linkup()
 	{

 	}

 	// display routing table for this node
 	void showrt()
 	{
  		for( int i = 0 ; i < rt.size() ; i++ )
		{
			System.out.println("Destination = " + key[i] + ", cost = " + rup.route_table.get(key[i]).cost +" , Link = " + rup.route_table.get(key[i]).link);	
		}
 	}

 	void close_cmd()
 	{

 	}

	public void run()
	{
		try
		{
			String cont = "Y";
			String command;
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			User_input ui = new User_input();

			while(cont.equals("Y") == true)
			{
				System.out.println("userthread started");
				iperror = 0;
				porterror = 0;
				formaterror = 0;

				System.out.println("Type the action required:\n 1. LINKDOWN {ip_address port} \n 2. LINKUP {ip_address port} \n 3. SHOWRT \n 4. CLOSE");
				command = br.readLine();
				s=command.split(" ");

				switch(s[0])
				{
					case "LINKDOWN":
					{
						ret = ui.validate();
						if(ret != -1)
						{
							ui.linkdown();	
							break;
						}
						else
						{
							break;
						}
					}
					case "LINKUP":
					{
						ret = ui.validate();
						if(ret != -1)
						{
							ui.linkup();	
							break;
						}
						else
						{
							break;
						}
						
					}
					case "SHOWRT":
					{
						ui.showrt();
						break;
					}
					case "CLOSE":
					{
						ui.close_cmd();
						break;
					}
					default:
					{
						System.out.println("Format error: please enter commands in the exact format");
						formaterror = 1;
						break;
					}

				}
				if(formaterror == 1)
				{
					continue;
				}
				else
				{
					System.out.println("Do you want to run more actions: default is N. choose (Y/N)");
					cont = br.readLine();
				}
			}

			System.out.println("no more actions will be performed, just periodic route_update are being sent");
			System.out.println("if you wish to shut the node, press Y");
			if(br.readLine().equals("Y"))
			{
				System.exit(0);
			}
			else
			{
				System.out.println("Node is active.. If you want to terminate later, just press ctrl-c");
			}
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