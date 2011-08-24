package mobicomp.emu;

/* 
 * GraphFileReader
 *
 * reads a graph from a file.
 *
 *
 * graph file format
 * =================
 *
 * Position			Type	Description
 *
 * Header:
 * -------
 * Byte 0:			byte	version (must be 0x03)
 * Byte 1 to 4:		int		number of clients
 * Byte 5 to 9:		int		number of links (positive!)
 * Byte 10 to n:	Client	the clients (arbitary order)
 * Byte n+1 to end:	Link	the links (arbitary order)
 *
 * Client:
 * -------
 * Byte 0 to 3:		int		length of name (in characters)
 * Byte 4 to x:		chars	[internal] name (ASCII codes)
 * Byte x+1 to x+4:	int		x coordinate
 * Byte x+5 to x+8:	int		y coordinate
 * Byte x+9 to x+12:int		length of class name
 * Byte x+13 to y:	chars	class name
 * Byte y+1 to y+4:	int		length of parameters
 * Byte y+5 to z	chars	parameters
 * 
 * Link:
 * -----
 * Byte 0 to 3:		int		start client (number according to position in the file)
 * Byte 4 to 7:		int		end client (dito)
 * Byte 8:			byte	delay and error type (use constants ERROR_x and DELAY_x)
 * Byte 9 to 12:	int		constant delay
 * Byte 13 to 16:	float	distance delay factor
 * Byte 17 to 20:	float	constant error probability
 * Byte 21 to 24:	int		length of error pattern
 * Byte 25 to x:	chars	error pattern
 *
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Vector;

public class GraphFileReader
{
	// some constants
	public static final int CURRENT_VERSION = 0x03;
	
	// masks for the selected error/delay types
	public static final int DELAY_NO 		= 0x01;
	public static final int DELAY_CONST 	= 0x02;
	public static final int DELAY_DIST 		= 0x04;
	public static final int ERROR_NO 		= 0x08;
	public static final int ERROR_CONST		= 0x10;
	public static final int ERROR_PATTERN 	= 0x20;

	private File file;
	
	private Vector names;
	
	public GraphFileReader(File file)
	{
		this.file = file;
	}
	
	public boolean readIt() throws FileNotFoundException, IOException
	{
		names = new Vector();
		Graph graph = Emulator.getRef().getGraph();
		
		int numberOfClients;
		int numberOfLinks;
		FileInputStream is;
		
		// open file input stream
		if (file.exists())
			is = new FileInputStream(file);
		else
			throw new FileNotFoundException(
				"ERROR: File " + file.getName() + " no found");
		
		// open data input stream
		DataInputStream dis = new DataInputStream(is);
		
		// read the header
		if (is.read() != CURRENT_VERSION)
			throw new IOException("ERROR: Wrong file version");
			
		numberOfClients = dis.readInt();
		numberOfLinks = dis.readInt();;
		
		int nameLength;
		int classLength;
		int paramLength;
		int classpathLength;
		int xPos, yPos;
		String name;
		String className;
		String params;
		String classpath;
		String nextChar;
		byte[] nameArray;
		byte[] classArray;
		byte[] paramArray;
		byte[] classpathArray;
		
		// read all nodes
		for (int i=1; i<=numberOfClients; i++)
		{
			nameLength = dis.readInt();
			nameArray = new byte[nameLength];
			dis.read(nameArray, 0, nameLength);
			name = new String(nameArray);
			xPos = dis.readInt();
			yPos = dis.readInt();

			Client newClient = graph.createClient(name, xPos, yPos);
			
			classLength = dis.readInt();
			if (classLength != 0)
			{
				classArray = new byte[classLength];
				dis.read(classArray, 0, classLength);
				className = new String(classArray);
				newClient.setClassName(className);
			}

			paramLength = dis.readInt();
			if (paramLength != 0)
			{
				paramArray = new byte[paramLength];
				dis.read(paramArray, 0, paramLength);
				params = new String(paramArray);
				newClient.setParameters(params);
			}
			
			classpathLength = dis.readInt();
			if (classpathLength != 0)
			{
				classpathArray = new byte[classpathLength];
				dis.read(classpathArray, 0, classpathLength);
				classpath = new String(classpathArray);
				newClient.setClassPath(classpath);
			}
			
			graph.addClient(newClient);			
			names.add(name);
		}
		
		// read all links
		int srcAddress;
		int destAddress;
		byte type;
		int constDelay;
		float distFact;
		float errorProb;
		int patternLength;
		byte[] patternArray;
		String pattern;
		
		for (int i=1; i<=numberOfLinks; i++)
		{
			srcAddress = dis.readInt();
			destAddress = dis.readInt();
			type = dis.readByte();
			constDelay = dis.readInt();
			distFact = dis.readFloat();
			errorProb = dis.readFloat();
			patternLength = dis.readInt();
			patternArray = new byte[patternLength];
			dis.read(patternArray, 0, patternLength);
			pattern = new String(patternArray);
			
			Client start = graph.getClient((String) names.get(srcAddress));
			Client end = graph.getClient((String) names.get(destAddress));
			
			Link newLink = graph.createLink(start, end);
			newLink.setConstDelayMs(constDelay);
			newLink.setDelayFactor(distFact);
			newLink.setConstErrorProb(errorProb);
			newLink.setErrorPattern(pattern);
			
			if ((type & DELAY_NO) != 0)
				newLink.setDelayType(Link.NO_DELAY);
			else if ((type & DELAY_CONST) != 0)
				newLink.setDelayType(Link.CONST_DELAY);
			else if ((type & DELAY_DIST) != 0)
				newLink.setDelayType(Link.DIST_DELAY);
			else
				throw new IOException("ERROR: Unknown delay type in " + type);
				
			if ((type & ERROR_NO) != 0)
				newLink.setErrorType(Link.NO_ERROR);
			else if ((type & ERROR_CONST) != 0)
				newLink.setErrorType(Link.CONST_ERROR);
			else if ((type & ERROR_PATTERN) != 0)
				newLink.setErrorType(Link.DETERM_ERROR);
			else
				throw new IOException("ERROR: Unknown error type in " + type);
			
			graph.addLink(newLink);
		}
		
		return true;
	}
}