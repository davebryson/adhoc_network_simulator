package mobicomp.emu;

/*
 * Client
 *
 * this class is the internal representation of a client. the task of this class
 * is to keep track of all its incoming and outgoing links, but also to draw 
 * a graphical representation into the DrawPanel.
 *
 */
 
import java.util.Vector;
import java.awt.*;

public class Client
{
	// SLEEPING_xxx means the corresponding program is not running yet
	public static final Color RUNNING_CLIENT = new Color(0.5f, 0.5f, 1.0f);
	public static final Color RUNNING_CLIENT_BORDER = new Color(0.0f, 0.0f, 1.0f);
	public static final Color RUNNING_CLIENT_TEXT = new Color(0.0f, 0.0f, 0.0f);
	public static final Color SLEEPING_CLIENT = new Color(1.0f, 0.5f, 0.5f);
	public static final Color SLEEPING_CLIENT_BORDER = new Color(1.0f, 0.0f, 0.0f);
	public static final Color SLEEPING_CLIENT_TEXT = new Color(0.0f, 0.0f, 0.0f);
	
	// radius and the width of the border when drawn
	public static final int RADIUS = 30;
	public static final int BORDER_WIDTH = 7;
	
	// the (internal) name of the client
	private String name;
	
	// classname, parameters and classpath to start the client
	private String className;
	private String parameters;
	private String classpath;
	
	
	// the position on the drawpanel
	private int x;
	private int y;
	
	// all links ending at this client
	private Vector links = new Vector();
	
	// all sockets of the client
	private Vector sockets = new Vector();
	
	// is the client already running in the emulator?
	private boolean running;
	
	// small buffer - we don't want to redraw the whole node each time
	private Image backupImage;
	private Graphics2D backupGraphic;
	
	// constructor
	public Client(String name, int x, int y)
	{
		Graph graph = Emulator.getRef().getGraph();
		
		if (graph.getClient(name) != null)
		{
			// duplicate use of names not allowed. postfix counter to make it
			// unique
			String newname;
			int counter = 2;
			
			do
			{
				newname = name + " (" + counter + ")";
				counter++;
			}
			while (graph.getClient(newname) != null);
			
			name = newname;
		}
		
		this.name = name;
		this.x = x;
		this.y = y;
		this.classpath = System.getProperty("user.dir");
	}
	
	// constructor used when className, parameters and classpath are already known
	public Client(String name, String className, String params, String classpath, int x, int y)
	{
		this(name, x, y);
		this.className = className;
		this.parameters = params;
		this.classpath = classpath;
	}
	
	// returns the parameters as string array, "\\s" is any whitespace
	public String[] getParameterArray()
	{
		if (parameters == null)
			return new String[0];
		else
			return parameters.split("\\s");
	}
	
	// returns the classpath for this client
	public String getClassPath(){
		return classpath;
	}
	
	// adds a socket
	public void addSocket(EmuSocket socket)
	{
		if (socket != null)
			sockets.add(socket);
	}
	
	// removes a socket
	public void removeSocket(EmuSocket socket)
	{
		if (socket != null)
			sockets.remove(socket);
	}
	
	// get all sockets
	public Vector getSockets()
	{
		return sockets;
	}
	
	// adds a new link
	public void addLink(Link lnk)
	{
		if (lnk != null)
			links.add(lnk);
	}
	
	// removes a link
	public void removeLink(Link lnk)
	{
		if (lnk != null)
			links.remove(lnk);
	}

	// draw the node into g
	// param1: the graphics object to draw
	// param2: reference to the DrawPanel. needed to create compatible images
	//
	// to speed up the drawing, the node is only painted the first time and
	// and saved in backupImage. every subsequent call of draw() just copies the
	// image from backupImage to g
	public void draw(Graphics g, Component c)
	{
		// g needs to be of type Graphics2D !!
		if (g==null || c==null || !(g instanceof Graphics2D))
			throw new RuntimeException("ERROR: Drawing failed");
		
		// draw the backupImage if required
		if (backupImage == null)
		{
			backupImage = c.createImage(2*RADIUS, 2*RADIUS);
			backupGraphic = (Graphics2D) backupImage.getGraphics();
			backupGraphic.setRenderingHint(
				RenderingHints.KEY_ANTIALIASING, 
				RenderingHints.VALUE_ANTIALIAS_ON);

			drawOnce(backupGraphic);
		}

		// copy the image over from the backup
		g.drawImage(backupImage, x-RADIUS, y-RADIUS, null);
	}
	
