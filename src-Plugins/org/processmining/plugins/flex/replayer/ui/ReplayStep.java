package org.processmining.plugins.flex.replayer.ui;

import javax.swing.JPanel;

import org.processmining.plugins.flex.replayer.algorithms.IFlexLogReplayAlgorithm;

/**
 * @author arya
 * @email arya.adriansyah@gmail.com
 * @version Dec 1, 2009
 * 
 * Abstract class for replay step required for Flex replay algorithm
 */
public abstract class ReplayStep extends JPanel {
	private static final long serialVersionUID = -6447646079319355783L;

	public abstract boolean precondition();

	public abstract void readSettings(IFlexLogReplayAlgorithm algorithm);
}