package mobicomp.emu;

/*
 * ControlPanel
 *
 * the control panel is the right side of the main window containing a number of
 * buttons that allow the user to edit the graph, start clients and much more
 *
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import java.io.File;
import java.util.Vector;

public class ControlPanel extends JPanel implements ActionListener
{
	// currently open graph file
	private File curFile;
	
	// reference to the Emulator
	private Emulator emu;
	
	// parent window
	private JFrame wnd;
	
	// the GUI components
	private JLabel packetLabel = new JLabel("# of packets sent:");
	private JLabel packetCountLabel = new JLabel("0");
	
	private JLabel clientLabel = new JLabel("# of clients:");
	private JLabel clientCountLabel = new JLabel("0");
	
	private JLabel linkLabel = new JLabel("# of links:");
	private JLabel linkCountLabel = new JLabel("0");
	
	private JButton addButton = new JButton("Add Client");
	private JButton removeButton = new JButton("Remove Client");
	private JButton editButton = new JButton("Edit Links");
	private JButton startButton = new JButton("Start Clients");
	
	private JButton newButton = new JButton("New Graph");
	private JButton loadButton = new JButton("Load Graph");
	private JButton saveButton = new JButton("Save Graph");
	private JButton saveAsButton = new JButton("Save Graph As");
	
	private JButton settingsButton = new JButton("Settings");
	private JButton showWndButton = new JButton("Hide Output Wnd");
	private JButton exitButton = new JButton("Exit");
	
	// constructor
	public ControlPanel(JFrame parent)
	{
		if (parent == null)
			throw new NullPointerException();
		
		this.wnd = parent;
		
		emu = Emulator.getRef();
		initPanel();
	}
	
	// place GUI elements in the window
	private void initPanel()
	{
		//this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.setLayout(new BorderLayout());
		
		// add action listeners to the buttons
		addButton.addActionListener(this);
		removeButton.addActionListener(this);
		editButton.addActionListener(this);
		startButton.addActionListener(this);
		newButton.addActionListener(this);
		loadButton.addActionListener(this);
		saveButton.addActionListener(this);
		saveAsButton.addActionListener(this);
		settingsButton.addActionListener(this);
		showWndButton.addActionListener(this);
		exitButton.addActionListener(this);

		// Statistics Area
		JPanel statsPane = new JPanel();
		statsPane.setBorder(BorderFactory.createTitledBorder("Statistics"));
		statsPane.setLayout(new GridBagLayout());
		
		GridBagConstraints cLeft = new GridBagConstraints();
		GridBagConstraints cRight = new GridBagConstraints();

		cLeft.gridx = 0;
		cLeft.gridy = 0;
		cLeft.weightx = 1.0;
		cLeft.anchor = GridBagConstraints.LINE_START;
		statsPane.add(clientLabel, cLeft);
		
		cRight.gridx = 1;
		cRight.gridy = 0;
		cRight.anchor = GridBagConstraints.LINE_END;
		statsPane.add(clientCountLabel, cRight);
		
		cLeft.gridy = 1;
		statsPane.add(linkLabel, cLeft);
		
		cRight.gridy = 1;
		statsPane.add(linkCountLabel, cRight);
		
		cLeft.gridy = 2;
		statsPane.add(packetLabel, cLeft);
		
		cRight.gridy = 2;
		statsPane.add(packetCountLabel, cRight);
		
		// Edit Area
		JPanel editPane = new JPanel();
		editPane.setBorder(BorderFactory.createTitledBorder("Edit Graph manually"));
		editPane.setLayout(new GridLayout(0, 1, 0, 5));
		
		editPane.add(addButton);
		editPane.add(removeButton);
		editPane.add(editButton);
		editPane.add(startButton);
		
		// Load/Save Area
		JPanel loadSavePane = new JPanel();
		loadSavePane.setBorder(BorderFactory.createTitledBorder("Load/Save Graph"));
		loadSavePane.setLayout(new GridLayout(0, 1, 0, 5));
		
		loadSavePane.add(newButton);
		loadSavePane.add(loadButton);
		loadSavePane.add(saveButton);
		loadSavePane.add(saveAsButton);
		
		// Misc Area
		JPanel miscPane = new JPanel();
		miscPane.setBorder(BorderFactory.createTitledBorder("Miscellaneous"));
		miscPane.setLayout(new GridLayout(0, 1, 0, 5));
		
		miscPane.add(settingsButton);
		miscPane.add(showWndButton);
		miscPane.add(exitButton);
		
		// add all panels to a temporary panel which is the added in the main 
		// panel. if the panels were added directly to the main panel, the 
		// remaining space would not be at the end but distributed over the
		// panels
		JPanel tmp = new JPanel();
		tmp.setLayout(new BoxLayout(tmp, BoxLayout.Y_AXIS));
		
		// add all panels to the main panel
		tmp.add(statsPane);
		tmp.add(Box.createRigidArea(new Dimension(0,10)));
		tmp.add(editPane);
		tmp.add(Box.createRigidArea(new Dimension(0,10)));
		tmp.add(loadSavePane);
		tmp.add(Box.createRigidArea(new Dimension(0,10)));
		tmp.add(miscPane);
		
		// add the temporary panel to the main panel (in the northern part)
		this.add(tmp, BorderLayout.NORTH);
		
		CounterUpdateThread dummy = new CounterUpdateThread();
		dummy.start();
	}
	
	// handles any user input
	public void actionPerformed(ActionEvent aevent)
	{
		Object source = aevent.getSource();
		Graph graph = emu.getGraph();
		
		if (source == addButton)
		{
			// add a program
			Client newClient = graph.createClient();
			EditClientWindow ecWnd = new EditClientWindow(wnd, newClient);
			if (!ecWnd.isCanceled()){
				graph.addClient(newClient); // add the new client
			} else {
				emu.rewindNextNodeName(); // rewind the next node id by 1
			}
		} 
		else if (source == removeButton)
		{
			Vector clients = graph.getClientNames();
			
			if (clients.isEmpty())
				return;
			
			// ask user which client to delete
			Object[] possibilities = clients.toArray();
			
			String s = (String)JOptionPane.showInputDialog(wnd,
				"Remove which client?",
				"Remove client",
				JOptionPane.PLAIN_MESSAGE,
				null,
				possibilities,
				possibilities[0]);
			
			if (s != null)
				emu.removeClient(graph.getClient(s));
		}
		else if (source == editButton)
		{
			if (emu.getGraph().getNumberOfClients() > 0)
			{
				// ask user which link to edit
				EditLinkWindow editLnk = new EditLinkWindow(wnd);
			}
		}
		else if (source == startButton)
		{
			// start all clients
			graph.startAllClients();
		}
		else if (source == newButton)
		{
			// create a new empty graph
			graph.reset();
		}
		else if (source == loadButton)
		{
			emu.loadGraph();
		}
		else if (source == saveButton)
		{
			// save graph to current file
			emu.saveGraph();
		}
		else if (source == saveAsButton)
		{
			// save graph as new file
			emu.saveGraphAs();
		}
		else if (source == settingsButton)
		{
			new OptionsWindow(wnd);
		}
		else if (source == showWndButton)
		{
			if (showWndButton.getText().equals("Show output Wnd"))
			{
				emu.setOutputWindowVisibility(true);
				showWndButton.setText("Hide output Wnd");
			}
			else
			{
				emu.setOutputWindowVisibility(false);
				showWndButton.setText("Show output Wnd");
			}
		}
		else if (source == exitButton)
		{
			System.exit(0);
		}
		else
		{
			// any other source - ignore it
		}
	}
	
	// thread to update the counters in the statistics area every SLEEPTIME ms
	private class CounterUpdateThread extends Thread
	{
	    private int SLEEPTIME = 1000;
		private boolean stopped = true;
		
		public CounterUpdateThread()
		{
			stopped = false;
		}
		
		public void run()
		{
			while(!stopped)
			{
				Graph graph = emu.getGraph();
				
				if (graph != null)
				{
					// update the GUI
					clientCountLabel.setText(Integer.toString(graph.getNumberOfClients()));
					packetCountLabel.setText(Integer.toString(emu.getPacketCount()));
					linkCountLabel.setText(Integer.toString(graph.getNumberOfLinks()));
					Emulator.getRef().redrawGraph();
				}
				
				// sleep some time
				try 
				{
					Thread.sleep(SLEEPTIME);
				} 
				catch (InterruptedException e) 
				{
					// do nothing
				}
			}
		}
		
		public void stopThread()
		{
			stopped = true;
		}
	}
}