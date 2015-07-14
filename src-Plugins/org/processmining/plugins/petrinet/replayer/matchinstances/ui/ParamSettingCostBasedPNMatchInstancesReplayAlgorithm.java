/**
 * 
 */
package org.processmining.plugins.petrinet.replayer.matchinstances.ui;

import info.clearthought.layout.TableLayout;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.processmining.plugins.petrinet.replayer.matchinstances.algorithms.CostBasedPNMatchInstancesReplayAlgorithm;

import com.fluxicon.slickerbox.components.NiceIntegerSlider;
import com.fluxicon.slickerbox.components.NiceSlider.Orientation;
import com.fluxicon.slickerbox.components.SlickerTabbedPane;
import com.fluxicon.slickerbox.factory.SlickerFactory;

/**
 * @author aadrians
 * 
 */
public class ParamSettingCostBasedPNMatchInstancesReplayAlgorithm extends PNParamSettingStep {
	private static final long serialVersionUID = 558723509846707248L;

	// default replay value
	private static int DEFMOVEONMODELREALCOST = 2;
	private static int DEFMOVEONMODELINVICOST = 0;
	private static int DEFMOVEONLOGCOST = 5;
	private static int DEFLIMMAXNUMINSTANCES = 1000000;
	private static int MAXLIMMAXNUMINSTANCES = 5000000;

	// overall layout
	private SlickerTabbedPane tabPane; // tab basic/advance
	private JPanel advancedPanel;
	private JPanel basicPanel;

	/**
	 * BASIC PANEL
	 */
	private boolean useBasic = true;

	private JCheckBox basAllowMoveOnModelInvi;
	private JCheckBox basAllowMoveOnModelReal;
	private JCheckBox basMaxInstance;
	private NiceIntegerSlider basLimMaxEvents;

	/**
	 * ADVANCED PANEL
	 */
	// move on log
	private ButtonGroup moveOnLogGroup;
	private JRadioButton yesMoveOnLog;
	private JRadioButton noMoveOnLog;
	private NiceIntegerSlider moveOnLogSlider;

	// initiated execute invi task
	private ButtonGroup moveOnModelInviGroup;
	private JRadioButton yesMoveOnModelInvi;
	private JRadioButton noMoveOnModelInvi;
	private NiceIntegerSlider moveOnModelInviSlider;

	// initiated execute real task
	private ButtonGroup moveOnModelRealGroup;
	private JRadioButton yesMoveOnModelReal;
	private JRadioButton noMoveOnModelReal;
	private NiceIntegerSlider moveOnModelRealSlider;

	// use max instances limitation
	private NiceIntegerSlider maxExpInstSlider;

	public ParamSettingCostBasedPNMatchInstancesReplayAlgorithm() {
		super();
		initComponents();
	}

