package mobicomp.emu;

/*
 * EmuSocket
 *
 * this class extends the abstract class DatagramSocketImpl which is required to
 * replace the default java.net.MulticastSocket
 *
 * setOption() and getOption() calls are ignored since they have to do with
 * lower levels which are not implemented by the emulator
 *
 */

import java.net.*;
import java.util.Vector;
import java.io.IOException;
import java.io.FileDescriptor;

public class EmuSocket extends DatagramSocketImpl
{
	// stuff needed for connecting and disconnecting sockets
	private boolean connected = false;
	private InetAddress connectedAddress = null;
	private int connectedPort = -1;
	
	// value of SO_TIMEOUT (0 for infinity)
	private int so_timeout = 0;
	
	private static Emulator emu = Emulator.getRef();
	
	// name of the owner of the socket
	private String ownername;
	
	// all packets ready to be received (priority queue according to the arrival 
	// time of a packet)
	private Vector waitingPackets = new Vector();
	
	// all groups this socket is currently bound to
	private Vector groups = new Vector();
	
	// the thread currently waiting for a packet
	private Thread sleepThread;
	
	// receive a packet by adding it to the waitingPackets vector. the next time
	// receive() is called, this packet will be returned
	public synchronized void receivePacket(DatagramPacket packet, int delay)
	{
		// don't accept packages, if there are to many waiting. this can happen
		// if one of the clients opened a socket and never (or rarely) checks
		// for incoming packets. this would result in a 
		// java.lang.OutOfMemoryException (and this happens fast! *argh*)
		if (waitingPackets.size() > Options.packetBufferSize)
		{
			if (Options.outputPacketOverflow)
				emu.sendEmulatorMessage("Packet thrown away due to buffer overflow\n", false);
				
			return;
		}
			
		// insert the packet in a sorted manner
		long arrivalTime = System.currentTimeMillis() + delay;
		sortedInsert(new pqElem(packet, arrivalTime));
		
		if (sleepThread != null)
			sleepThread.interrupt();
	}
	
	// insert a packet in the waitingPackets priority queue
	private synchronized void sortedInsert(pqElem elem)
	{
		for(int i=0; i<waitingPackets.size(); i++)
		{
			pqElem curElem = (pqElem) waitingPackets.get(i);
			
			if (curElem.arrivalTime > elem.arrivalTime)
			{
				waitingPackets.insertElementAt(elem, i);
				return;
			}
		}
		
		// insert packet at the end
		waitingPackets.insertElementAt(elem, waitingPackets.size());
	}
	
	// finalizer
	public void finalize()
	{
		close();
	}
	
	protected String getOwnerName()
	{
		return ownername;
	}
	
	public Vector getGroups()
	{
		return groups;
	}
	
	/* ************ methods from abstract class DatagramSocketImpl ********** */
	
	// no explicit constructor - create() is called when the socket is created
	
	// Binds this DatagramSocket to a specific address & port. 
	public void bind(int lport, InetAddress laddr)
	{
		if (lport < 0 || laddr == null)
			throw new NullPointerException();
		
		// register the socket with its port and address at the controller
		emu.register(this, lport, laddr);
	}

	// Closes this datagram socket. 
	public void close()
	{
		emu.unregister(this);
	}
	
	// Connects the socket to a remote address for this socket. 
	public void connect(InetAddress address, int port)
	{
		connected = true;
		connectedAddress = address;
		connectedPort = port;
	}

	public void create()
	{
		ownername = emu.mapThreadToNodename(Thread.currentThread());
	}
	
	// Disconnects the socket. 
	public void disconnect() 
	{
		connected = false;
		connectedAddress = null;
		connectedPort = -1;
	}

	// Gets the datagram socket file descriptor 
	public FileDescriptor getFileDescriptor()
	{
		// System.out.println("getFileDescriptor()");
		
		throw new RuntimeException(
			"ERROR: method getFileDescriptor() is not supported");
	}

	// Returns the port number on the local host to which this socket is bound. 
	public int getLocalPort()
	{
		return 0;
	}
	
	public int getTimeToLive()
	{
		return 0;
	}
	
	public byte getTTL()
	{
		return 0;
	}
	
