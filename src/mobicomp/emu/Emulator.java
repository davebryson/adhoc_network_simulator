package mobicomp.emu;

/*
 * Emulator
 *
 * Emulates an mulitcast network.
 *
 */

import java.util.Iterator;
import java.util.Set;
import java.util.HashMap;
import java.util.Vector;
import java.util.Properties;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.DatagramSocketImpl;
import java.net.DatagramSocketImplFactory;
import java.net.InetAddress;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;

import java.awt.Dimension;

public class Emulator
{
	// needed for singleton pattern
	private static Emulator ref = null;
	
	// a hashmap: groupaddress -> vector of all members (sockets) of this group
	private HashMap groups;
	
	// a vector with all registered sockets
	private Vector allSockets;
	
	// number of packets sent
	private int packetCount = 0;
	
	// number of packets thrown away due to errors on the link
	private int packetErrorCount = 0;
	
	// references the the main windows and its drawpanel
	private MainWindow mainWnd;
	private DrawPanel drawPanel;
	
	// counter to generate unique standard node names
	private int nextNodeIndex = 0;
	
	// all currently running ThreadGroups. we need this to identify which socket
	// belongs to which node (resp. to the name of a node)
	private Vector threadGroups = null;
	
	// the currently opened graph file
	private File curFile = null;
	
	// the output handler
	private OutputHandler oh;
	
	// private default constructor (-> singleton!)
	private Emulator()
	{
		ref = this;
		
		// set a new DatagramSocketFactory
		try
		{
			DatagramSocket.setDatagramSocketImplFactory(new EmuSocketFactory());
		}
		catch (IOException e)
		{
			System.out.println("oops, something went wrong");
			e.printStackTrace();
		}

		// read config file
		try 
		{
			FileInputStream propFile = new FileInputStream("emulator.txt");
			Properties p = new Properties(System.getProperties());
			p.load(propFile);
			System.setProperties(p);
		} 
		catch (IOException ioe) 
		{
			System.out.println("ERROR: could not read from file emulator.txt");
		}
		
		// set global settings according to the properties
		if (System.getProperty("mobicomp.emu.outputPacketSent", "false").equals("false"))
			Options.outputPacketSent = false;
		else
			Options.outputPacketSent = true;
			
		if (System.getProperty("mobicomp.emu.outputPacketLost", "false").equals("false"))
			Options.outputPacketLost = false;
		else
			Options.outputPacketLost = true;
		
		if (System.getProperty("mobicomp.emu.outputPacketOverflow", "false").equals("false"))
			Options.outputPacketOverflow = false;
		else
			Options.outputPacketOverflow = true;

		if (System.getProperty("mobicomp.emu.printToConsole", "false").equals("false"))
			Options.printToConsole = false;
		else
			Options.printToConsole = true;
			
		if (System.getProperty("mobicomp.emu.addNodePrefix", "false").equals("false"))
			Options.addNodePrefix = false;
		else
			Options.addNodePrefix = true;
			
		Options.charBufferSize = Integer.parseInt(System.getProperty("mobicomp.emu.charBufferSize", "2000"));
		Options.packetBufferSize = Integer.parseInt(System.getProperty("mobicomp.emu.packetBufferSize", "20"));
		Options.flashTime = Integer.parseInt(System.getProperty("mobicomp.emu.flashTime", "800"));

		// start handling output
		oh = new OutputHandler();
		oh.start();
		
		packetCount = 0;
		
		allSockets = new Vector();
		groups = new HashMap();
		threadGroups = new Vector();
		
		mainWnd = new MainWindow();
        System.out.println("Get DrawPanel");
		drawPanel = mainWnd.getDrawPanel();
	}
	
	// show/hide output window
	public void setOutputWindowVisibility(boolean visible)
	{
		if (visible)
			oh.showWindow();
		else
			oh.hideWindow();
	}
	
	// returns a reference to itself
	public static Emulator getRef()
	{
		if (ref != null)
		{
			return ref;
		}
		
		throw new RuntimeException("ERROR: Can not access singleton");
	}
	
	// redraw the graph (eg when the color of a node has changed)
	public void redrawGraph()
	{
		Dimension dim = new Dimension();
		drawPanel.getSize(dim);
		drawPanel.repaint(0, 0, dim.width, dim.height);
	}
	
	public Graph getGraph()
	{
		return (Graph) drawPanel;
	}
	
	public int getPacketCount()
	{
		return packetCount;
	}
	
	public String getNextNodeName()
	{
		return Integer.toString(++nextNodeIndex);
	}
	
	/**
	 * Rewind the id for the next node by one 
	 * should be called if the node generation is aborted
	 */
	public void rewindNextNodeName(){
		nextNodeIndex--;
	}
	

