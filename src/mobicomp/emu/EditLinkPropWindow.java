package mobicomp.emu;

/*
 * EditLinkPropWindow
 *
 * this class offers a window to edit the properties of a link
 *
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import java.util.Vector;
import java.util.Iterator;

public class EditLinkPropWindow extends JDialog implements ActionListener
{
	// parent window
	private JFrame parent;
	
	// the link to edit
	private Link link;
	
	// reference to the Emulator class
	private static final Emulator emu = Emulator.getRef();
	
	// true when the user pressed OK, false when pressed Cancel
	private boolean okpressed = false;
	
	// the GUI elements for the delay settings
	JRadioButton noDelay = new JRadioButton();
	JRadioButton constDelay = new JRadioButton();
	JRadioButton distDelay = new JRadioButton();
	ButtonGroup delayGroup;
	JLabel noDelayLabel = new JLabel("no delay");
	JLabel constDelayLabel = new JLabel("constant delay ");
	JLabel constDelayMsLabel = new JLabel(" ms");
	JLabel distDelayLabel1 = new JLabel("delay according to distance between");
	JLabel distDelayLabel2 = new JLabel("nodes. Use factor ");
	JTextField constDelayField = new JTextField(5);
	JTextField distDelayField = new JTextField(5);
	JButton delayButton = new JButton("Apply delay settings to all links");
	
	// the GUI elements for the delay settings
	JRadioButton noError = new JRadioButton();
	JRadioButton constError = new JRadioButton();
	JRadioButton determError = new JRadioButton();
	JLabel noErrorLabel = new JLabel("no error");
	JLabel constErrorLabel = new JLabel("constant error probability ");
	JLabel constErrorPercLabel = new JLabel("%");
	JLabel determErrorLabel1 = new JLabel("deterministic error pattern");
	JLabel determErrorLabel2 = new JLabel("(use + for success and - for error)");
	JTextField constErrorField = new JTextField(3);
	JTextField determErrorPatternField = new JTextField(20);
	JButton errorButton = new JButton("Apply error settings to all links");
	
	// the GUI elements for the direction settings
	
	JLabel startEndLabel = new JLabel();
	JLabel endStartLabel  = new JLabel();
	JLabel bidirectionalLabel = new JLabel();
	JRadioButton startEndButton = new JRadioButton();
	JRadioButton endStartButton = new JRadioButton();
	JRadioButton bidirectionalButton = new JRadioButton();
	
	
	
	// the two buttons OK and Cancel
	JButton okButton = new JButton("OK");
	JButton cancelButton = new JButton("Cancel");
	
	
	// constructor
	public EditLinkPropWindow(JFrame parent, Link link)
	{
		// create new window (modal!)
		super(parent, "Edit link properties", true);
		
		if (parent == null || link == null)
			throw new NullPointerException();
		
		this.parent = parent;
		this.link = link;
		
		initWindow();
	}
	
	public boolean okPressed()
	{
		return okpressed;
	}
	
	// write all changes to the link
	public void doUpdate()
	{
		int ms;
		float factor, prob;
		String pattern;
		
		try
		{
			ms = Integer.parseInt(constDelayField.getText());
			factor = Float.parseFloat(distDelayField.getText());
			prob = Float.parseFloat(constErrorField.getText());
			pattern = determErrorPatternField.getText();
		}
		catch (NumberFormatException e)
		{
			// this should not happen - wrong inputs are already handled by
			// the window
			throw new RuntimeException("ERROR: Internal error while updating");
		}
		
		link.setConstDelayMs(ms);
		link.setConstErrorProb(prob);
		link.setDelayFactor(factor);
		link.setErrorPattern(pattern);
		
		if (noDelay.isSelected())
			link.setDelayType(Link.NO_DELAY);
		else if (constDelay.isSelected())
			link.setDelayType(Link.CONST_DELAY);
		else if (distDelay.isSelected())
			link.setDelayType(Link.DIST_DELAY);
			
		if (noError.isSelected())
			link.setErrorType(Link.NO_ERROR);
		else if (constError.isSelected())
			link.setErrorType(Link.CONST_ERROR);
		else if (determError.isSelected())
			link.setErrorType(Link.DETERM_ERROR);
		
		// if the link is now set to be bidirectional but was unidirectional before
		// add the link to the list of links at the end node
		if (bidirectionalButton.isSelected() && !link.isBidirectional()){
			link.setBidirectional(true);
			link.getEnd().addLink(link);
		} else if (startEndButton.isSelected() && link.isBidirectional()){
			link.setBidirectional(false);
			link.getEnd().removeLink(link);
		} else if (endStartButton.isSelected()){
			link.getStart().removeLink(link);
			Client newStart = link.getEnd();
			link.setEnd(link.getStart());
			link.setStart(newStart);
			if (link.isBidirectional()){
				link.setBidirectional(false);
			} else {
				link.getStart().addLink(link);
			}
		}
		emu.redrawGraph();
	}
	
	// place GUI elements in the window
	private void initWindow()
	{
		JPanel cp = new JPanel();
		cp.setLayout(new BoxLayout(cp, BoxLayout.Y_AXIS));

	// Delay settings
		JPanel delayPanel = new JPanel();
		delayPanel.setBorder(BorderFactory.createTitledBorder("Delay settings"));
		delayPanel.setLayout(new GridBagLayout());
		
		ButtonGroup delayGroup = new ButtonGroup();
		delayGroup.add(noDelay);
		delayGroup.add(constDelay);
		delayGroup.add(distDelay);
		
		// select the correct radio button
		switch (link.getDelayType())
		{
			case Link.NO_DELAY:
				noDelay.setSelected(true);
				break;
				
			case Link.CONST_DELAY:
				constDelay.setSelected(true);
				break;
				
			case Link.DIST_DELAY:
				distDelay.setSelected(true);
				break;
				
			default:
				throw new RuntimeException("ERROR: Illegal delay type");
		}
		
		// fill in the fields
		constDelayField.setText(Integer.toString(link.getConstDelayMs()));
		distDelayField.setText(Float.toString(link.getDelayFactor()));
		
		GridBagConstraints radioCon = new GridBagConstraints();
		GridBagConstraints textCon = new GridBagConstraints();
		
		// radio button for "no delay"
		radioCon.gridx = 0;
		radioCon.gridy = 0;
		radioCon.weightx = 0.1;
		delayPanel.add(noDelay, radioCon);
		
		// text for "no delay"
		JPanel noDelayPanel = new JPanel();
		noDelayPanel.setLayout(new BoxLayout(noDelayPanel, BoxLayout.X_AXIS));
		noDelayLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		noDelayPanel.add(noDelayLabel);
		
		textCon.gridx = 1;
		textCon.gridy = 0;
		textCon.weightx = 1.0;
		textCon.anchor = GridBagConstraints.LINE_START;
		delayPanel.add(noDelayPanel, textCon);
		
		// radio button for "constant delay"
		radioCon.gridy = 1;
		radioCon.insets = new Insets(8, 0, 0, 0);
		delayPanel.add(constDelay, radioCon);
		
		// text and input field for "constant delay"
		JPanel constDelayPanel = new JPanel();
		constDelayPanel.setLayout(new BoxLayout(constDelayPanel, BoxLayout.X_AXIS));
		constDelayPanel.add(constDelayLabel);
		constDelayPanel.add(constDelayField);
		constDelayPanel.add(constDelayMsLabel);
		
		textCon.gridy = 1;
		textCon.insets = new Insets(8, 0, 0, 0);
		delayPanel.add(constDelayPanel, textCon);
		
		// radio button for "distance based delay"
		radioCon.gridy = 2;
		radioCon.gridheight = 2;
		delayPanel.add(distDelay, radioCon);
		
		// text and input field for "distance based delay"
		JPanel distDelayPanel1 = new JPanel();
		distDelayPanel1.setLayout(new BoxLayout(distDelayPanel1, BoxLayout.X_AXIS));
		distDelayPanel1.add(distDelayLabel1);
		
		textCon.gridy = 2;
		delayPanel.add(distDelayPanel1, textCon);
		
		JPanel distDelayPanel2 = new JPanel();
		distDelayPanel2.setLayout(new BoxLayout(distDelayPanel2, BoxLayout.X_AXIS));
		distDelayPanel2.add(distDelayLabel2);
		distDelayPanel2.add(distDelayField);
		
		textCon.gridy = 3;
		textCon.insets = new Insets(0,0,0,0);
		delayPanel.add(distDelayPanel2, textCon);
		
		// the button "apply to all"
		radioCon.gridy = 4;
		radioCon.gridheight = 1;
		radioCon.gridwidth = 2;
		radioCon.weightx = 0.0;
		radioCon.insets = new Insets(15,0,0,0);
		delayButton.addActionListener(this);
		delayPanel.add(delayButton, radioCon);
	// end of delay settings

	// Error settings
		JPanel errorPanel = new JPanel();
		errorPanel.setBorder(BorderFactory.createTitledBorder("Error settings"));
		errorPanel.setLayout(new GridBagLayout());
		
		ButtonGroup errorGroup = new ButtonGroup();
		errorGroup.add(noError);
		errorGroup.add(constError);
		errorGroup.add(determError);
		
		// select the correct radio button
		switch (link.getErrorType())
		{
			case Link.NO_ERROR:
				noError.setSelected(true);
				break;
				
			case Link.CONST_ERROR:
				constError.setSelected(true);
				break;
				
			case Link.DETERM_ERROR:
				determError.setSelected(true);
				break;
				
			default:
				throw new RuntimeException("ERROR: Illegal error type");
		}
		
		// fill in the fields
		constErrorField.setText(Float.toString(link.getConstErrorProb()));
		determErrorPatternField.setText(link.getErrorPattern());
		
		// use the same constraints as above
		
		// radio button for "no delay"
		radioCon.gridx = 0;
		radioCon.gridy = 0;
		radioCon.insets = new Insets(0,0,0,0);
		radioCon.weightx = 0.1;
		radioCon.gridwidth = 1;
		errorPanel.add(noError, radioCon);
		
		// text for "no error"
		JPanel noErrorPanel = new JPanel();
		noErrorPanel.setLayout(new BoxLayout(noErrorPanel, BoxLayout.X_AXIS));
		noErrorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		noErrorPanel.add(noErrorLabel);
		
		textCon.gridx = 1;
		textCon.gridy = 0;
		textCon.insets = new Insets(0,0,0,0);
		textCon.weightx = 1.0;
		textCon.anchor = GridBagConstraints.LINE_START;
		errorPanel.add(noErrorPanel, textCon);
		
		// radio button for "constant error"
		radioCon.gridy = 1;
		radioCon.insets = new Insets(8, 0, 0, 0);
		errorPanel.add(constError, radioCon);
		
		// text and input field for "constant error"
		JPanel constErrorPanel = new JPanel();
		constErrorPanel.setLayout(new BoxLayout(constErrorPanel, BoxLayout.X_AXIS));
		constErrorPanel.add(constErrorLabel);
		constErrorPanel.add(constErrorField);
		constErrorPanel.add(constErrorPercLabel);
		
		textCon.gridy = 1;
		textCon.insets = new Insets(8, 0, 0, 0);
		errorPanel.add(constErrorPanel, textCon);
		
		// radio button for "error pattern"
		radioCon.gridy = 2;
		radioCon.gridheight = 3;
		errorPanel.add(determError, radioCon);
		
		// text and input field for "distance based delay"
		JPanel determErrorPanel1 = new JPanel();
		determErrorPanel1.setLayout(new BoxLayout(determErrorPanel1, BoxLayout.X_AXIS));
		determErrorPanel1.add(determErrorLabel1);
		
		textCon.gridy = 2;
		errorPanel.add(determErrorPanel1, textCon);
		
		JPanel determErrorPanel2 = new JPanel();
		determErrorPanel2.setLayout(new BoxLayout(determErrorPanel2, BoxLayout.X_AXIS));
		determErrorPanel2.add(determErrorLabel2);
		
		textCon.gridy = 3;
		textCon.insets = new Insets(0,0,0,0);
		errorPanel.add(determErrorPanel2, textCon);
		
		JPanel determErrorPanel3 = new JPanel();
		determErrorPanel3.setLayout(new BoxLayout(determErrorPanel3, BoxLayout.X_AXIS));
		determErrorPanel3.add(determErrorPatternField);
		
		textCon.gridy = 4;
		errorPanel.add(determErrorPanel3, textCon);
		
		// the button "apply to all"
		radioCon.gridy = 5;
		radioCon.gridheight = 1;
		radioCon.gridwidth = 2;
		radioCon.weightx = 0.0;
		radioCon.insets = new Insets(15,0,0,0);
		errorButton.addActionListener(this);
		errorPanel.add(errorButton, radioCon);
	// end of error settings	
		
		
		
		
	// start direction settings
		
		JPanel directionPanel = new JPanel();
		directionPanel.setBorder(BorderFactory.createTitledBorder("Direction settings"));
		directionPanel.setLayout(new GridBagLayout());

		ButtonGroup directionGroup = new ButtonGroup();
		directionGroup.add(startEndButton);
		directionGroup.add(endStartButton);
		directionGroup.add(bidirectionalButton);
		
		// select the correct radio button
		if (link.isBidirectional()){
			bidirectionalButton.setSelected(true);
		} else{
			startEndButton.setSelected(true);
		}
		
		// fill in the fields
		startEndLabel.setText(link.getStart().getName() + " ==> " + link.getEnd().getName());
		endStartLabel.setText(link.getStart().getName() + " <== " + link.getEnd().getName());
		bidirectionalLabel.setText(link.getStart().getName() + " <=> " + link.getEnd().getName());

		
		// use the same constraints as above
		
		// radio button for "start --> end"
		radioCon.gridx = 0;
		radioCon.gridy = 0;
		radioCon.weightx = 0.1;
		radioCon.gridwidth = 1;
		radioCon.insets = new Insets(0,0,0,0);
		directionPanel.add(startEndButton, radioCon);
		
		// text for "start --> end"
		JPanel startEndPanel = new JPanel();
		startEndPanel.setLayout(new BoxLayout(startEndPanel, BoxLayout.X_AXIS));
		startEndLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		startEndPanel.add(startEndLabel);
		
		textCon.gridx = 1;
		textCon.gridy = 0;
		textCon.weightx = 1.0;
		textCon.insets = new Insets(0,0,0,0);
		textCon.anchor = GridBagConstraints.LINE_START;
		directionPanel.add(startEndPanel, textCon);
		
		// radio button for "end --> start"
		radioCon.gridy = 1;
		directionPanel.add(endStartButton, radioCon);
		
		// text and input field for "end --> start"
		JPanel endStartPanel = new JPanel();
		endStartPanel.setLayout(new BoxLayout(endStartPanel, BoxLayout.X_AXIS));
		endStartPanel.add(endStartLabel);
		
		textCon.gridy = 1;
		directionPanel.add(endStartPanel, textCon);
		
		// radio button for " A <--> B"
		radioCon.gridy = 2;
		directionPanel.add(bidirectionalButton, radioCon);
		
		JPanel bidirectionalPanel = new JPanel();
		bidirectionalPanel.setLayout(new BoxLayout(bidirectionalPanel, BoxLayout.X_AXIS));
		bidirectionalPanel.add(bidirectionalLabel);
		
		textCon.gridy = 2;
		directionPanel.add(bidirectionalPanel, textCon);

		
		
		// end direction settings	
		
		// the buttons
		okButton.addActionListener(this);
		cancelButton.addActionListener(this);
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);

		cp.add(delayPanel);
		cp.add(Box.createRigidArea(new Dimension(0,10)));
		cp.add(errorPanel);
		cp.add(Box.createRigidArea(new Dimension(0,10)));
		cp.add(directionPanel);
		cp.add(Box.createRigidArea(new Dimension(0,20)));
		cp.add(buttonPanel);
		
		// set default button
		getRootPane().setDefaultButton(okButton);
		
		this.setContentPane(cp);
		
		//this.addWindowListener(listener);
		this.pack();
		this.setVisible(true);
	}
	
	private boolean checkIfInputIsLegal()
	{
		try
		{
			int ms = Integer.parseInt(constDelayField.getText());
			
			if (ms < 0)
				throw new NumberFormatException();
		}
		catch (NumberFormatException e)
		{
			JOptionPane.showMessageDialog(parent, 
				"Please enter a positive integer as constant delay",
				"Illegal input", 
				JOptionPane.ERROR_MESSAGE);
				
			return false;
		}
		
		try
		{
			float fact = Float.parseFloat(distDelayField.getText());
			
			if (fact < 0)
				throw new NumberFormatException();
		}
		catch (NumberFormatException e)
		{
			JOptionPane.showMessageDialog(parent, 
				"Please enter a positive float as delay factor",
				"Illegal input", 
				JOptionPane.ERROR_MESSAGE);
				
			return false;
		}
		
		try
		{
			float prob = Float.parseFloat(constErrorField.getText());
			
			if ((prob < 0) || (prob > 100))
				throw new NumberFormatException();
		}
		catch (NumberFormatException e)
		{
			JOptionPane.showMessageDialog(parent, 
				"Please enter a float between 0 and 100 as error probability",
				"Illegal input", 
				JOptionPane.ERROR_MESSAGE);
				
			return false;
		}
		
		String pattern = determErrorPatternField.getText();
		
		for (int i=0; i<pattern.length(); i++)
		{
			if ((pattern.charAt(i) != '+') && (pattern.charAt(i) != '-'))
			{
				JOptionPane.showMessageDialog(parent, 
					"Please use only '+' and '-' without spaces to\n" +
					"specify the error pattern",
					"Illegal input", 
					JOptionPane.ERROR_MESSAGE);
					
				return false;
			}	
		}
		
		return true;
	}
	
	// handles any user input
	public void actionPerformed(ActionEvent aevent)
	{
		Object source = aevent.getSource();
		
		if (source == okButton)
		{
			// check if input is legal
			if (!checkIfInputIsLegal())
				return;
			
			// everything seems to be ok
			okpressed = true;
			// do the update of the link
			doUpdate();
			this.setVisible(false);
		}
		else if (source == cancelButton)
		{
			okpressed = false;
			this.setVisible(false);
		}
		else if (source == delayButton)
		{
			if (!checkIfInputIsLegal())
			{
				return;
			}
			
			// check if the user really want to do this
			if (JOptionPane.showConfirmDialog(parent, 
					"Are you sure you want to apply these delay\n" +
					"settings to all links",
					"Apply to all links?", 
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE)
				
				== JOptionPane.YES_OPTION)
			{
				// apply delay settings to all links
				Vector allLinks = emu.getGraph().getLinks();
				
				for (Iterator iter=allLinks.iterator(); iter.hasNext(); )
				{
					int ms;
					float factor;
					Link curLnk = (Link) iter.next();
					
					try
					{
						ms = Integer.parseInt(constDelayField.getText());
						factor = Float.parseFloat(distDelayField.getText());
					}
					catch (NumberFormatException e)
					{
						// this should not happen - wrong inputs are already handled by
						// the window
						throw new RuntimeException("ERROR: Internal error while updating");
					}
					
					curLnk.setConstDelayMs(ms);
					curLnk.setDelayFactor(factor);
					
					if (noDelay.isSelected())
						curLnk.setDelayType(Link.NO_DELAY);
					else if (constDelay.isSelected())
						curLnk.setDelayType(Link.CONST_DELAY);
					else if (distDelay.isSelected())
						curLnk.setDelayType(Link.DIST_DELAY);
						
				}
				
				emu.redrawGraph();
			}
		}
		else if (source == errorButton)
		{
			if (!checkIfInputIsLegal())
			{
				return;
			}
			
			// check if the user really want to do this
			if (JOptionPane.showConfirmDialog(parent, 
					"Are you sure you want to apply these error\n" +
					"settings to all links",
					"Apply to all links?", 
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE)
				
				== JOptionPane.YES_OPTION)
			{
				// apply error settings to all links
				Vector allLinks = emu.getGraph().getLinks();
				
				for (Iterator iter=allLinks.iterator(); iter.hasNext(); )
				{
					Link curLnk = (Link) iter.next();
					float prob;
					String pattern;
					
					try
					{
						prob = Float.parseFloat(constErrorField.getText());
						pattern = determErrorPatternField.getText();
					}
					catch (NumberFormatException e)
					{
						// this should not happen - wrong inputs are already handled by
						// the window
						throw new RuntimeException("ERROR: Internal error while updating");
					}
					
					curLnk.setConstErrorProb(prob);
					curLnk.setErrorPattern(pattern);
					
					if (noError.isSelected())
						curLnk.setErrorType(Link.NO_ERROR);
					else if (constError.isSelected())
						curLnk.setErrorType(Link.CONST_ERROR);
					else if (determError.isSelected())
						curLnk.setErrorType(Link.DETERM_ERROR);
				}
				
				emu.redrawGraph();
			}
		}
		else
		{
			// ignore other actions
		}
	}
}