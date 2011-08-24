package mobicomp.emu;

/* 
 * GraphFileWriter
 *
 * writes a graph to a file.
 *
 */
 
import java.io.File;
import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;
import java.util.Iterator;

public class GraphFileWriter
{
	private File file;

	public GraphFileWriter(File file)
	{
		this.file = file;
	}
	
	// save the graph to a file
	public boolean writeIt() throws IOException
	{
		Graph graph = Emulator.getRef().getGraph();
		
		Vector clients = graph.getClients();
		Vector links = graph.getLinks();
		
		FileOutputStream os;
		
		if (file.exists())
		{
			// create an empty new file
			file.delete();
			file.createNewFile();
		}

		// open file output stream
		os = new FileOutputStream(file);
		
		// open the data output stream
		DataOutputStream dos = new DataOutputStream(os);
		
		// write the header
		dos.writeByte(GraphFileReader.CURRENT_VERSION);
		dos.writeInt(clients.size());
		dos.writeInt(links.size());
		
		Client curCl;
		String name;
		int nameLength;
		byte[] nameByteArray;
		char[] nameCharArray;
		
		// write all nodes
		for(Iterator iter = clients.iterator(); iter.hasNext(); )
		{
			curCl = (Client) iter.next();
			name = curCl.getName();
			nameLength = name.length();
			nameCharArray = name.toCharArray();
			nameByteArray = new byte[nameLength];
			for(int i=0; i<nameLength; i++)
			{
				nameByteArray[i] = (byte) nameCharArray[i];
			}
			
			dos.writeInt(nameLength);
			dos.write(nameByteArray);
			dos.writeInt(curCl.getX());
			dos.writeInt(curCl.getY());
			
			// write class
			name = curCl.getClassName();
			if (name == null || name.equals(""))
			{
				// write empty string
				dos.writeInt(0);
			}
			else
			{
				// write class name
				nameLength = name.length();
				nameCharArray = name.toCharArray();
				nameByteArray = new byte[nameLength];
				for(int i=0; i<nameLength; i++)
				{
					nameByteArray[i] = (byte) nameCharArray[i];
				}
				dos.writeInt(nameLength);
				dos.write(nameByteArray);
			}
			
			// write parameters
			name = curCl.getParameters();
			if (name == null || name.equals(""))
			{
				// write empty string
				dos.writeInt(0);
			}
			else
			{
				// write class name
				nameLength = name.length();
				nameCharArray = name.toCharArray();
				nameByteArray = new byte[nameLength];
				for(int i=0; i<nameLength; i++)
				{
					nameByteArray[i] = (byte) nameCharArray[i];
				}
				dos.writeInt(nameLength);
				dos.write(nameByteArray);
			}
			
			// write classpath
			name = curCl.getClassPath();
			if (name == null || name.equals(""))
			{
				// write empty string
				dos.writeInt(0);
			}
			else
			{
				// write classpath
				nameLength = name.length();
				nameCharArray = name.toCharArray();
				nameByteArray = new byte[nameLength];
				for(int i=0; i<nameLength; i++)
				{
					nameByteArray[i] = (byte) nameCharArray[i];
				}
				dos.writeInt(nameLength);
				dos.write(nameByteArray);
			}
		}
		
		// write all links
		for(Iterator iter = links.iterator(); iter.hasNext(); )
		{
			Link curLnk = (Link) iter.next();
			
			int src = clients.indexOf(curLnk.getStart());
			int dest = clients.indexOf(curLnk.getEnd());
			
			int type = 0;
			int delayType = curLnk.getDelayType();
			int errorType = curLnk.getErrorType();
			
			switch (delayType)
			{
				case Link.NO_DELAY:
					type |= GraphFileReader.DELAY_NO;
					break;
					
				case Link.CONST_DELAY:
					type |= GraphFileReader.DELAY_CONST;
					break;
					
				case Link.DIST_DELAY:
					type |= GraphFileReader.DELAY_DIST;
					break;
					
				default:
					throw new RuntimeException("ERROR: Unknown delay type " + delayType);
			}
			
			switch (errorType)
			{
				case Link.NO_ERROR:
					type |= GraphFileReader.ERROR_NO;
					break;
					
				case Link.CONST_ERROR:
					type |= GraphFileReader.ERROR_CONST;
					break;
					
				case Link.DETERM_ERROR:
					type |= GraphFileReader.ERROR_PATTERN;
					break;
					
				default:
					throw new RuntimeException("ERROR: Unknown error type" + errorType);
			}
			
			dos.writeInt(src);
			dos.writeInt(dest);
			dos.writeByte(type);
			dos.writeInt(curLnk.getConstDelayMs());
			dos.writeFloat(curLnk.getDelayFactor());
			dos.writeFloat(curLnk.getConstErrorProb());
			dos.writeInt(curLnk.getErrorPattern().length());
			dos.write(curLnk.getErrorPattern().getBytes());
		}
		
		return true;
	}
}