	// Send a packet to all members of the group p.getAddress()
	public synchronized void send(EmuSocket sender, DatagramPacket p)
	{
		Client senderClient = drawPanel.getClient(sender.getOwnerName());
		
		if (senderClient == null)
		{
			// this can happen if the user just removed this client
			return;
		}
		
		Vector neighbors = senderClient.getLinks();
		
		if (neighbors == null || neighbors.isEmpty())
		{
			// this client has no links - dont send the packet
			return;
		}
		
		for (Iterator iter = neighbors.iterator(); iter.hasNext(); )
		{
			Link curLink = (Link) iter.next();
			
			switch (curLink.send(p, senderClient, sender))
			{
				case Link.PACKET_SENT:
				{
					// packet was successfully send - increase counter
					this.packetCount++;
					
					if (Options.outputPacketSent){
						sendEmulatorMessage(
							"Packet sent from " + sender.getOwnerName() + " to "
							+ curLink.getReceiver(senderClient).getName() 
							+ "\n Content: " + toHexString(p.getData()) + "\n"
							, false);
					}	
					break;
				}
				
				case Link.PACKET_LOST:
				{
					// simulated error - increase error counter
					this.packetErrorCount++;
					
					if (Options.outputPacketLost)
						sendEmulatorMessage(
							"Packet lost on the link between " + sender.getOwnerName()
							+ " and " + curLink.getReceiver(senderClient).getName() 
							+ "\n", false);
						
					break;
				}
				
				case Link.NO_GROUP_MEMBER:
				{
					// no packet needs to be sent
					break;
				}
				
				default:
					throw new RuntimeException("ERROR: illegal return value");
			}
		}
		
		// send the packet to the sender socket, too
		sender.receivePacket(p, 0);
	}
	
	// Register a new socket in the system
	//
	// NOTE: The socket is bound to a local address/port, but has NOT yet joined
	//       a multicast group.
	public void register(EmuSocket socket, int port, InetAddress addr)
	{
		if (socket.getOwnerName() == null)
		{
			// the emulator could not identify the owner of the socket
			JOptionPane.showMessageDialog(mainWnd, 
				"A socket was opened but the emulator could not identify\n" +
				"its owner. Please do not create sockets as a reaction\n" +
				"to an event from a GUI component.\n\n" +
				"For further information, please consult the documentation",
				"Socket error",
				JOptionPane.ERROR_MESSAGE);
			
			return;
		}
		
		allSockets.add(new VectorElement(socket, port, addr));
		
		// tell the client about the new socket
		drawPanel.getClient(socket.getOwnerName()).addSocket(socket);
	}
	
	// Add a socket to a group. Of course, a socket can be connected to multiple
	// multicast groups
	public void join(EmuSocket socket, InetAddress addr)
	{
		if (!groups.containsKey(addr))
		{
			// create a new group with an empty member vector
			groups.put(addr, new Vector());
		}
		
		// add the socket as new member
		Vector curMembers = (Vector) groups.get(addr);
		
		if (curMembers.contains(socket))
		{
			// the socket already joined this group
			return;
		}
		else
		{
			// add the socket as a new member
			curMembers.add(socket);
		}
	}
	
	// Remove a socket from the system
	public void unregister(EmuSocket socket)
	{
		// remove the socket from all groups
		Set keys = groups.keySet();
		
		for (Iterator iter = keys.iterator(); iter.hasNext(); )
		{
			Vector curGroup = (Vector) groups.get((InetAddress) iter.next());
			curGroup.remove(socket);
		}
		
		// remove the socket itself
		for (Iterator iter = allSockets.iterator(); iter.hasNext(); )
		{
			VectorElement curElem = (VectorElement) iter.next();
			
			if (curElem.socket == socket)
			{
				allSockets.remove(curElem);
				return;
			}
		}
		
		drawPanel.getClient(socket.getOwnerName()).removeSocket(socket);
	}
	
	// Remove a socket from the multicast group with address addr
	public void leave(EmuSocket socket, InetAddress addr)
	{
		Vector groupMembers = (Vector) groups.get(addr);
		
		groupMembers.remove(socket);
		
		if (groupMembers.isEmpty())
		{
			groups.remove(addr);
		}
	}
	
