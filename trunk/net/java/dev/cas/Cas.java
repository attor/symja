/*
CAS Computer Algebra System
Copyright (C) 2005  William Tracy

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
*/

package net.java.dev.cas;

import java.awt.*;
import java.awt.event.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;

import javax.swing.*;
import javax.swing.event.*;

import java.util.Hashtable;

import org.matheclipse.parser.client.eval.*;

/*import org.javadev.AnimatingCardLayout;
import org.javadev.effects.*;*/

import net.java.swingfx.waitwithstyle.InfiniteProgressPanel;


/** This is currently the primary class to launch CAS.
  * Extends JApplet so that it can functions as an applet; if main()
  * is called, the applet is wrapped in a window (JFrame).
  */
public class Cas extends JApplet {
    /** Launches CAS as an application.
      * Creates a window (JFrame) and displays the Cas panel inside
      * that window. Calls init() and start() on the Cas to mimic the
      * applet initialization behavior.
      */
	public static void main(String args[]) {
		JFrame frame = new JFrame("CAS");
		Cas cas = new Cas();

		frame.setContentPane(cas);
		cas.init();
		cas.start();

        readyFrame(frame);
	}

    /** Sizes a frame, makes it quit the application when it closes,
      * and displays it.
      */
    protected static void readyFrame(JFrame frame) {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.show();
    }

    /** Displays the previously entered commands and their results.
      * Displayed at the center of the display.
      */
	protected JList commands;

    /** Displays the command list in a red-yellow color scheme.
      */
	protected MathRenderer redRenderer;

    /** Displays the command list in a blue-green color scheme.
      */
	protected MathRenderer blueRenderer;

	/** Displays the command list in a gray color scheme.
	  */
	protected MathRenderer grayRenderer;

	/** Contains the previously executed commands for display.
	  */
	protected DefaultListModel model;

    /** Area where user enters new commands. Displayed at bottom of
      * screen.
      */
	protected JTextField commandLine;

    /** Button to execute commands. Displayed at bottom right corner
      * of the screen.
      */
	protected JButton exec;

    /** Initializes the applet.
      * Lays out the controls and registers listeners with them.
      */
	public void init() {
		JComponent content = (JComponent)getContentPane();
        JPanel primaryView = new JPanel();
        //AnimatingCardLayout layout = new RandomAnimatingCardLayout(new Animation[] {new FadeAnimation(), new SlideAnimation()});
        CardLayout layout = new CardLayout();

        content.setLayout(layout);
        content.add(primaryView, "primary");

        setupContent(primaryView);
        populateContent(primaryView);
	}

    /** Cached plots tend to choke when applet is reloaded, so we
      * need to clear them.
      */
    /*public void stop() {
        Plotter.clearCache();
        ParametricPlotter.clearCache();
        Plotter3D.clearCache();
    }*/

    /** Sets the border and layout of content.
      */
    protected static void setupContent(JComponent content) {
        content.setBorder(BorderFactory.createEmptyBorder(5,
                                                          5,
                                                          5,
                                                          5));
        content.setLayout(new BorderLayout());
    }

    /** Adds components to content.
      */
    protected void populateContent(JComponent content) {
        content.add(createCenter(), BorderLayout.CENTER);
		content.add(createToolbar(), BorderLayout.NORTH);
    }

    /** Sets up the center and bottom portions of the display.
      * Setup of the bottom part of the screen is delegated to
      * setupBottom().
      */
	protected JPanel createCenter() {
		JPanel center = new JPanel();
        JScrollPane commandScroller;

		model = new DefaultListModel();
		commands = new JList(model);
		center.setLayout(new BorderLayout());

        setupRenderers();

        commandScroller = new JScrollPane(commands);
        center.add(commandScroller, BorderLayout.CENTER);
        center.add(createBottom(commandScroller),
                   BorderLayout.SOUTH);

        DragSource.getDefaultDragSource()
                                 .createDefaultDragGestureRecognizer(
                                             commands,
                                             1,
                                             new ListDragListener());
        commands.addMouseListener(new InsertSelectedListener(
                                                       commandLine));

		return center;
	}

    /** Initializes the different colored renderers for the commands
      * list.
      */
    protected void setupRenderers() {
        redRenderer = new MathRenderer(255, 0, 0, 0, 1, 0);
        blueRenderer = new MathRenderer(0, 0, 255, 0, 1, -1);
        grayRenderer = new MathRenderer(128, 128, 128);
        commands.setCellRenderer(redRenderer);
    }

