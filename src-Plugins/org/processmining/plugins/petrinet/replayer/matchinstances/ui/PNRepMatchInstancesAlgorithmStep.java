/**
 * 
 */
package org.processmining.plugins.petrinet.replayer.matchinstances.ui;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.Dimension;

import javax.swing.JComboBox;

import org.processmining.plugins.petrinet.replayer.matchinstances.algorithms.CompleteCostBasedPNMatchInstancesReplayAlgorithm;
import org.processmining.plugins.petrinet.replayer.matchinstances.algorithms.CostBasedPNMatchInstancesReplayAlgorithm;
import org.processmining.plugins.petrinet.replayer.matchinstances.algorithms.IPNMatchInstancesLogReplayAlgorithm;

import com.fluxicon.slickerbox.factory.SlickerFactory;
import com.fluxicon.slickerbox.ui.SlickerComboBoxUI;

/**
 * @author aadrians
 *
 */
public class PNRepMatchInstancesAlgorithmStep extends PNReplayStep {

	private static final long serialVersionUID = 8899781737579767946L;

	private JComboBox combo;

	public PNRepMatchInstancesAlgorithmStep() {
		initComponents();
	}

	private void initComponents() {
		// init instance
		SlickerFactory slickerFactory = SlickerFactory.instance();

		double size[][] = { { TableLayoutConstants.FILL }, { 80, 30 } };
		setLayout(new TableLayout(size));
		String body = "<p>Select your replay algorithm.</p>";
		add(slickerFactory
				.createLabel("<html><h1>Select Algorithm</h1>" + body),
				"0, 0, l, t");

		// add combobox
		IPNMatchInstancesLogReplayAlgorithm[] availAlgorithms = new IPNMatchInstancesLogReplayAlgorithm[2];
		availAlgorithms[0] = new CompleteCostBasedPNMatchInstancesReplayAlgorithm(); 
		availAlgorithms[1] = new CostBasedPNMatchInstancesReplayAlgorithm();

		combo = new JComboBox(availAlgorithms);
		combo.setPreferredSize(new Dimension(150, 25));
		combo.setSize(new Dimension(150, 25));
		combo.setMinimumSize(new Dimension(150, 25));
		combo.setSelectedItem(0);
		combo.setUI(new SlickerComboBoxUI());
		add(combo, "0, 1");
	}

	public IPNMatchInstancesLogReplayAlgorithm getAlgorithm() {
		return (IPNMatchInstancesLogReplayAlgorithm) combo.getSelectedItem();
	}

}
