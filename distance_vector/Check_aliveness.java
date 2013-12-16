import java.util.*;
import java.io.*;

public class Check_aliveness extends bfclient implements Runnable
{
	public void run()
	{
		long diff;
		while(true)
		{
			Date current_timestamp = new Date();
			for(String key: nt.keySet())
			{
				if(neighbours.get(key).up_status == 1)
				{
					diff = current_timestamp.getTime() - nt.get(key).last_update_timestamp.getTime();	// difference in milliseconds
					diff = diff/1000;																	// difference in seconds
					if(diff > (3*nt.get(key).timeout))
					{
						neighbours.get(key).up_status = 0;
						processing.linkdown_calculation(key);

						// System.out.println("reseted timer due to neighbour timeout");
						// as soon as distance vector changes, reset timer and send update message
						send_update.send_route_update();
						t.cancel();
						t = new Timer();
						t.schedule(new Send_update(),(long)timeout*1000,(long)timeout*1000);
					}
				}
			}

		}
	}
}