package mobicomp.emu;

/*
 * OptionsWindow
 *
 * this window allows the user to change certains global settings of the 
 * emulator
 *
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.FileOutputStream;

public class OptionsWindow extends JDialog implements ActionListener
{
	private JFrame parent;
	
	// Emulator output
	JCheckBox outPackSent = new JCheckBox("Packets sent");
	JCheckBox outPackLost = new JCheckBox("Packets lost due to errors");
	JCheckBox outPackBuffer = new JCheckBox("Packets lost due to buffer overflow");
	
	// Output settings
	JRadioButton settPrintToConsole = new JRadioButton();
	JLabel settPrintToConsoleLabel = new JLabel("Print to console");
	JRadioButton settPrintToWindow = new JRadioButton();
	JLabel settPrintToWindowLabel = new JLabel("Print to output window");
	ButtonGroup settPrintTarget = new ButtonGroup();
	JCheckBox settAddPrefix = new JCheckBox("Add \"Node:\" prefix");
	JLabel settCharsPerTab = new JLabel("# of characters saved per tab: ");
	JTextField settCharsField = new JTextField(5);
	
	// Miscellaneous settings
	JLabel miscBufferSize1 = new JLabel("Packet buffer size:");
	JLabel miscBufferSize2 = new JLabel("packets");
	JTextField miscBufferSizeField = new JTextField(3);
	JLabel miscFlashLink1 = new JLabel("Flash link for");
	JLabel miscFlashLink2 = new JLabel("ms when used");
	JTextField miscFlashLinkField = new JTextField(4);
	
	// OK and Cancel buttons
	JButton okButton = new JButton("Apply");
	JButton cancelButton = new JButton("Cancel");
	
	public OptionsWindow(JFrame parent)
	{
		super(parent, "Emulator Options", true);
		
		this.parent = parent;
		initWindow();
	}
	
	private void initWindow()
	{
		// create a new content pane
		JPanel cp = new JPanel();
		cp.setLayout(new BoxLayout(cp, BoxLayout.Y_AXIS));
		
		// emulator output
		JPanel emuOutPanel = new JPanel();
		emuOutPanel.setBorder(BorderFactory.createTitledBorder("Emulator output"));
		emuOutPanel.setLayout(new GridLayout(0, 1, 0, 5));
		
		emuOutPanel.add(outPackSent);
		emuOutPanel.add(outPackLost);
		emuOutPanel.add(outPackBuffer);
		
		// output settings
		JPanel outputPanel = new JPanel();
		outputPanel.setBorder(BorderFactory.createTitledBorder("Output settings"));
		outputPanel.setLayout(new GridBagLayout());
		
		settPrintTarget.add(settPrintToConsole);
		settPrintTarget.add(settPrintToWindow);
		
		GridBagConstraints con = new GridBagConstraints();
		con.gridx = 0;
		con.gridy = 0;
		con.anchor = GridBagConstraints.LINE_START;
		
		outputPanel.add(settPrintToConsole, con);
		
		con.gridx = 1;
		settPrintToConsoleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		outputPanel.add(settPrintToConsoleLabel, con);
		
		con.gridy = 1;
		outputPanel.add(settAddPrefix, con);
		
		con.gridx = 0;
		con.gridy = 2;
		con.insets = new Insets(10, 0, 0, 0);
		outputPanel.add(settPrintToWindow, con);
		
		con.gridx = 1;
		con.insets = new Insets(10, 0, 0, 0);
		settPrintToWindowLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		outputPanel.add(settPrintToWindowLabel, con);
		
		JPanel charPerTabPanel = new JPanel();
		charPerTabPanel.setLayout(new BoxLayout(charPerTabPanel, BoxLayout.X_AXIS));
		charPerTabPanel.add(settCharsPerTab);
		charPerTabPanel.add(settCharsField);
		
		con.gridy = 3;
		con.insets = new Insets(0,0,0,0);
		outputPanel.add(charPerTabPanel, con);
		
		// miscellaneous settings
		JPanel miscPanel = new JPanel();
		miscPanel.setBorder(BorderFactory.createTitledBorder("Miscellaneous settings"));
		miscPanel.setLayout(new BoxLayout(miscPanel, BoxLayout.Y_AXIS));
		
		JPanel bufferPanel = new JPanel();
		bufferPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
		bufferPanel.add(miscBufferSize1);
		bufferPanel.add(miscBufferSizeField);
		bufferPanel.add(miscBufferSize2);
		
		miscPanel.add(bufferPanel);
		
		JPanel flashPanel = new JPanel();
		flashPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		flashPanel.add(miscFlashLink1);
		flashPanel.add(miscFlashLinkField);
		flashPanel.add(miscFlashLink2);
		
		miscPanel.add(flashPanel);
		
		// the buttons
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		buttonPanel.add(okButton);
		buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonPanel.add(cancelButton);
		
		cp.add(emuOutPanel);
		cp.add(Box.createRigidArea(new Dimension(0, 10)));
		cp.add(outputPanel);
		cp.add(Box.createRigidArea(new Dimension(0, 10)));
		cp.add(miscPanel);
		cp.add(Box.createRigidArea(new Dimension(0, 10)));
		cp.add(buttonPanel);
		cp.add(Box.createRigidArea(new Dimension(0, 10)));
		
		// insert the values from class Options
		if (Options.outputPacketSent)
			outPackSent.setSelected(true);
			
		if (Options.outputPacketLost)
			outPackLost.setSelected(true);
			
		if (Options.outputPacketOverflow)
			outPackBuffer.setSelected(true);
		
		if (Options.printToConsole)
			settPrintToConsole.setSelected(true);
		else
			settPrintToWindow.setSelected(true);
		
		if (Options.addNodePrefix)
			settAddPrefix.setSelected(true);
		
		miscBufferSizeField.setText(Integer.toString(Options.packetBufferSize));
		miscFlashLinkField.setText(Integer.toString(Options.flashTime));
		settCharsField.setText(Integer.toString(Options.charBufferSize));
		
		// set the default button
		getRootPane().setDefaultButton(okButton);
		
		// add action listener to the buttons
		okButton.addActionListener(this);
		cancelButton.addActionListener(this);
		
		// activate the window
		this.setContentPane(cp);
		this.pack();
		this.setResizable(false);
		this.setVisible(true);
	}
	
	// allpy the changes made and save them to "emulator.txt"
	private boolean applyValues()
	{
		if (!checkValues())
			return false;
		
		if (outPackSent.isSelected())
		{
			Options.outputPacketSent = true;
			System.setProperty("mobicomp.emu.outputPacketSent", "true");
		}
		else
		{
			Options.outputPacketSent = false;
			System.setProperty("mobicomp.emu.outputPacketSent", "false");
		}
			
		if (outPackLost.isSelected())
		{
			Options.outputPacketLost = true;
			System.setProperty("mobicomp.emu.outputPacketLost", "true");
		}
		else
		{
			Options.outputPacketLost = false;
			System.setProperty("mobicomp.emu.outputPacketLost", "false");
		}
			
		if (outPackBuffer.isSelected())
		{
			Options.outputPacketOverflow = true;
			System.setProperty("mobicomp.emu.outputPacketOverflow", "true");
		}
		else
		{
			Options.outputPacketOverflow = false;
			System.setProperty("mobicomp.emu.outputPacketOverflow", "false");
		}
			
		if (settPrintToConsole.isSelected())
		{
			Options.printToConsole = true;
			System.setProperty("mobicomp.emu.printToConsole", "true");
		}
		else
		{
			Options.printToConsole = false;
			System.setProperty("mobicomp.emu.printToConsole", "false");
		}
			
		if (settAddPrefix.isSelected())
		{
			Options.addNodePrefix = true;
			System.setProperty("mobicomp.emu.addNodePrefix", "true");
		}
		else
		{
			Options.addNodePrefix = false;
			System.setProperty("mobicomp.emu.addNodePrefix", "false");
		}
		
		try
		{
			Options.charBufferSize = Integer.parseInt(settCharsField.getText());
			System.setProperty("mobicomp.emu.charBufferSize", settCharsField.getText());
			Options.flashTime = Integer.parseInt(miscFlashLinkField.getText());
			System.setProperty("mobicomp.emu.flashTime", miscFlashLinkField.getText());
			Options.packetBufferSize = Integer.parseInt(miscBufferSizeField.getText());
			System.setProperty("mobicomp.emu.packetBufferSize", miscBufferSizeField.getText());
		}
		catch (NumberFormatException e)
		{
			throw new RuntimeException("ERROR: an impossible thing happend :)");
		}
		
		try
		{
			System.getProperties().store(new FileOutputStream("emulator.txt"), "Properties of the emulator");
		}
		catch (IOException e)
		{
			Emulator.getRef().sendEmulatorMessage("ERROR: Could not write settings to emulator.txt\n", true);
		}
		
		return true;
	}
	
	// check if the users input is legal
	public boolean checkValues()
	{
		try
		{
			int a = Integer.parseInt(settCharsField.getText());
			int b = Integer.parseInt(miscFlashLinkField.getText());
			int c = Integer.parseInt(miscBufferSizeField.getText());
			
			if (a>0 && b>=0 && c>0)
				return true;
			else
				return false;
		}
		catch (NumberFormatException e)
		{
			return false;
		}
	}
	
	public void actionPerformed(ActionEvent event)
	{
		Object source = event.getSource();
		
		if (source == okButton)
		{
			// write changes to class Options
			if (applyValues())
				this.setVisible(false);
			else
				JOptionPane.showMessageDialog(
					parent, 
					"Please enter positive integers for buffer size,\n" +
					"flash time and # charactes per tab.", 
					"Illegal input", 
					JOptionPane.OK_OPTION);
		}
		else if (source == cancelButton)
		{
			// discard changes made
			this.setVisible(false);
		}
		else
		{
			// ignore action
		}
	}
}