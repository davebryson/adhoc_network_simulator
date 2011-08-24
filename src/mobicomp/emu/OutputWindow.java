package mobicomp.emu;

/*
 * OutputWindow
 * 
 * this window replaces the default console. it offers a tab for each running
 * client and a special "All" tab where the output of all clients is accumulated
 *  
 */
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.io.PrintStream;
import java.util.HashMap;

public class OutputWindow extends JFrame implements ChangeListener {

    private JTabbedPane tabs;

    private Color[] colors;

    private HashMap nameToTab;

    private int colorCounter;

    private static int NR_OF_COLORS = 11;

    private static Color ERROR_COLOR = new Color(255, 0, 0);

    public OutputWindow() {
        super("Output Window");
        // just to be sure that the color of the selected tab is gray
        UIManager.put("TabbedPane.selected", Color.GRAY);
        tabs = new JTabbedPane(JTabbedPane.BOTTOM, JTabbedPane.WRAP_TAB_LAYOUT);
        nameToTab = new HashMap();
        // add change listener
        tabs.addChangeListener(this);
        // fill the color-vector with some colors
        colors = new Color[NR_OF_COLORS];
        colors[0] = new Color(255, 128, 128);
        colors[1] = new Color(255, 255, 0);
        colors[2] = new Color(0, 255, 0);
        colors[3] = new Color(0, 255, 255);
        colors[4] = new Color(0, 0, 255);
        colors[5] = new Color(255, 0, 255);
        colors[6] = new Color(255, 255, 128);
        colors[7] = new Color(128, 255, 128);
        colors[8] = new Color(128, 255, 255);
        colors[9] = new Color(128, 128, 255);
        colors[10] = new Color(255, 128, 255);
        colorCounter = -1;
        // add a tab with a JTextArea of size 25x80 characters (-> console)
        // this is an easy way for setting up the window with the right size.
        // the tab is removed after the pack() is called
        JTextArea dummy = new JTextArea(null, 25, 80);
        dummy.setFont(new Font("Courier", Font.PLAIN, 12));
        JScrollPane scrolldummy = new JScrollPane(dummy);
        scrolldummy.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        tabs.addTab("blah", scrolldummy);
        // add the tab containing output of all other tabs
        addTab("All");
        // add the tab for the emulator output
        addTab("Emulator");
        this.getContentPane().add(tabs);
        this.setResizable(false);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.pack();
        // remove the tab needed for sizing the window
        tabs.removeTabAt(0);
        stateChanged(null);
        this.setVisible(true);
    }

    public void addTab(String name) {
        if (nameToTab.containsKey(name)) {
        // there is already a tab with that name!
        throw new RuntimeException("ERROR: Tab already exists"); }
        TabProp tb = new TabProp();
        tb.colorIndex = colorCounter;
        colorCounter = (colorCounter + 1) % NR_OF_COLORS;
        tb.textpane = new JColorTextPane();
        tb.textpane.setEditable(false);
        tb.textpane.setFont(new Font("Courier", Font.PLAIN, 12));
        tb.scrollpane = new JScrollPane(tb.textpane);
        tb.scrollpane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        tb.scrollpane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        synchronized (nameToTab) {
            nameToTab.put(name, tb);
        }
        tabs.addTab(name, tb.scrollpane);
        // update colors (needed if "Emulator" is selected)
        stateChanged(null);
    }

    public void removeTab(String name) {
        if (!nameToTab.containsKey(name)) {
        // there is no tab with this name to remove
        throw new RuntimeException("ERROR: No tab found with name " + name); }
        for (int i = 1; i < tabs.getTabCount(); i++) {
            if (tabs.getTitleAt(i).equals(name)) {
                tabs.removeTabAt(i);
                nameToTab.remove(name);
                return;
            }
        }
        // update colors
        stateChanged(null);
    }

    public void write(PrintStream console, String tabName, String text, boolean error) {
        TabProp tb;
        synchronized (nameToTab) {
            tb = (TabProp) nameToTab.get(tabName);
        }
        if (tb == null) {
            console.println("no tab");
            // there is no tab with this name...
            return;
        }
        // write the text to the specified tab
        tb.textpane.append(text, Color.WHITE, error);
        if (!tabName.equals("All")) {
            // write the text to the "all" tab, too
            ((TabProp) nameToTab.get("All")).textpane.append(text, colors[tb.colorIndex], error);
        }
        // scroll down the current tab
        JScrollBar sb = ((TabProp) nameToTab.get(tabs.getTitleAt(tabs.getSelectedIndex()))).scrollpane.getVerticalScrollBar();
        try {
            sb.setValue(sb.getMaximum());
        } catch (ArrayIndexOutOfBoundsException e) {
            // this can happen very rarely when another thread changes the
            // maximum after sb.getMaximum(), but before sb.setValue()
        }
    } // from interface ChangeListener

    public void stateChanged(ChangeEvent e) {
        if (tabs.getTitleAt(tabs.getSelectedIndex()).equals("All")) {
            // draw colorful tabs if "All" is selected
            for (int i = 1; i < tabs.getTabCount(); i++) {
                tabs.setBackgroundAt(i, colors[((TabProp) nameToTab.get(tabs.getTitleAt(i))).colorIndex]);
            }
        } else {
            // remove all colors from the tabs if "All" isn't selected
            for (int i = 0; i < tabs.getTabCount(); i++) {
                tabs.setBackgroundAt(i, (Color) UIManager.get("Tabbedpane.unselected"));
            }
        }
    }

    // a small datatype for all properties of a tab
    private class TabProp {

        public int colorIndex;

        public JScrollPane scrollpane;

        public JColorTextPane textpane;
    }

    // a private class that allows colored text in a JTextPane
    private class JColorTextPane extends JTextPane {

        private DefaultStyledDocument doc;

        private int charCounter;

        public JColorTextPane() {
            doc = new DefaultStyledDocument();
            charCounter = 0;
            setDocument(doc);
            setMargin(new Insets(0, 0, 0, 0));
        }

        // synchronized to prevent some problems if different threads try to write to the console at the
        // same time
        public synchronized void append(String text, Color color, boolean error) {
            try {
                SimpleAttributeSet attr = new SimpleAttributeSet();
                StyleConstants.setBackground(attr, color);
                if (error) {
                    StyleConstants.setForeground(attr, ERROR_COLOR);
                }
                doc.insertString(doc.getLength(), text, attr);
                // remove some old stuff if the document is getting to long
                if (doc.getLength() > Options.charBufferSize) {
                    doc.remove(0, doc.getLength() - Options.charBufferSize);
                }
            } catch (BadLocationException e) {
                // ignore it
            }
        }

        public synchronized void append(String text) {
            append(text, Color.white, false);
        }
    }
}