	private void initComponents() {
		// init tab
		tabPane = new SlickerTabbedPane("Choose wizard", new Color(200, 200, 200, 230), new Color(0, 0, 0, 230),
				new Color(220, 220, 220, 150));

		SlickerFactory slickerFactory = SlickerFactory.instance();

		// init advance configuration
		basicPanel = new JPanel();
		basicPanel.setBackground(new Color(200, 200, 200));
		basicPanel.setSize(700, 465);

		int basicRowIndex = 1;
		double sizeBasic[][];
		sizeBasic = new double[][] { { 400, 350 },
				{ 100, 30, 30, 30, 30, 30, 60, 30, 30, 30, 35, 30 } };
		basicPanel.setLayout(new TableLayout(sizeBasic));
		basicPanel.add(
				slickerFactory.createLabel("<html><h1>Configure cost</h1><p>Check appropriate options</p></html>"),
				"0, 0, 1, 0, l, t");

		// init basic instance
		advancedPanel = new JPanel();
		advancedPanel.setBackground(new Color(200, 200, 200));
		advancedPanel.setSize(700, 465);

		basAllowMoveOnModelInvi = slickerFactory.createCheckBox("Identify unobservable activities", true);
		basAllowMoveOnModelReal = slickerFactory.createCheckBox("Identify skipped activities", true);
		basMaxInstance = slickerFactory.createCheckBox("Use max instances limitation", true);

		basicPanel.add(slickerFactory.createLabel("<html><h2>What's your goal?</h2></html>"),
				"0, " + String.valueOf(basicRowIndex++));
		basicPanel.add(basAllowMoveOnModelInvi, "0, " + String.valueOf(basicRowIndex++));
		basicPanel.add(basAllowMoveOnModelReal, "0, " + String.valueOf(basicRowIndex++));
		basicRowIndex++;

		basicPanel.add(slickerFactory.createLabel("<html><h2>Additional replay configuration</h2></html>"), "0, "
				+ String.valueOf(basicRowIndex++));
		basLimMaxEvents = slickerFactory.createNiceIntegerSlider("", 1000, MAXLIMMAXNUMINSTANCES, DEFLIMMAXNUMINSTANCES,
				Orientation.HORIZONTAL);
		basLimMaxEvents.setPreferredSize(new Dimension(300, 20));
		basLimMaxEvents.setMaximumSize(new Dimension(300, 20));
		basicPanel.add(basMaxInstance, "0, " + String.valueOf(basicRowIndex));
		basicPanel.add(basLimMaxEvents, "1, " + String.valueOf(basicRowIndex++));

		int rowIndex = 1;

		double size[][] = new double[][] { { 400, 350 },
				{ 100, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 65 } };
		advancedPanel.setLayout(new TableLayout(size));
		advancedPanel.add(slickerFactory.createLabel("<html><h1>Configure cost</h1><p>Set the costs by moving slider. Setting # max. instances to its maximum value means setting it to unlimited</p></html>"), "0, 0, 1, 0, l, t");

		// max num of explored instances
		maxExpInstSlider = slickerFactory.createNiceIntegerSlider("", 1, MAXLIMMAXNUMINSTANCES, DEFLIMMAXNUMINSTANCES,
				Orientation.HORIZONTAL);
		maxExpInstSlider.setPreferredSize(new Dimension(200, 20));
		maxExpInstSlider.setMaximumSize(new Dimension(200, 20));
		advancedPanel.add(slickerFactory.createLabel("<html><h2>Max # instances</h2></html>"),
				"0, " + String.valueOf(rowIndex));
		advancedPanel.add(maxExpInstSlider, "1, " + String.valueOf(rowIndex++));

		// skipping event cost
		moveOnLogSlider = slickerFactory
				.createNiceIntegerSlider("", 1, 50, DEFMOVEONLOGCOST, Orientation.HORIZONTAL);
		moveOnLogSlider.setPreferredSize(new Dimension(200, 20));
		moveOnLogSlider.setMaximumSize(new Dimension(200, 20));
		moveOnLogSlider.setMinimumSize(new Dimension(200, 20));
		moveOnLogGroup = new ButtonGroup();
		yesMoveOnLog = slickerFactory.createRadioButton("Do move on log");
		noMoveOnLog = slickerFactory.createRadioButton("Don't do move on log");
		moveOnLogGroup.add(yesMoveOnLog);
		moveOnLogGroup.add(noMoveOnLog);
		yesMoveOnLog.setSelected(true);

		advancedPanel.add(slickerFactory.createLabel("<html><h2>Cost of move on log</h2>"), "0, " + rowIndex
				+ ", 1, " + String.valueOf(rowIndex++));
		advancedPanel.add(noMoveOnLog, "0, " + String.valueOf(rowIndex++));
		advancedPanel.add(yesMoveOnLog, "0, " + String.valueOf(rowIndex));
		advancedPanel.add(moveOnLogSlider, "1, " + String.valueOf(rowIndex++));

		// initiated execute invi task
		moveOnModelInviSlider = slickerFactory.createNiceIntegerSlider("", 0, 50, DEFMOVEONMODELINVICOST,
				Orientation.HORIZONTAL);
		moveOnModelInviSlider.setPreferredSize(new Dimension(200, 20));
		moveOnModelInviSlider.setMaximumSize(new Dimension(200, 20));
		moveOnModelInviGroup = new ButtonGroup();
		yesMoveOnModelInvi = slickerFactory.createRadioButton("Do move on model (invi)");
		noMoveOnModelInvi = slickerFactory.createRadioButton("Don't do move on model (invi)");
		moveOnModelInviGroup.add(yesMoveOnModelInvi);
		moveOnModelInviGroup.add(noMoveOnModelInvi);
		yesMoveOnModelInvi.setSelected(true);

		advancedPanel.add(slickerFactory.createLabel("<html><h2>Cost of move on model (invi)</h2>"), "0, "
				+ rowIndex + ", 1, " + String.valueOf(rowIndex++));
		advancedPanel.add(noMoveOnModelInvi, "0, " + String.valueOf(rowIndex++));
		advancedPanel.add(yesMoveOnModelInvi, "0, " + String.valueOf(rowIndex));
		advancedPanel.add(moveOnModelInviSlider, "1, " + String.valueOf(rowIndex++));

		// initiated execute real task
		moveOnModelRealSlider = slickerFactory.createNiceIntegerSlider("", 1, 50, DEFMOVEONMODELREALCOST,
				Orientation.HORIZONTAL);
		moveOnModelRealSlider.setPreferredSize(new Dimension(200, 20));
		moveOnModelRealSlider.setMaximumSize(new Dimension(200, 20));
		moveOnModelRealGroup = new ButtonGroup();
		yesMoveOnModelReal = slickerFactory.createRadioButton("Do move on model");
		noMoveOnModelReal = slickerFactory.createRadioButton("Don't do move on model");
		moveOnModelRealGroup.add(yesMoveOnModelReal);
		moveOnModelRealGroup.add(noMoveOnModelReal);
		yesMoveOnModelReal.setSelected(true);

		advancedPanel.add(slickerFactory.createLabel("<html><h2>Cost of move on model</h2>"), "0, " + rowIndex
				+ ", 1, " + String.valueOf(rowIndex++));
		advancedPanel.add(noMoveOnModelReal, "0, " + String.valueOf(rowIndex++));
		advancedPanel.add(yesMoveOnModelReal, "0, " + String.valueOf(rowIndex));
		advancedPanel.add(moveOnModelRealSlider, "1, " + String.valueOf(rowIndex++));

		// add all tabs
		tabPane.addTab("Basic wizard", basicPanel, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				useBasic = true;
			}
		});
		tabPane.addTab("Advanced", advancedPanel, new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				useBasic = false;
			}
		});
		add(tabPane);
	}

	@Override
	public Object[] getAllParameters() {
		Object[] parameters = new Object[7];

		if (useBasic) {
			parameters[CostBasedPNMatchInstancesReplayAlgorithm.MOVEONLOGCOST] = DEFMOVEONLOGCOST;

			parameters[CostBasedPNMatchInstancesReplayAlgorithm.ALLOWMOVEONLOG] = true;

			parameters[CostBasedPNMatchInstancesReplayAlgorithm.MOVEONMODELINVICOST] = basAllowMoveOnModelInvi.isSelected() ? DEFMOVEONMODELINVICOST
					: 0;
			parameters[CostBasedPNMatchInstancesReplayAlgorithm.ALLOWMOVEONMODELINVI] = basAllowMoveOnModelInvi.isSelected();

			parameters[CostBasedPNMatchInstancesReplayAlgorithm.MOVEONMODELREALCOST] = yesMoveOnModelReal.isSelected() ? DEFMOVEONMODELREALCOST
					: 0;
			parameters[CostBasedPNMatchInstancesReplayAlgorithm.ALLOWMOVEONMODELREAL] = basAllowMoveOnModelReal.isSelected();

			parameters[CostBasedPNMatchInstancesReplayAlgorithm.MAXEXPLOREDINSTANCESINTVAL] = basMaxInstance.isSelected() ? basLimMaxEvents
					.getValue() : MAXLIMMAXNUMINSTANCES;

		} else {
			parameters[CostBasedPNMatchInstancesReplayAlgorithm.MOVEONLOGCOST] = yesMoveOnLog.isSelected() ? moveOnLogSlider
					.getValue() : 0;
			parameters[CostBasedPNMatchInstancesReplayAlgorithm.ALLOWMOVEONLOG] = yesMoveOnLog.isSelected();

			parameters[CostBasedPNMatchInstancesReplayAlgorithm.MOVEONMODELINVICOST] = yesMoveOnModelInvi.isSelected() ? moveOnModelInviSlider
					.getValue() : 0;
			parameters[CostBasedPNMatchInstancesReplayAlgorithm.ALLOWMOVEONMODELINVI] = yesMoveOnModelInvi.isSelected();

			parameters[CostBasedPNMatchInstancesReplayAlgorithm.MOVEONMODELREALCOST] = yesMoveOnModelReal.isSelected() ? moveOnModelRealSlider
					.getValue() : 0;
			parameters[CostBasedPNMatchInstancesReplayAlgorithm.ALLOWMOVEONMODELREAL] = yesMoveOnModelReal.isSelected();

			parameters[CostBasedPNMatchInstancesReplayAlgorithm.MAXEXPLOREDINSTANCESINTVAL] = maxExpInstSlider.getValue() == MAXLIMMAXNUMINSTANCES ? Integer.MAX_VALUE : maxExpInstSlider.getValue();
		}
		return parameters;
	}

	@Override
	public Object getParameterValue(int paramVariableValIndex) {
		if (paramVariableValIndex == CostBasedPNMatchInstancesReplayAlgorithm.MAXEXPLOREDINSTANCESINTVAL) {
			if (useBasic) {
				if (basMaxInstance.isSelected()) {
					return basLimMaxEvents.getValue();
				} else {
					return Integer.MAX_VALUE;
				}
			} else {
				return maxExpInstSlider.getValue();
			}
		}
		return null;
	}

}