	public void join(InetAddress inetaddr)
	{
		emu.join(this, inetaddr);
		
		if (!groups.contains(inetaddr))
			groups.add(inetaddr);

	}
	
	public void joinGroup(SocketAddress mcastaddr, NetworkInterface netIf)
	{
		return;
	}

	public void leave(InetAddress inetaddr)
	{
		emu.leave(this, inetaddr);
		
		groups.remove(inetaddr);
	}

	public void leaveGroup(SocketAddress mcastaddr, NetworkInterface netIf)
	{
		return;
	}

	public int peek(InetAddress i)
	{
		// wait until there's a packet waiting and then retun its address & port
		while (waitingPackets.isEmpty())
		{
			// sleep some time
			try
			{
				Thread.sleep(100);
			}
			catch (InterruptedException e)
			{
				// just ignore it
			}
		}
		
		DatagramPacket nextPacket = 
			(DatagramPacket) waitingPackets.firstElement();
			
		i = nextPacket.getAddress();
		return nextPacket.getPort();
	}

	public int peekData(DatagramPacket p)
	{
		// wait until there's a packet waiting and then retun its address & port
		while (waitingPackets.isEmpty())
		{
			// sleep some time
			try
			{
				Thread.sleep(100);
			}
			catch (InterruptedException e)
			{
				// just ignore it
			}
		}
		
		DatagramPacket nextPacket = 
			(DatagramPacket) waitingPackets.firstElement();
			
		p.setAddress(nextPacket.getAddress());
		p.setData(nextPacket.getData());
		p.setPort(nextPacket.getPort());
		return nextPacket.getPort();
	}

	// Receives a datagram packet from this socket
	public void receive(DatagramPacket p) throws IOException
	{
	    
	    
		if (sleepThread != null)
		{
			throw new RuntimeException("ERROR: only one thread is allowed to call receive() on a socket");
		}
		
		boolean packetReady = false;
		DatagramPacket newPacket = null;
		long startedReceivingAt = System.currentTimeMillis();
		
		sleepThread = Thread.currentThread();
		int sleeptime = 0;
		
		while (!packetReady)
		{
			while (waitingPackets.isEmpty())
			{
				// sleep a loooong time
				try
				{
					if (so_timeout == 0)
						Thread.sleep(Long.MAX_VALUE);
					else
					    try{
					        Thread.sleep((startedReceivingAt + so_timeout) - System.currentTimeMillis());
					    } catch (IllegalArgumentException iae){
					        // the timeout has become negative
					    }
					sleepThread = null;
					throw new SocketTimeoutException();
				}
				catch (InterruptedException e)
				{
					// a new packet arrived!
				}
			}
			
			// check if we already are allowed to deliver the packet
			long arrivalTime = ((pqElem) waitingPackets.firstElement()).arrivalTime;
			
			
			if (arrivalTime > System.currentTimeMillis())
			{
				// sleep until the packet can be delivered
				try
				{
					if (so_timeout == 0)
						Thread.sleep(arrivalTime - System.currentTimeMillis());
					else
					{
						try {
                            if (arrivalTime > startedReceivingAt + so_timeout){
                          // Do while: If the operating system has a time granularity which is less than so_timeout ms the sleep command 
                          // is NOT called at all. This leads to problems if the link has a delay >0 since the currentTimeMillis() 
                          // is less than startedReceivingAt + so_timeout
                                do{
                                    Thread.sleep((startedReceivingAt + so_timeout) - System.currentTimeMillis());
                                }while(System.currentTimeMillis() == startedReceivingAt);
                            }
                            else{
                                Thread.sleep(arrivalTime - System.currentTimeMillis());
                            }
                        } catch (IllegalArgumentException e1) {
                           // the timeout has become negative
                        }
					}
					
					// check for timeout
					if ((so_timeout != 0) && (System.currentTimeMillis() >= (startedReceivingAt + so_timeout))){
					    sleepThread = null;
					    throw new SocketTimeoutException();
					}
				}
				catch (InterruptedException e)
				{
					if (System.currentTimeMillis() < arrivalTime){
					    continue;
					}
				}
			}
			
			newPacket = ((pqElem) waitingPackets.remove(0)).packet;
			
			// a packet is ready. but if we are connected, we cannot 
			// accept packets from every source!
			if (connected &&
			   ((newPacket.getAddress() != connectedAddress) ||
				(newPacket.getPort() != connectedPort)))
			{
				// ignore the packet
			}
			else
			{
				// after tons of checks, we are allowed to deliver the packet...
				packetReady = true;
			}
		}
		
		sleepThread = null;
		
		// deliver the packet
		p.setAddress(newPacket.getAddress());
		
		if (newPacket.getData().length < p.getData().length)
			System.arraycopy(newPacket.getData(), 0, p.getData(), 0, newPacket.getData().length);
		else
			System.arraycopy(newPacket.getData(), 0, p.getData(), 0, p.getData().length);
		
		p.setLength(newPacket.getLength());
		p.setPort(newPacket.getPort());
		
		
		
//		boolean stopped = false;
//		DatagramPacket newPacket = null;
//		
//		while (!stopped)
//		{
//			// wait until there is a packet ready for being received
//			while (waitingPackets.isEmpty() ||
//				  
//
//			{
//				// sleep some time
//				try
//				{
//					Thread.currentThread().sleep(100);
//				}
//				catch (InterruptedException e)
//				{
//					// just ignore it
//				}
//			}
//			
//			// now there is a packet. but we are only allowed to receive packets
//			// from the connected address, if we are connected
//			
//			if (connected)
//			{
//				if ((newPacket.getAddress() != connectedAddress) ||
//					(newPacket.getPort() != connectedPort))
//				{
//					// ignore the packet
//				}
//				else
//				{
//					// accept the packet - it's coming from our connected
//					// address & port
//					stopped = true;
//				}
//			}
//			else
//			{
//				// always accept the the packet since we're not connected
//				stopped = true;
//			}
//		}
//		
//		// finally, there is packet waiting to be received. copy the content 
//		// of this packet to p
//		p.setAddress(newPacket.getAddress());
//		
//		if (newPacket.getData().length < p.getData().length)
//			System.arraycopy(newPacket.getData(), 0, p.getData(), 0, newPacket.getData().length);
//		else
//			System.arraycopy(newPacket.getData(), 0, p.getData(), 0, p.getData().length);
//		
//		p.setLength(newPacket.getLength());
//		p.setPort(newPacket.getPort());
	}
	
