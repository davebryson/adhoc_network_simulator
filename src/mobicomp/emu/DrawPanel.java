package mobicomp.emu;

/*
 * DrawPanel
 * 
 * this class has two tasks: it implements the interface Graph and it draws the
 * current graph in the GUI. the drawpanel is the left side of the main window
 *  
 */

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

import java.util.Vector;
import java.util.Iterator;

public class DrawPanel extends JPanel implements Graph {
	// minimal distance between two nodes (*r)
	// must be greater or equal to 3 + 2*sqrt(2)

	// public static final double MIN_DISTANCE = (3+2*Math.sqrt(2));
	public static final double MIN_DISTANCE = 7.5;

	// background color of the panel
	public static final Color BACKGROUND = Color.white;

	// parent of this panel
	private JFrame wnd;

	// reference to the Emulator
	private Emulator emu;

	// lists of all nodes and all (closed!) links.
	private Vector clients;

	private Vector links;

	// here we save the link we are currently drawing (the open link)
	private Link openLink = null;

	public DrawPanel(JFrame parent) {
		if (parent == null)
			throw new NullPointerException();

		this.wnd = parent;
		this.emu = Emulator.getRef();

		clients = new Vector();
		links = new Vector();

		initWindow();
	}

	// sets up the window
	private void initWindow() {
		MouseInputListener ml = new myMouseListener();
		this.addMouseListener(ml);
		this.addMouseMotionListener(ml);

		this.setPreferredSize(new Dimension(700, 700));
	}

	// returns the client that intersects with the coordiate (x,y) or null
	// otherwise
	public Client getClientAt(int x, int y) {
		for (Iterator iter = clients.iterator(); iter.hasNext();) {
			Client curElem = (Client) iter.next();

			if (curElem.intersects(x, y))
				return curElem;
		}

		return null;
	}

	// returns the link that intersects with the coordinate (x,y) or null
	// otherwise
	public Link getLinkEndingAt(int x, int y) {
		for (Iterator iter = links.iterator(); iter.hasNext();) {
			Link curLnk = (Link) iter.next();

			if (curLnk.hasEdgeAt(x, y)) {
				return curLnk;
			}
		}
		return null;
	}
	
	public Link getLinkIntersecting(int x, int y){
	    for (Iterator iter = links.iterator(); iter.hasNext();) {
			Link curLnk = (Link) iter.next();

			if (curLnk.intersects(x, y)) {
				return curLnk;
			}
		}
		return null;
	}

	// draw the graph
	public void paint(Graphics g) {
		this.setBackground(BACKGROUND);

		super.paint(g);

		// draw all nodes
		for (Iterator iter = clients.iterator(); iter.hasNext();) {
			((Client) iter.next()).draw(g, this);
		}

		// draw all closed links
		for (Iterator iter = links.iterator(); iter.hasNext();) {
			((Link) iter.next()).draw(g);
		}

		// draw the open link, if there is one
		if (openLink != null)
			openLink.draw(g);
	}

	// moves a client to another position. if it would intersect with another
	// client, the nearest non-intersection position is chosen
	public void moveClient(Client node, int x, int y) {
		if (node == null)
			throw new NullPointerException();

		// check for overlapping
		if (!overlaps(node, x, y)) {
			node.moveTo(x, y);
			return;
		}

		// we have an intersection and need to find the nearest point
		// without intersection. it get's very slow here...

		int distance = 1;

		while (true) {
			for (int i = 0; i < 2 * distance; i++) {
				if (!overlaps(node, x - distance + i, y - distance)) {
					node.moveTo(x - distance + i, y - distance);
					return;
				}

				if (!overlaps(node, x + distance, y - distance + i)) {
					node.moveTo(x + distance, y - distance + i);
					return;
				}

				if (!overlaps(node, x + distance - i, y + distance)) {
					node.moveTo(x + distance - i, y + distance);
					return;
				}

				if (!overlaps(node, x - distance, y + distance - i)) {
					node.moveTo(x - distance, y + distance - i);
					return;
				}
			}

			distance++;
		}

		// we will never reach this point
	}