    /** Sets up the bottom part of the display (text field and
      * execute (button). commandLine is a member of the class
      * instead of being local to this function so that the insert
      * command can access it; exec is a class member also just for
      * consistency.
      */
	protected JPanel createBottom(JScrollPane scroll) {
		JPanel bottom = new JPanel();
        ActionListener executeListener;

        commandLine = new JTextField("Plot3D({x * y, -y * x}, {x, -10.0, 10.0}, {y, -10.0, 10.0})");
        exec = new JButton("Execute");

        executeListener = new ExecuteListener(commandLine,
                                              model,
                                              scroll,
                                              this);
        commandLine.addActionListener(executeListener);
        exec.addActionListener(executeListener);
        bottom.setLayout(new BorderLayout());
        bottom.add(commandLine, BorderLayout.CENTER);
        bottom.add(exec, BorderLayout.EAST);

		return bottom;
	}

    /** Sets up the tool bar.
      * Icons are from Sun's Java Look and Feel graphics repository:
      * http://java.sun.com/developer/techDocs/hi/repository/
      */
	protected JToolBar createToolbar() {
        JToolBar tools = new JToolBar();
        JButton insert = new JButton(new ImageIcon(
                            getClass().getResource("Import24.gif")));
        JButton preferences = new JButton(new ImageIcon(
                       getClass().getResource("Preferences24.gif")));
        JButton about = new JButton(new ImageIcon(getClass()
                                       .getResource("About24.gif")));
        InsertListener il = new InsertListener(commandLine, this);

        insert.addActionListener(il);
        insert.setToolTipText(
                     "Insert a function or constant into a command");
        tools.add(insert);
        preferences.addActionListener(
                                new PreferencesListener(commands,
                                                        redRenderer,
                                                        blueRenderer,
                                                        grayRenderer,
                                                        this));
        preferences.setToolTipText("Preferences");
        tools.add(preferences);
        about.addActionListener(new AboutListener(this));
        about.setToolTipText("About CAS");
        tools.add(about);

		return tools;
	}

	

    public void start() {
        commandLine.selectAll();
        commandLine.requestFocus();
    }
}

/** Responsible for executing a command when the user presses
  * enter or clicks "execute". The parsed text and command return
  * value are appended to the ListModel.
  */
class ExecuteListener implements ActionListener {
    /** This is the area that commands originate from.
      */
	JTextField source;

    /** Completed commands and their results are appended to this.
      */
	DefaultListModel sink;

    /** Scrolls the display to the bottom to show the new command's
      * result.
      */
    Scroller scroller;

    /** Displays fun patterns while a command is executed.
      */
	InfiniteProgressPanel progress;

    /** Creates an ExecuteLister that takes commands from in, writes
      * them to out, scrolls to the bottom of scroll, and displays a
      * wait animation in parent.
      */
	public ExecuteListener(JTextField in,
                           DefaultListModel out,
                           JScrollPane scroll,
                           RootPaneContainer parent) {
        if (in == null)
            throw new NullPointerException("No input field");
        if (out == null)
            throw new NullPointerException("No output model");
        if (scroll == null)
            throw new NullPointerException("No scroll pane");
        if (parent == null)
            throw new NullPointerException("No parent");

		source = in;
		sink = out;

        scroller = new Scroller(scroll);
        progress = new InfiniteProgressPanel();
        parent.setGlassPane(progress);
	}

    /** Sets up the wait animation and starts execution of the
      * command.
      */
	public void actionPerformed(ActionEvent ae) {
        progress.start();
        new CommandExecutor(sink,
                            source,
                            scroller,
                            progress).start();
	}
}

/** Executes a command in a new thread. This allows the AWT thread to
  * display a wait animation while the command executes.
  */
class CommandExecutor extends Thread {
    /** The command to be executed.
      */
    protected String in;

    /** The model to which the output is appended.
      */
    protected DefaultListModel out;

    /** The text field the command came from.
      */
    protected JTextField source;

    /** Scrolls the scroll pane down.
      */
    Scroller scroller;

    /** Displays the wait animation.
      */
    InfiniteProgressPanel progress;

    /** Creates a CommandExecutor to execute the command in the text
      * field. When the thread is started, progress is displayed, the
      * command is executed, the result is appended to
      * out, scroller is scrolled to the bottom, and progress is
      * then hidden.
      */
    public CommandExecutor(
                         DefaultListModel out,
                         JTextField source,
                         Scroller scroller,
                         InfiniteProgressPanel progress) {
        in = source.getText();
        this.out = out;
        this.source = source;
        this.scroller = scroller;
        this.progress = progress;
    }