	// draws a picture of the node to backupImage
	private void drawOnce(Graphics g)
	{
		String displayedName = name;
		FontMetrics fm = g.getFontMetrics();
		
		// shorten the name if it is to long to display
		while (fm.stringWidth(displayedName) > 2*(RADIUS-BORDER_WIDTH))
			displayedName = 
				displayedName.substring(0, displayedName.length()-1);
		
		// offsets to place the text nicely in the middle of the circle
		int strOffsetX = fm.stringWidth(displayedName)/2;
		int strOffsetY = fm.getMaxAscent() - 
			(fm.getMaxAscent()+fm.getMaxDescent())/2;
		
		// draw background
		g.setColor(DrawPanel.BACKGROUND);
		g.fillRect(0, 0, 2*RADIUS, 2*RADIUS);
		
		// draw the node
		setBorderColor(g);
		g.fillOval(0, 0, 2*RADIUS, 2*RADIUS);
		setInsideColor(g);
		g.fillOval(BORDER_WIDTH, BORDER_WIDTH, 
			2*(RADIUS-BORDER_WIDTH), 2*(RADIUS-BORDER_WIDTH));
		setTextColor(g);
		g.drawString(displayedName, RADIUS-strOffsetX, RADIUS+strOffsetY);
	}
	
	private void setBorderColor(Graphics g)
	{
		if (running)
			g.setColor(RUNNING_CLIENT_BORDER);
		else
			g.setColor(SLEEPING_CLIENT_BORDER);
	}
	
	private void setInsideColor(Graphics g)
	{
		if (running)
			g.setColor(RUNNING_CLIENT);
		else
			g.setColor(SLEEPING_CLIENT);
	}
	
	private void setTextColor(Graphics g)
	{
		if (running)
			g.setColor(RUNNING_CLIENT_TEXT);
		else
			g.setColor(SLEEPING_CLIENT_TEXT);
	}
	
	// returns true if (x,y) is inside of the circle
	public boolean intersects(int x, int y)
	{
		if ((x-this.x)*(x-this.x) + (y-this.y)*(y-this.y) < RADIUS*RADIUS)
			return true;
		else
			return false;
	}
	
	// change the position. positions off the visible screen (incl. negative
	// coordinates) are allowed
	public void moveTo(int x, int y)
	{
		this.x = x;
		this.y = y;
	}
	
	// returns true if (x,y) is in the border area of this node
	public boolean isOnBorder(int x, int y)
	{
		if (Math.pow(x-this.x, 2) + Math.pow(y-this.y, 2) > 
			Math.pow(RADIUS-BORDER_WIDTH, 2))
			return true;
		else
			return false;
	}
	
	// change the name of the node. it is assumend that the name is unique!
	public void setName(String name)
	{
		this.name = name;
		
		// force the image to be redrawn next time
		backupGraphic = null;
		backupImage = null;
	}
	
	// implies a change of the color
	public void setRunning()
	{
		this.running = true;
		
		// force the image to be redrawn next time
		backupGraphic = null;
		backupImage = null;
	}
	
	// some one-liners to get and set private fields...
	public boolean 	isRunning() 	{ return running; }
	
	public String 	getName() 		{ return name; }
	public String 	getClassName() 	{ return className; }
	public String 	getParameters() { return parameters; }
	public int		getX()			{ return x; }
	public int		getY()			{ return y; }
	public Vector	getLinks()		{ return links; }
	
	public void setClassName(String name)	{ this.className = name; }
	public void setClassPath(String classpath) { this.classpath = classpath; }
	public void setParameters(String param) { this.parameters = param; }
	public void setX(int x)					{ this.x = x; }
	public void setY(int y)					{ this.y = y; }
}