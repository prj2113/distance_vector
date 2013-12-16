import java.util.*;
import java.io.*;

public class Neighbour_timeout
{
	Date last_update_timestamp;
	long timeout;

	Neighbour_timeout(Date date, long timeout)
	{
		last_update_timestamp = date;
		this.timeout = timeout;
	}
}