    /** Executes the command.
      */
    public void run() {
        try {
	    ComplexEvaluator evaluator = new ComplexEvaluator();
	    String result = ComplexEvaluator.toString(evaluator.evaluate(in));


            EventQueue.invokeLater(new Displayer(in,
                                                 result,
                                                 out,
                                                 source,
                                                 scroller,
                                                 progress));
        } catch (Throwable thrown) {
            thrown.printStackTrace();
            EventQueue.invokeLater(new Displayer(in,
                                                 thrown.toString(),
                                                 out,
                                                 source,
                                                 scroller,
                                                 progress));
        }
    }
}

/** Displays the results of a command. This must be executed on the
  * AWT event queue to avoid deadlock, so it can't simply be
  * displayed from the thread that generated the results.
  */
class Displayer implements Runnable {
    /** The text input by the user.
      */
    protected String input;

    /** The result of executing the string entered by the user.
      */
    protected String output;

    /** The model to which the input and output are to be appended.
      */
    protected DefaultListModel sink;

    /** Receives the focus after the input and output have been
      * displayed.
      */
    JTextField source;

    /** Scrolls down to display the new output.
      */
    Scroller scroller;

    /** Needs to be disabled now that the results have been computed.
      */
    InfiniteProgressPanel progress;

    /** Assembles a displayer that, when run, appends in and out to
      * sink. Scroller is scrolled to the bottom, progress is hidden,
      * and source gets the focus.
      */
    public Displayer(String in,
                     String out,
                     DefaultListModel sink,
                     JTextField source,
                     Scroller scroller,
                     InfiniteProgressPanel progress) {
        input = in;
        output = out;
        this.sink = sink;
        this.source = source;
        this.scroller = scroller;
        this.progress = progress;
    }

    /** Does the work.
      */
    public void run() {
        progress.stop();

        sink.addElement(input);
        sink.addElement(output);

        scroller.scrollToEnd();
        source.selectAll();
        source.requestFocus();
    }
}

/** Handles drag events on JLists.
  */
class ListDragListener implements DragGestureListener {
    /**
     * Finds the item the user clicked on, casts it to a String, and
     * transfers it.
     */
    public void dragGestureRecognized(DragGestureEvent dge) {
        JList list = (JList)dge.getComponent();
        Transferable text = null;

        for (int counter = list.getFirstVisibleIndex();
                 counter <= list.getLastVisibleIndex();
                 ++counter) {
            if (list.getCellBounds(counter, counter).contains(
                                              dge.getDragOrigin())) {
                text = new StringSelection(
                      (String)list.getModel().getElementAt(counter));
                dge.startDrag(DragSource.DefaultCopyDrop, text);
                return;
            }
        }
    }
}


/** Handles double-click events on JLists. Finds the item the user
  * clicked on, casts it to a String, and inserts it into the text
  * field.
  */
class InsertSelectedListener extends MouseAdapter {
    /** Receives the text the user double-clicked.
      */
    JTextField target;

    /** Initializes the listener. Selected items are inserted into
      * commandLine.
      */
    public InsertSelectedListener(JTextField commandLine) {
        target = commandLine;
    }

    /** If the user double-clicked, the source is cast to a JList.
      * Iterates over the list's contents until the selected one is
      * found. Casts the selected object to a String and inserts it
      * into the text field.
      */
    public void mousePressed(MouseEvent e) {
        if (e.getClickCount() >= 2) {
            JList list = (JList)e.getSource();
            String text = null;

            for (int counter = list.getFirstVisibleIndex();
                     counter <= list.getLastVisibleIndex();
                     ++counter) {
                if (list.getCellBounds(counter, counter).contains(
                                                     e.getPoint())) {
                    text = ((String)list.getModel().getElementAt(
                                                           counter));
                    target.replaceSelection(text);

                    return;
                }
            }
        }
    }
}

class AboutListener implements ActionListener {
    Component parent;

    public AboutListener(Component c) {
        parent = c;
    }

    public void actionPerformed(ActionEvent e) {
        JOptionPane.showMessageDialog(parent,
            "CAS Computer Algebra System, Copyright (C) 2005 William Tracy (afishionado@dev.java.net)\nCAS comes with ABSOLUTELY NO WARRENTY. This is Free Software, and you are welcome to\nredistribute it under certain conditions.\n\nCAS is based on the work of the HartMath project (http://hartmath.dev.java.net)\n\nIt also contains code from the SwingFX (http://swingfx.dev.java.net) and\nAnimatingCardLayout (http://animatingcardlayout.dev.java.net) libraries.",
            "About CAS",
            JOptionPane.INFORMATION_MESSAGE, new ImageIcon(getClass()
                                       .getResource("About24.gif")));
    }
}