	public boolean startClient(Client client)
	{
		if (client.getName() == null)
			// can not happen - clients are always created with a standard name
			throw new RuntimeException(
				"ERROR: Trying to start a client without an internal name");
		
		if (client.getClassName() == null)
		{
			JOptionPane.showMessageDialog(mainWnd, 
				"Please specify a class name",
				"No class name found",
				JOptionPane.ERROR_MESSAGE);
				
			EditClientWindow ecWnd = new EditClientWindow(mainWnd, client);
			return false;
		}
		
		// the input seems to be ok - create the new instance of the program
		ThreadGroup newGroup = new ThreadGroup(client.getName());
		addThreadGroup(newGroup);
		
		// let's try to start that program
		ClientThread dummy = new ClientThread(client, newGroup);
		String ans = dummy.ready();
		
		if (ans == null)
		{
			oh.addClient(client.getName());
			dummy.start();
			return true;
		}
			
		// there seems to be some problem
		JOptionPane.showMessageDialog(mainWnd, 
			ans,
			"Error",
			JOptionPane.ERROR_MESSAGE);
			
		removeThreadGroup(newGroup);
		EditClientWindow ecWnd = new EditClientWindow(mainWnd, client);
		return ecWnd.isCanceled();
	}
	
	// remove a client from the emulation
	public synchronized void removeClient(Client c)
	{
		if (!c.isRunning())
		{
			// remove all links starting/ending at the client
			Vector linksToRemove = c.getLinks();
			while (!linksToRemove.isEmpty())
				drawPanel.removeLink((Link) linksToRemove.get(0));
				
			// just remove the client from the graph
			drawPanel.removeClient(c);
			return;
		}
		
		// remove all sockets from the client
		boolean finished = false;
		
		while (!finished)
		{
			EmuSocket toRemove = null;
			finished = true;
			
			for (Iterator iter=allSockets.iterator(); iter.hasNext(); )
			{
				toRemove = ((VectorElement) iter.next()).socket;
				
				if (toRemove.getOwnerName().equals(c.getName()))
				{
					finished = false;
					break;
				}
			}
			
			if (!finished)
				unregister(toRemove);
		}
		
		// remove all links starting/ending at the client
		Vector linksToRemove = c.getLinks();
		while (!linksToRemove.isEmpty())
			drawPanel.removeLink((Link) linksToRemove.get(0));
			
		// remove the node from the graph
		drawPanel.removeClient(c);
		
		// remove client from the output handler
		oh.removeClient(c.getName());
		
		// NOTE:
		//
		// The program is still running! There is no way to force a running
		// thread to stop from outside (without the use of the depricated 
		// methods as Thread.stop() or ThreadGroup.stop()). If there is a way
		// to terminate the program WITHOUT the use of System.exit(), the user
		// can of course do this.
	}
	
	// send a message from the emulator
	public void sendEmulatorMessage(String msg, boolean error)
	{
		oh.sendEmuMsg(msg, error);
	}
	
	// loads a graph from file
	// returns true if the graph was really loaded, false if not
	public boolean loadGraph()
	{
		// ask user which graph file to open
		JFileChooser dlg = new JFileChooser();
		dlg.setFileFilter(new myFilter());
		dlg.setDialogTitle("Open File");
		dlg.setDialogType(JFileChooser.OPEN_DIALOG);
		dlg.setCurrentDirectory(new java.io.File("."));
		int ret = dlg.showOpenDialog(mainWnd);
		
		if (ret != JFileChooser.APPROVE_OPTION)
		{
			// user pressed cancel
			return false;
		}
		
		// reset current graph
		curFile = dlg.getSelectedFile();
		drawPanel.reset();
		
		// read the graph from file
		GraphFileReader reader;
		
		try
		{
			reader = new GraphFileReader(curFile);
			reader.readIt();
			return true;
		}
		catch (IOException e)
		{
			JOptionPane.showMessageDialog(mainWnd, 
				"Could not open file " + curFile.getName(),
				"Error opening graph",
				JOptionPane.ERROR_MESSAGE);
				
			curFile = null;
			return false;
		}
	}
	
	public boolean saveGraphAs()
	{
		// ask user where to save the graph file
		JFileChooser dlg = new JFileChooser();
		dlg.setFileFilter(new myFilter());
		dlg.setDialogTitle("Save File As");
		dlg.setDialogType(JFileChooser.SAVE_DIALOG);
		dlg.setCurrentDirectory(new java.io.File("."));
		int ret = dlg.showSaveDialog(mainWnd);
		
		if (ret == JFileChooser.APPROVE_OPTION)
		{
			curFile = dlg.getSelectedFile();
			
			// add .graph extension if required
			if (!(curFile.getName().toLowerCase()).endsWith(".graph"))
				curFile = new File(curFile.getPath() + ".graph");
			
			return saveGraph();
		}
		
		return false;
	}
	
