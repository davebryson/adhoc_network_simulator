package mobicomp.emu;

/*
 * OutputHandler
 *
 * this class controls the class OutputWindow which replaces the console by a
 * tabbed pane
 *
 */

import java.io.PrintStream;
import java.io.OutputStream;

public class OutputHandler
{
	private PrintStream defOutStream;
	private PrintStream defErrStream;
	private PrintStream myOutStream;
	private PrintStream myErrStream;
	
	private OutputWindow outwnd;
	
	public OutputHandler()
	{
		defOutStream = System.out;
		defErrStream = System.err;
		myOutStream = new PrintStream(new myOutputStream(false), true);
		myErrStream = new PrintStream(new myOutputStream(true), true);
		
		outwnd = new OutputWindow();
	}
	
	// start handling the output by exchanging the System.out stream
	public void start()
	{
		System.setOut(myOutStream);
		System.setErr(myErrStream);
	}
	
	// hide output window
	public void hideWindow()
	{
		outwnd.setVisible(false);
	}
	
	// show output window
	public void showWindow()
	{
		outwnd.setVisible(true);
	}
	
	// add a client
	public void addClient(String name)
	{
		outwnd.addTab(name);
	}
	
	// remove a client
	public void removeClient(String name)
	{
		outwnd.removeTab(name);
	}
			
	public void sendEmuMsg(String msg, boolean error)
	{
		if (Options.printToConsole)
		{
			if (Options.addNodePrefix)
				defOutStream.print("Emulator: " + msg);
			else
				defOutStream.print(msg);
		}
		else
		{
			outwnd.write(defOutStream, "Emulator", msg, error);
		}
	}
	
	// private class
	private class myOutputStream extends OutputStream
	{
		// true if it catches output to System.err, false if System.out
		private boolean error;
		
		private boolean newline = true;
		
		public myOutputStream(boolean error)
		{
			this.error = error;
		}
		
		public void write(int b)
		{
			String threadName = Emulator.getRef().mapThreadToNodename(Thread.currentThread());
			if (threadName == null)
				threadName = "Emulator";
			
			if (Options.printToConsole)
			{
				if (Options.addNodePrefix)
				{
					// print output with name prefix to the console
					if (newline)
					{
						newline = false;
						
						// attach prefix "name: " to each new line
						defOutStream.print(threadName + ": ");
					}
					
					if (b == '\n')
					{
						newline = true;
					}
				}
				
				defOutStream.write(b);
			}
			else
			{
				// print output to the tab window
				outwnd.write(defOutStream, threadName, Character.toString((char) b), error);
			}
		}
	}
}