	// check wheter or not the coordinate (x,y) is a position for the client
	// where it doesn't intersect with another client. this method is slow as
	// hell - for a big graph, this must be rewritten using a fancy data
	// structure
	private boolean overlaps(Client node, int x, int y) {
		for (Iterator iter = clients.iterator(); iter.hasNext();) {
			Client nc = (Client) iter.next();

			if ((nc != node)
					&& (Math.pow(x - nc.getX(), 2) + Math.pow(y - nc.getY(), 2) < MIN_DISTANCE
							* Math.pow(Client.RADIUS, 2)))
				return true;
		}

		return false;
	}

	/* ********************** from interface Graph ************************** */
	public void addClient(Client client) {
		if (client == null)
			throw new NullPointerException();

		moveClient(client, client.getX(), client.getY());
		clients.add(client);
		repaint();
	}

	// remove a client from the graph
	// 
	// NOTE: it is assumed that this client has NO links and NO open sockets !
	public void removeClient(Client client) {
		clients.remove(client);
		repaint();
	}

	// returns a client given its name or null if there is no client called name
	public Client getClient(String name) {
		for (Iterator iter = clients.iterator(); iter.hasNext();) {
			Client cur = (Client) iter.next();

			if (cur.getName().equals(name))
				return cur;
		}

		return null;
	}

	// returns a list of all names of the clients
	public Vector getClientNames() {
		Vector ret = new Vector();

		for (Iterator iter = clients.iterator(); iter.hasNext();)
			ret.add(((Client) iter.next()).getName());

		return ret;
	}

	// add a new client
	public Client createClient() {
		return new Client(emu.getNextNodeName(), 100, 100);
	}

	// add a new client at a default position
	public Client createClient(String name) {
		return new Client(name, 100, 100);
	}

	// add a new client
	public Client createClient(String name, String className, String params,
			String classpath) {
		return new Client(name, className, params, classpath, 100, 100);
	}

	// add a new client with a default name
	public Client createClient(int x, int y) {
		return new Client(emu.getNextNodeName(), x, y);
	}

	// add a new client
	public Client createClient(String name, int x, int y) {
		return new Client(name, x, y);
	}

	// add a new client
	public Client createClient(String name, String className, String params,
			String classpath, int x, int y) {
		return new Client(name, className, params, classpath, x, y);
	}

	public void addLink(Link link) {
		if (link == null)
			throw new NullPointerException();

		links.add(link);
		link.getStart().addLink(link);
		if (link.isBidirectional()) {
			link.getEnd().addLink(link);
		}
		repaint();
	}

	public void removeLink(Link link) {
		if (link == null)
			throw new NullPointerException();

		links.remove(link);
		link.getStart().removeLink(link);
		link.getEnd().removeLink(link);
		repaint();
	}

	public Link getLink(Client c1, Client c2) {
		if (c1 == null || c2 == null)
			throw new NullPointerException();

		for (Iterator iter = links.iterator(); iter.hasNext();) {
			Link curLink = (Link) iter.next();

			if (curLink.conntects(c1, c2))
				return curLink;
		}

		return null;
	}

	// create a new closed link
	public Link createLink(Client src, Client dest) {
		if (src == null || dest == null)
			throw new NullPointerException();

		return new Link(src, dest);
	}

	// create a new open link
	public Link createLink(Client src, int destx, int desty) {
		return new Link(src, destx, desty);
	}

	// return true if c1 and c2 are linked in the graph, false otherwise
	public boolean isLinked(Client c1, Client c2) {
		if (c1 == null || c2 == null)
			throw new NullPointerException();

		for (Iterator iter = c1.getLinks().iterator(); iter.hasNext();) {
			Link curLnk = (Link) iter.next();

			if ((curLnk.getStart() == c2) || (curLnk.getEnd() == c2)) {
				return true;
			}
		}

		// if unidirectional links exist...
		for (Iterator iter = c2.getLinks().iterator(); iter.hasNext();) {
			Link curLnk = (Link) iter.next();

			if ((curLnk.getStart() == c1) || (curLnk.getEnd() == c1)) {
				return true;
			}
		}

		return false;
	}

