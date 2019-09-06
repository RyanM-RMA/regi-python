/*
 * Copyright (c) 2018
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */
package usace.rowcps.headless;

import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 *
 * @author @author <a href="mailto:ryanm@rmanet.com">Ryan A. Miles (ryanm@rmanet.com)</a>
 */
public class TestFrame extends JFrame
{

	private final ExecutorService _executor = Executors.newSingleThreadExecutor();
	private final List<JButton> _buttons = new ArrayList<>();
	
	/**
	 * Formatted as:
	 * [n][0] = Button Name
	 * [n][1] = Python file
	 */
	private final String[][] _pythonTestFilesAndDisplayNames = new String[][]{
		{"Auto Adjust",		"InflowCalcAutoAdjust.py"},
		{"Clone",			"InflowCalcClone.py"},
		{"Balance All",		"InflowCalcBalanceAll.py"},
		{"Zero Negatives",	"InflowCalcZeroNegative.py"},
	};
	
	public TestFrame() throws HeadlessException
	{
		buildComponents();
	}

	public static void main(String[] args)
	{
		TestHeadless.beforeClass();
		TestFrame frame = new TestFrame();
		frame.pack();
		frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
		frame.setVisible(true);
	}

	private void buildComponents()
	{
		setLayout(new GridLayout(0, 2, 2, 2));

		for (String[] testData : _pythonTestFilesAndDisplayNames)
		{
			JButton btn = new JButton(new ButtonAction(testData[0], testData[1]));
			_buttons.add(btn);
			add(btn);
		}
	}
	
	private void performHeadless(String file)
	{
		disableButtons();
		
		_executor.submit(() -> 
		{
			String[] args = TestHeadless.getArgsForFile(file);
			try
			{
				RegiCLI.runHeadlessTest(args);
			}
			catch (Exception ex)
			{
				Logger.getLogger(TestFrame.class.getName()).log(Level.SEVERE, "Exception occurred", ex);
			}
			SwingUtilities.invokeLater(this::enableButtons);
		});
	}
	
	private void disableButtons()
	{
		for (JButton btn : _buttons)
		{
			btn.setEnabled(false);
		}
	}
	
	private void enableButtons()
	{
		for (JButton btn : _buttons)
		{
			btn.setEnabled(true);
		}
	}
	
	private class ButtonAction extends AbstractAction
	{
		private final String _file;
		public ButtonAction(String name, String file)
		{
			super(name);
			_file = file;
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			performHeadless(_file);
		}
	}
}