	// Sends a datagram packet from this socket.
	public void send(DatagramPacket p) throws IOException
	{
		if (connected)
		{
			// we're connected! only sending to the connected address & port is 
			// allowed
			
			if (p.getAddress() == null)
			{
				p.setAddress(connectedAddress);
				p.setPort(connectedPort);
			}
			else
			{
				if ((p.getAddress() != connectedAddress) ||
					(p.getPort() != connectedPort))
				{
					// not allowed to send to another address or port than the 
					// connected ones!
					
					throw new SecurityException(
						"connected address and packet address differ");
				}
			}
		}
		
		emu.send(this, p);
	}
	
	public void setTimeToLive(int ttl)
	{
		return;
	}

	public void setTTL(byte ttl)
	{
		return;
	}
	
	/* *************** some methods from interface SocketOptions ************ */
	
	// Fetch the value of an option. 
	public Object getOption(int optID)
	{
		// we just ignore all options - they are about low level stuff...
		
		if (optID == SocketOptions.SO_TIMEOUT)
			return new Integer(so_timeout);
		
		return null;
	}
	
	// Enable/disable the option specified by optID. 
	public void setOption(int optID, Object value)
	{
		// we only handle SO_TIMEOUT - the rest is mostly about low level stuff
		if (optID == SocketOptions.SO_TIMEOUT)
		{
			int val = ((Integer) value).intValue();
			
			if (val > 0)
				this.so_timeout = val;
		}
	}
	
	/* *********************** private classes ****************************** */
	// small datatype for the priority queue
	private class pqElem
	{
		public DatagramPacket packet;
		public long arrivalTime;
		
		public pqElem(DatagramPacket packet, long arrTime)
		{
			this.packet = packet;
			this.arrivalTime = arrTime;
		}
	}
	
}
