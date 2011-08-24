package mobicomp.emu;

/*
 * EditLinkWindow
 *
 * this class offers a window to add and remove links
 *
 */

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

import java.util.Vector;
import java.util.Iterator;

public class EditLinkWindow extends JDialog implements ActionListener, ListSelectionListener
{
	// parent window
	private JFrame parent;
	
	// the GUI elements (labels, buttons, ...)
	private JLabel selectLabel = new JLabel("Select link");
	private JLabel arrowLabel = new JLabel(" <==> ");
	
	private JButton addButton = new JButton("Add link");
	private JButton removeButton = new JButton("Remove link");
	private JButton editButton = new JButton("Edit link");
	private JButton closeButton = new JButton("Close");
	
	private JList fromList;
	private JList toList;
	
	private DefaultListModel fromListModel;
	private DefaultListModel toListModel;

	// constructor
	public EditLinkWindow(JFrame parent)
	{
		// create new dialog window (modal!)
		super(parent, "Edit links", true);
		
		this.parent = parent;
		initWindow();
	}
	
	// place GUI elements in the window
	private void initWindow()
	{
		// set up the two lists
		Vector clientNames = Emulator.getRef().getGraph().getClientNames();
		
		fromListModel = new DefaultListModel();
		for (Iterator iter = clientNames.iterator(); iter.hasNext(); )
			fromListModel.addElement((String) iter.next());
			
		toListModel = new DefaultListModel();
		for (Iterator iter = clientNames.iterator(); iter.hasNext(); )
			toListModel.addElement((String) iter.next());
			
		fromList = new JList(fromListModel);
		fromList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		fromList.setSelectedIndex(0);
		fromList.setVisibleRowCount(5);
		fromList.setFixedCellWidth(100);
		
		JScrollPane fromListScrollPane = new JScrollPane(fromList);
		fromListScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

		toList = new JList(toListModel);
		toList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		toList.setSelectedIndex(0);
		toList.setVisibleRowCount(5);
		toList.setFixedCellWidth(100);
		
		JScrollPane toListScrollPane = new JScrollPane(toList);
		toListScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		// add listerner to the lists
		fromList.addListSelectionListener(this);
		toList.addListSelectionListener(this);
		
		// add action listeners to the buttons
		addButton.addActionListener(this);
		removeButton.addActionListener(this);
		editButton.addActionListener(this);
		closeButton.addActionListener(this);
		
		JPanel cp = new JPanel();
		cp.setLayout(new GridBagLayout());
		cp.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		GridBagConstraints c = new GridBagConstraints();
		
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 3;
		c.weightx = 1.0;
		c.ipady = 20;
		c.anchor = GridBagConstraints.LINE_START;
		cp.add(selectLabel, c);
		
		c.gridy = 1;
		c.gridwidth = 1;
		c.weightx = 0.45;
		c.ipady = 0;
		c.anchor = GridBagConstraints.CENTER;
		cp.add(fromListScrollPane, c);
		
		c.gridx = 1;
		c.weightx = 0.1;
		cp.add(arrowLabel, c);
		
		c.gridx = 2;
		c.weightx = 0.45;
		cp.add(toListScrollPane, c);
		
		// the panel with the buttons "add link" and "remove link"
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(0, 3));
		buttonPanel.add(addButton);
		buttonPanel.add(editButton);
		buttonPanel.add(removeButton);
		
		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 3;
		c.weightx = 1.0;
		c.insets = new Insets(10, 0, 0, 0);
		cp.add(buttonPanel, c);
		
		c.gridx = 1;
		c.gridy = 3;
		c.gridwidth = 1;
		c.weightx = 0.0;
		cp.add(closeButton, c);
		
		updateButtonStatus();
		
		// set default button
		getRootPane().setDefaultButton(closeButton);
		
		setContentPane(cp);
		
		// add the window listener and show the window
		WindowListener listener = new WindowAdapter()
		{
			public void windowClosing(WindowEvent event)
			{
				// this.hide();
			}
		};
		
		this.addWindowListener(listener);
		this.pack();
		this.setVisible(true);
	}
	
	// update enabled/disabled status of the buttons
	private void updateButtonStatus()
	{
		Graph graph = Emulator.getRef().getGraph();
		
		// get the status of both lists
		Client fromClient = graph.getClient((String) fromList.getSelectedValue());
		Client toClient = graph.getClient((String) toList.getSelectedValue());
		
		if (fromClient.equals(toClient))
		{
			// disable all buttons
			addButton.setEnabled(false);
			editButton.setEnabled(false);
			removeButton.setEnabled(false);
		}
		else
		{
			if (graph.isLinked(fromClient, toClient))
			{
				// disable add button
				addButton.setEnabled(false);
				editButton.setEnabled(true);
				removeButton.setEnabled(true);
			}
			else
			{
				// disable edit and remove button
				addButton.setEnabled(true);
				editButton.setEnabled(false);
				removeButton.setEnabled(false);
			}
		}
	}
	
	// from interface ListSelectionListener: update button status on list changes
	public void valueChanged(ListSelectionEvent e)
	{
		updateButtonStatus();
	}
	
	// from interface ActionListener: handles any user input
	//
	// NOTE: checks whether add/edit/remove-operations are allowed are done even
	// though the "wrong" buttons should be disabled
	public void actionPerformed(ActionEvent aevent)
	{
		Object source = aevent.getSource();
		
		Graph graph = Emulator.getRef().getGraph();
		
		if (source == addButton)
		{
			// add a new link
			Client fromClient = graph.getClient((String) fromList.getSelectedValue());
			Client toClient = graph.getClient((String) toList.getSelectedValue());
			
			if (!fromClient.equals(toClient) &&
				!graph.isLinked(fromClient, toClient))
			{
				// create new link
				Link newLnk = graph.createLink(fromClient, toClient);
				graph.addLink(newLnk);
			}
		}
		else if (source == removeButton)
		{
			Client fromClient = graph.getClient((String) fromList.getSelectedValue());
			Client toClient = graph.getClient((String) toList.getSelectedValue());
			
			if (graph.isLinked(fromClient, toClient))
			{
				Link remLnk = graph.getLink(fromClient, toClient);
				graph.removeLink(remLnk);
			}
		}
		else if (source == editButton)
		{
			Client fromClient = graph.getClient((String) fromList.getSelectedValue());
			Client toClient = graph.getClient((String) toList.getSelectedValue());
			
			if (!graph.isLinked(fromClient, toClient))
			{
				// user wants to edit the properties of an inexisting link...
				JOptionPane.showMessageDialog(parent, 
					"There is no link to edit",	"Error", 
					JOptionPane.ERROR_MESSAGE);
					
				return;
			}
			
			Link editLink = graph.getLink(fromClient, toClient);

			this.setVisible(false);
			EditLinkPropWindow editWnd = new EditLinkPropWindow(parent, editLink);
		}
		else if (source == closeButton)
		{
			// close the window
			this.setVisible(false);
		}
		else
		{
			// ignore any other actions
		}
		
		updateButtonStatus();
	}
}