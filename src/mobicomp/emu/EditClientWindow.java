package mobicomp.emu;

/*
 * EditClientWindow
 *
 * this class offers a window to edit the properties of a client
 *
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class EditClientWindow extends JDialog implements ActionListener
{
	// the client object this window is editing
	private Client client;
	
	// default values for class name and parameters
	private static String defClassName;
	
	// the GUI elements
	private JLabel classLabel = new JLabel("Class name (incl. package): ");
	private JLabel paramLabel = new JLabel("Parameters for main(): ");
	private JLabel classpathLabel = new JLabel("Classpath: ");
	private JLabel nameLabel = new JLabel("Internal name: ");
	private JLabel posLabel = new JLabel("Position: ");
	private JLabel posXLabel = new JLabel("x: ");
	private JLabel posYLabel = new JLabel(" y: ");
	private JLabel runningLabel;
	
	private JTextField classField = new JTextField(15);
	private JTextField paramField = new JTextField(15);
	private JTextField classpathField = new JTextField(15);
	private JTextField nameField = new JTextField(15);
	private JTextField posXField = new JTextField(3);
	private JTextField posYField = new JTextField(3);
	
	private boolean canceled;	// flag indicating if the dialog was canceled
	
	private JButton okButton = new JButton("OK");
	private JButton cancelButton = new JButton("Cancel");
	
	// constructor
	public EditClientWindow(JFrame parent, Client client)
	{
		// create new window (modal!)
		super(parent, "Edit client", true);
		
		if (parent == null || client == null)
			throw new NullPointerException();
				
		this.client = client;
		this.canceled = true;
		
		initWindow();
	}
	
	// place GUI elements in the window
	private void initWindow()
	{
		// set the known attributes to the fields
		if (client.getClassName() == null)
		{
			if (defClassName != null)
				classField.setText(defClassName);
		}
		else
			classField.setText(client.getClassName());
		
		if (client.getParameters() != null)
			paramField.setText(client.getParameters());
		
		if (client.getClassPath() != null){
			classpathField.setText(client.getClassPath());
		}
		
		if (client.getName() != null)
			nameField.setText(client.getName());
			
		posXField.setText(Integer.toString(client.getX()));
		posYField.setText(Integer.toString(client.getY()));
		
		// some attributes (class name, ...) can not be changed when running
		if (client.isRunning())
		{
			classField.setEditable(false);
			paramField.setEditable(false);
			nameField.setEditable(false);
			classpathField.setEditable(false);
		}
		
		// add action listeners to the buttons
		okButton.addActionListener(this);
		cancelButton.addActionListener(this);
		
		// adjust running/not running text
		if (client.isRunning())
			runningLabel = new JLabel("This client is already running");
		else
			runningLabel = new JLabel("This client is not yet running");
		
		JPanel cp = new JPanel();
		cp.setLayout(new GridBagLayout());
		cp.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		GridBagConstraints cLabels = new GridBagConstraints();
		GridBagConstraints cFields = new GridBagConstraints();

		cLabels.weightx = 0.5;
		cLabels.anchor = GridBagConstraints.LINE_START;
		cLabels.gridx = 0;
		cLabels.gridy = 0;
		cp.add(classLabel, cLabels);
		
		cFields.weightx = 0.5;
		cFields.gridx = 1;
		cFields.gridy = 0;
		cp.add(classField, cFields);
		
		cLabels.gridy = 1;
		cp.add(paramLabel, cLabels);
		
		cFields.gridy = 1;
		cp.add(paramField, cFields);
		
		cLabels.gridy = 2;
		cp.add(classpathLabel,cLabels);
		
		cFields.gridy = 2;
		cp.add(classpathField,cFields);
		
		cLabels.gridy = 3;
		cp.add(nameLabel, cLabels);
		
		cFields.gridy = 3;
		cp.add(nameField, cFields);
		
		cLabels.gridy = 4;
		cp.add(posLabel, cLabels);

		// the panel containing the fields for entering x and y
		JPanel xyPanel = new JPanel();
		xyPanel.setLayout(new BoxLayout(xyPanel, BoxLayout.LINE_AXIS));
		xyPanel.add(posXLabel);
		xyPanel.add(posXField);
		xyPanel.add(Box.createRigidArea(new Dimension(20, 0)));
		xyPanel.add(posYLabel);
		xyPanel.add(posYField);
		
		cFields.gridy = 4;
		cp.add(xyPanel, cFields);

		cLabels.gridy = 5;
		cLabels.anchor = GridBagConstraints.CENTER;
		cLabels.ipady = 40;
		cLabels.weightx = 1.0;
		cLabels.gridwidth = 2;
		cp.add(runningLabel, cLabels);
		
		cLabels.gridy = 6;
		cLabels.ipady = 0;
		cLabels.weightx = 0.5;
		cLabels.gridwidth = 1;
		cp.add(okButton, cLabels);
		
		cLabels.gridx = 1;
		cp.add(cancelButton, cLabels);
		
		setContentPane(cp);
		
		// set default button
		getRootPane().setDefaultButton(okButton);
		
		// add a window listener to the window and show it
		WindowListener listener = new WindowAdapter()
		{
			public void windowClosing(WindowEvent event)
			{
				//this.hide();
			}
		};
		
		this.addWindowListener(listener);
		this.pack();
		this.setVisible(true);
	}
	
	// handles any user input
	public void actionPerformed(ActionEvent aevent)
	{
		Object source = aevent.getSource();
		
		if (source == okButton)
		{
			// apply changes
			
			if (!client.isRunning())
			{
				if (!classField.getText().equals(new String("")))
					client.setClassName(classField.getText());
				
			//	if (!paramField.getText().equals(new String("")))
					client.setParameters(paramField.getText());
				
				if (!nameField.getText().equals(new String("")) &&
					(!client.getName().equals(nameField.getText())))
				{
					String newname = nameField.getText();
					Graph graph = Emulator.getRef().getGraph();
					
					// name changed - check for duplicate names
					if (graph.getClient(newname) != null)
					{
						int counter = 2;
						
						do
						{
							newname = nameField.getText() + " (" + counter + ")";
							counter++;
						}
						while (graph.getClient(newname) != null);
					}
					client.setName(newname);
				}
			}

			
			client.setClassPath(classpathField.getText());
			
			
			if (!posXField.getText().equals(new String("")))
				client.setX(Integer.parseInt(posXField.getText()));

			if (!posYField.getText().equals(new String("")))
				client.setY(Integer.parseInt(posYField.getText()));

			// set the default values for next time
			defClassName = classField.getText();
			this.canceled = false;
			this.setVisible(false);
		}
		else if (source == cancelButton)
		{
			// discard any changes
			this.canceled = true;
			this.setVisible(false);
			
		}
		else
		{
			this.canceled = true;
			// ignore other actions
		}
	}
	
	public boolean isCanceled() {
		return canceled;
	}
}