package mobicomp.emu;

/*
 * ClientThread
 *
 * the task of this class is to start a client in a threadgroup unique to the
 * client. this is required by the method mapThreadToNodename() of class
 * Emualtor. starting the client is done by reflection
 */

import java.io.File;
import java.lang.reflect.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class ClientThread extends Thread
{
	// the client object to be started
	private Client client;
	
	// the threadgroup in which the client needs to be started
	private ThreadGroup threadGroup;
	
	// constructor
	public ClientThread(Client client, ThreadGroup threadGroup)
	{
		// we need to join the passed ThreadGroup because the class 
		// MulticastNetworkEmulator needs this fact to map from sockets to node
		// names
		super(threadGroup, null, threadGroup.getName());
		
		this.client = client;
		this.threadGroup = threadGroup;
	}
	
	// checks whether the client is ready to be started. returns null if read or
	// an error string if not (e.g. when the user did not specify a class name)
	public String ready() throws RuntimeException
	{
		if (client.isRunning())
			throw new RuntimeException("ERROR: Unable to start a client twice");
			
		if (client.getClass() == null)
			return "Please specify a class name";
		
		try
		{
			String[] params = client.getParameterArray();
			
			// the parameters for main()
			Object[] argumentList = { params };
			
			// type of parameters that main() takes:
			// 1 parameter of type String[]
			
			Class[] paramTypes = new Class[1];
			
			if (params != null)
				paramTypes[0] = params.getClass();
			else
				paramTypes[0] = (new String[0]).getClass();
			
			// create an individual classloader for this client and prevent calls to the system classloader
			URLClassLoader loader = new URLClassLoader(getUrlsFromString(client.getClassPath()), new BlockerLoader());
			
			Class myClass = Class.forName(client.getClassName(),true, loader);
			Method mainMethod = myClass.getMethod("main", paramTypes);
			
			// if we get until here, everything seems to be ok
			
			return null;
		}
		catch (ClassNotFoundException e)
		{
			return "Classname " + client.getClassName() + " not found";
		}
		catch (NoSuchMethodException e)
		{
			return "No main() method found taking\n"
				+ "\"" + client.getParameters() + "\" as parameter";
		} catch (MalformedURLException e) {
			return "Invalid Classpath";
		}
		
		// we can not reach this place
	}
	
	// start the client
	public void run()
	{
		if (client.isRunning())
			throw new RuntimeException("ERROR: Unable to start a client twice");
			
		if (client.getClass() == null)
			throw new RuntimeException("ERROR: No classname found");
		
		try
		{
			String[] params = client.getParameterArray();
			
			// the parameters for main()
			Object[] argumentList = { params };
			
			// type of parameters that main() takes:
			// 1 parameter of type String[]
			
			Class[] paramTypes = new Class[1];
			
			if (params != null)
				paramTypes[0] = params.getClass();
			else
				paramTypes[0] = (new String[0]).getClass();
			
			// create an individual classloader for this client and prevent calls to the system classloader
		
			URLClassLoader loader = new URLClassLoader(getUrlsFromString(client.getClassPath()), new BlockerLoader());
			
			
			Class myClass = Class.forName(client.getClassName(),true, loader);
			Method mainMethod = myClass.getMethod("main", paramTypes);
			
			// ready to invoke main(), but first we need to tell the Emulator
			// that the client is now running!
			
			client.setRunning();
			Emulator.getRef().redrawGraph();

			// call main() 
			mainMethod.invoke(null, argumentList);
			
			// everything below this line is not executed until the main method
			// of the client has finished (and this might take a long time!)
		}
		catch (ClassNotFoundException e)
		{
			throw new RuntimeException(
				"Classname " + client.getClassName() + " not found");
		}
		catch (NoSuchMethodException e)
		{
			throw new RuntimeException(
				"Main method not found");
		}
		catch (IllegalAccessException e)
		{
			throw new RuntimeException(
				"Unable to access method main()");
		}
		catch (InvocationTargetException e)
		{		    
		    e.printStackTrace();
			// this should not happen if no NoSuchMethodException was thrown
			throw new RuntimeException(
				"Something very strange happend :)");
		}
		catch (IllegalArgumentException e)
		{
			// this should not happen if no NoSuchMethodException was thrown
			throw new RuntimeException(
				"Something very strange happend :)");
		} catch (MalformedURLException e){
			throw new RuntimeException(
				"Invalid classpath");
		}
	}
	
	// takes a claspath string and splits it up in an array of url 
	private URL[] getUrlsFromString(String classpath) throws MalformedURLException{
		String[] s = classpath.split(System.getProperty("path.separator"));
		URL[] urls = new URL[s.length]; // generate an URL array with enough fields to store all potential urls
		for(int i=0; i<s.length; i++){
			urls[i] = new File(s[i]).toURL();
		}
		return urls;
	}
	
}