	public int getNumberOfClients() {
		return clients.size();
	}

	public int getNumberOfLinks() {
		return links.size();
	}

	public Vector getClients() {
		return clients;
	}

	public Vector getLinks() {
		return links;
	}

	// reset the graph (= remove all links and nodes)
	public void reset() {
		while (!clients.isEmpty()) {
			emu.removeClient((Client) clients.firstElement());
		}
	}

	// start all clients that are not running yet
	public void startAllClients() {
		boolean done = false;

		// iterate over all clients and try to start them
		for (Iterator iter = clients.iterator(); iter.hasNext();) {
			Client cur = (Client) iter.next();

			if (!cur.isRunning()) {
				// try to start it
				while (!emu.startClient(cur)) {
					// retry until it works
				}
			}
		}
	}

	/* ********************* end of interface Graph ************************* */

	// the mouse listener of the drawpanel
	private class myMouseListener implements MouseInputListener {
		// != null if the user is currently dragging a node around
		private Client selectedNode = null;

		public void mouseClicked(MouseEvent e) {
			if (e.getButton() == MouseEvent.BUTTON3) {
				// right click
			    Link selLink = null;
				Client selClient = getClientAt(e.getX(), e.getY());
				
				if (selClient == null){
				    selLink = getLinkIntersecting(e.getX(),e.getY());
				}

				//clicked an empty space -> new node
				if ((selClient == null) && (selLink == null)) {
					// add new element
					Client newClient = createClient(e.getX(), e.getY());
					EditClientWindow ecWnd = new EditClientWindow(wnd,
							newClient);
					if (!ecWnd.isCanceled()) {
						addClient(newClient);
					} else {
						emu.rewindNextNodeName();
					}

					repaint();
				} else if (selClient != null){
					// edit the client
					EditClientWindow ecWnd = new EditClientWindow(wnd, selClient);
					repaint();
				} else{
				   EditLinkPropWindow elw =  new EditLinkPropWindow(wnd,selLink);				   
				}
			} 
		}

		public void mouseEntered(MouseEvent e) {
			// we don't care about that
		}

		public void mouseExited(MouseEvent e) {
			// we don't care about that
		}

		public void mousePressed(MouseEvent e) {
			// only left clicks are allowed to create and move links
			if (e.getButton() == MouseEvent.BUTTON1) {

				selectedNode = getClientAt(e.getX(), e.getY());

				if (selectedNode != null) {
					// check whether we want to move the node or create a new
					// link starting at this node

					if (selectedNode.isOnBorder(e.getX(), e.getY())) {
						openLink = new Link(selectedNode, e.getX(), e.getY());
						selectedNode = null;
					}
				} else {
					// does the user want to move an existing link?
					openLink = getLinkEndingAt(e.getX(), e.getY());

					if (openLink != null) {
						removeLink(openLink);
						openLink.setOpen(openLink.getPointNear(e.getX(), e
								.getY()));
					}
				}
			}
		}

		public void mouseReleased(MouseEvent e) {
		    //if we moved a node forget about it
		    selectedNode = null;
		    
			if (openLink != null) {
				// we need to tell the link his new endpoint
				Client end = getClientAt(e.getX(), e.getY());

				if (end == null) {
					// delete the open link - it will be GC'd
					openLink = null;
				} else {
					openLink.setEndpoint(end);

					if ((openLink.getStart() != openLink.getEnd())
							&& (!isLinked(openLink.getStart(), openLink
									.getEnd())))
						addLink(openLink);

					openLink = null;
				}

				repaint();
			}

		}

		public void mouseDragged(MouseEvent e) {
			if (selectedNode != null) {
				moveClient(selectedNode, e.getX(), e.getY());
				repaint();
			} else if (openLink != null) {
				openLink.setEndpoint(e.getX(), e.getY());
				repaint();
			}
		}

		public void mouseMoved(MouseEvent e) {
			// we don't care about that
		}
	}
}