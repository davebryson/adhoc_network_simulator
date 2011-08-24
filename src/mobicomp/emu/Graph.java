package mobicomp.emu;

/*
 * interface Graph
 *
 * offers some methods to access/edit a graph
 *
 */
 
import java.util.Vector;

public interface Graph
{
	public void addClient(Client client);
	public void removeClient(Client client);
	
	public Client getClient(String name);
	
	public Vector getClientNames();
	
	public Client createClient();
	public Client createClient(String name);
	public Client createClient(String name, String className, String arams, String classpath);
	public Client createClient(int x, int y);
	public Client createClient(String name, int x, int y);
	public Client createClient(String name, String className, String arams, String classpath, int x, int y);
	
	public void addLink(Link link);
	public void removeLink(Link link);
	
	public Link getLink(Client c1, Client c2);
	
	public Link createLink(Client src, Client dest);
	public Link createLink(Client src, int destx, int desty);
	
	public boolean isLinked(Client c1, Client c2);
	
	public int getNumberOfClients();
	public int getNumberOfLinks();
	
	public void reset();
	
	public void startAllClients();
	
	public Vector getClients();
	public Vector getLinks();
}