	public boolean saveGraph()
	{
		if (curFile == null)
		{
			return saveGraphAs();
		}
		else
		{
			GraphFileWriter writer = new GraphFileWriter(curFile);
			
			try
			{
				writer.writeIt();
				return true;
			}
			catch (IOException e)
			{
				JOptionPane.showMessageDialog(mainWnd, 
					"Graph couldn't be saved",
					"Error saving graph",
					JOptionPane.ERROR_MESSAGE);
			}
			
			return false;
		}
	}
	
	private void addThreadGroup(ThreadGroup group)
	{
		threadGroups.add(group);
	}
	
	private void removeThreadGroup(ThreadGroup group)
	{
		threadGroups.remove(group);
	}
	
	public String mapThreadToNodename(Thread thread)
	{
		ThreadGroup lookFor = thread.getThreadGroup();
		
		// System.out.println("Looking for: " + lookFor.getName());
		
		if (threadGroups == null)
			return null;
		
		for (Iterator iter=threadGroups.iterator(); iter.hasNext(); )
		{
			ThreadGroup curGroup = (ThreadGroup) iter.next();
			
			if (curGroup.parentOf(lookFor))
			{
				// we found the thread group we looked for
				return curGroup.getName();
			}
		}
		
		// if the thread isn't found in one of the threadgroups, the thread
		// must be the emulator itself and not a running client
		return null;
	}
	
	
	// convert a byte array to a string of hex symbols
	public static String toHexString(byte[] b) {
		StringBuffer sb = new StringBuffer(b.length * 2);
		for (int i = 0; i < b.length; i++) {
			// look up high nibble char
			sb.append(hexChar[(b[i] & 0xf0) >>> 4]);

			// look up low nibble char
			sb.append(hexChar[b[i] & 0x0f]);

			// add a space between every byte
			sb.append(' ');
		}
		sb.setLength(sb.length()-1);
		return sb.toString();
	}

//	 table to convert a nibble to a hex char.
	static char[] hexChar = {
	   '0' , '1' , '2' , '3' ,
	   '4' , '5' , '6' , '7' ,
	   '8' , '9' , 'a' , 'b' ,
	   'c' , 'd' , 'e' , 'f'};

	

/* NOT UP TO DATE !!	
	public String toString()
	{
		String ret = new String("Content of the emulator\n");
		ret += "-----------------------\n\n";
		ret += "active groups including their members:\n";
		
		for (Enumeration enum1 = groups.keys(); enum1.hasMoreElements(); )
		{
			InetAddress curAddress = (InetAddress) enum1.nextElement();
			
			ret += "\t" + curAddress.toString() + "\n";
			
			for (Enumeration enum2 = ((Hashtable) groups.get(curAddress)).keys();
				enum2.hasMoreElements(); )
			{
				String curName = (String) enum2.nextElement();
				
				ret += "\t\t" + curName + "\n";
			}
		}
		
		ret += "\nlinks between all members:\n";
		// ret += graph.toString();
		
		return ret;
	}
*/
	
	// main method of the whole project
	public static void main(String[] args)
	{
		ref = new Emulator();
	//	ref.start();
	}

	// private filename filter class for load/save graph dialog
	private class myFilter extends FileFilter
	{
		public boolean accept(File file)
		{
			if(file.isDirectory())
				return true;
			else
			{
				if(file.getName().endsWith(".graph"))
					return true;
				else
					return false;
			}
		}
		
		public String getDescription()
		{
			return "Ad Hoc Graph Files (*.graph)";
		}
	}
	
	// factory for replacing MulticastSocket with EmuSocket 
	private class EmuSocketFactory implements DatagramSocketImplFactory
	{
		public DatagramSocketImpl createDatagramSocketImpl()
		{
			return new EmuSocket();
		}
	}

	// A small class to save a socket with its address & port in the vector
	private class VectorElement
	{
		/* NOTE:
		 * We don't actually need to know the address and port of a socket since
		 * we're not going to send anything to these destinations. But we save
		 * them anyway - perhaps they could be used for anything in the future
		 */
		 
		public EmuSocket socket;
		public int port;
		public InetAddress address;
		
		public VectorElement(EmuSocket s, int p, InetAddress a)
		{
			socket = s;
			port = p;
			address = a;
		}
	}

/*	
	private class prohibitExitSecurityManager extends SecurityManager
	{
		public prohibitExitSecurityManager()
		{
			super();
		}
		
		public void checkExit(int code)
		{
			System.out.println("checked");
			throw new SecurityException("ERROR: System.exit() is not allowed");
		}
	}
*/
//	private class myListener implements AWTEventListener
//	{
//		public void eventDispatched(AWTEvent e)
//		{
//			if (e.getSource().getClass().getName().equals("javax.swing.JButton"))
//			System.out.println(e.getClass().getName());
//		}
//	}
}