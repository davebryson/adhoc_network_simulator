package mobicomp.emu;

/*
 * MainWindow
 *
 * the window of the emulator. on the left, there is a panel of type drawpanel, 
 * on the right is a controlpanel
 *
 */

import javax.swing.*;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.*;

public class MainWindow extends JFrame
{
	private Emulator emu;
	
	private JPanel draw;
	private JPanel control;

	// constructor
	public MainWindow()
	{
		super("SANS - Simple Ad Hoc Network Simulator");
		emu = Emulator.getRef();
		initWindow();
	}
	
	// place GUI elements in the window
	private void initWindow()
	{
        System.out.println("initWindow");
		// all the nodes and links are shown there
		draw = new DrawPanel(this);
        System.out.println("Init Control Panel");
		// the buttons on the right
		control = new ControlPanel(this);
		
		// the content pane
		JPanel cp = new JPanel();
		cp.setLayout(new BorderLayout());
		cp.add(draw, BorderLayout.CENTER);
		cp.add(control,BorderLayout.EAST);
		
		setContentPane(cp);
		
		WindowListener listener = new WindowAdapter()
		{
			public void windowClosing(WindowEvent event)
			{
				System.exit(0);
			}
		};
		
		this.addWindowListener(listener);
		this.pack();
		this.setResizable(true);
		this.setVisible(true);

	}
	
	public DrawPanel getDrawPanel()
	{
		return (DrawPanel) draw;
	}
}