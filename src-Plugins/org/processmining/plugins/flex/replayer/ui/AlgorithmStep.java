package org.processmining.plugins.flex.replayer.ui;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.Dimension;

import javax.swing.JComboBox;

import org.processmining.plugins.flex.replayer.algorithms.CancellationAwareAStarAlgorithm;
import org.processmining.plugins.flex.replayer.algorithms.ExtendedCostBasedAStarLogReplayAlgorithm;
import org.processmining.plugins.flex.replayer.algorithms.IFlexLogReplayAlgorithm;

import com.fluxicon.slickerbox.factory.SlickerFactory;
import com.fluxicon.slickerbox.ui.SlickerComboBoxUI;

/**
 * @author arya
 * @email arya.adriansyah@gmail.com
 * @version Dec 1, 2009
 */
public class AlgorithmStep extends ReplayStep {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4015645475781862031L;

	private JComboBox combo;

	public AlgorithmStep() {
		initComponents();
	}

	public boolean precondition() {
		return true;
	}

	private void initComponents() {
		// init instance
		SlickerFactory slickerFactory = SlickerFactory.instance();

		double size[][] = { { TableLayoutConstants.FILL }, { 80, 30 } };
		setLayout(new TableLayout(size));
		String body = "<p>Select your replay algorithm.</p>";
		add(slickerFactory.createLabel("<html><h1>Select Algorithm</h1>" + body), "0, 0, l, t");

		// add combobox
		IFlexLogReplayAlgorithm[] availAlgorithms = new IFlexLogReplayAlgorithm[2];
//		availAlgorithms[0] = new AStarFlexLogReplayAlgorithm();
//		availAlgorithms[1] = new CostBasedAStarLogReplayAlgorithm();
		availAlgorithms[0] = new ExtendedCostBasedAStarLogReplayAlgorithm();
		availAlgorithms[1] = new CancellationAwareAStarAlgorithm();
		combo = new JComboBox(availAlgorithms);
		combo.setPreferredSize(new Dimension(150, 25));
		combo.setSize(new Dimension(150, 25));
		combo.setMinimumSize(new Dimension(150, 25));
		combo.setSelectedItem(0);
		combo.setUI(new SlickerComboBoxUI());
		add(combo, "0, 1");
	}

	public IFlexLogReplayAlgorithm getAlgorithm() {
		return (IFlexLogReplayAlgorithm) combo.getSelectedItem();
	}

	public void readSettings(IFlexLogReplayAlgorithm algorithm) {
	}
}
