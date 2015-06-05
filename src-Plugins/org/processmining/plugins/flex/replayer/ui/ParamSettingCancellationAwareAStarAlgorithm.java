/**
 * 
 */
package org.processmining.plugins.flex.replayer.ui;

import info.clearthought.layout.TableLayout;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.processmining.plugins.flex.replayer.algorithms.IFlexLogReplayAlgorithm;

import com.fluxicon.slickerbox.components.NiceIntegerSlider;
import com.fluxicon.slickerbox.components.NiceSlider.Orientation;
import com.fluxicon.slickerbox.components.SlickerTabbedPane;
import com.fluxicon.slickerbox.factory.SlickerFactory;

/**
 * @author Arya Adriansyah
 * @email a.adriansyah@tue.nl
 * @version Mar 23, 2011
 */
public class ParamSettingCancellationAwareAStarAlgorithm extends ParamSettingStep {
	private static final long serialVersionUID = 7536753974291553887L;

	// name of variable
	public static int ISTESTINGMODEINTVAL = 0; // 0 if replay mode, 1 if testing
	// mode
	public static int MAXEXPLOREDINSTANCESINTVAL = 1; // maximum possible
	// instance explored before exploration stops
	// cost variables
	public static int INAPPROPRIATETRANSFIRECOST = 2; // transitions that fire
	// without proper predecessors
	public static int HEURISTICDISTANCECOST = 3; // number of events still left
	// to be replayed
	public static int SKIPPEDEVENTCOST = 4; // number of events that are ignored
	// in replay
	public static int REPLAYEDEVENTCOST = 5; // number of replayed events
	public static int INITIATIVEINVISTASKCOST = 6; // number of invisible tasks
	// that are executed without any occurrence of its corresponding events.
	public static int INITIATIVEREALTASKCOST = 7; // number of tasks that are
	// executed without any occurrence of its corresponding events.

	public static int FILELOCATIONSTRVAL = 8; // location of file to store
	// testing result
	public static int ANALYSISTYPEINTVAL = 9; // analysis type
	// (REMOVEEVENTANALYSIS/PREFIXANALYSIS/INVERTPREFIXANALYSIS)

	// permittable replay actions
	public static int ALLOWINVITASK = 10;
	public static int ALLOWREALTASK = 11;
	public static int ALLOWEVENTSKIP = 12;
	public static int ALLOWTASKEXECWOTOKENS = 13;
	public static int ALLOWINAPPROPRIATEFIRING = 14;

	// default replay value
	private static int DEFREPLAYEDEVENTCOST = 1;
	private static int DEFINAPPROPRIATETRANSFIRECOST = 6;
	private static int DEFINITIATIVEREALTASKCOST = 2;
	private static int DEFINITIATIVEINVISTASKCOST = 0;
	private static int DEFSKIPPEDEVENTCOST = 5;
	private static int DEFLIMMAXNUMINSTANCES = 500000;
	private static int MAXLIMMAXNUMINSTANCES = 1500000;

	// overall layout
	private SlickerTabbedPane tabPane; // tab basic/advance
	private JPanel advancedPanel;
	private JPanel basicPanel;

	/**
	 * BASIC PANEL
	 */
	private boolean useBasic = true;

	private JCheckBox basIdentifyInvi;
	private JCheckBox basIdentifyReal;
	private JCheckBox basMaxInstance;
	private NiceIntegerSlider limMaxEvents;

	// testing mode/replay mode
	private boolean isTestingMode = false;

	/**
	 * ADVANCED PANEL
	 */
	// inappropriate transitions fire
	private ButtonGroup inappropriateFiringGroup;
	private JRadioButton yesInappropriateFiring;
	private JRadioButton noInappropriateFiring;
	private NiceIntegerSlider unsatisfiedEventsSlider;

	// skipping an event
	private ButtonGroup skipEventGroup;
	private JRadioButton yesSkipEvent;
	private JRadioButton noSkipEvent;
	private NiceIntegerSlider skipEventSlider;

	// initiated execute invi task
	private ButtonGroup execInviTaskEventsGroup;
	private JRadioButton yesExecInviTask;
	private JRadioButton noExecInviTask;
	private NiceIntegerSlider execInviTaskSlider;

	// initiated execute real task
	private ButtonGroup execRealTaskEventsGroup;
	private JRadioButton yesExecRealTask;
	private JRadioButton noExecRealTask;
	private NiceIntegerSlider execRealTaskSlider;

	// specific elements
	// Testing selection
	private ButtonGroup testTypeGroup;
	private JRadioButton prefixType;
	private JRadioButton invertedPrefixType;
	private JRadioButton eventRemovalType;

	// use max instances limitation
	private NiceIntegerSlider maxExpInstSlider;

	// file output location (only for analysis)
	private JFileChooser fc;
	private JTextField fl;
	private JButton fs;

	public ParamSettingCancellationAwareAStarAlgorithm(boolean isTestingMode) {
		super(isTestingMode);
		initComponents(isTestingMode);
	}

	public boolean precondition() {
		return true;
	}

	public void readSettings(IFlexLogReplayAlgorithm algorithm) {
	}

	private void initComponents(boolean isTestingMode) {
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
		sizeBasic = new double[][] { { 400, 350 }, { 100, 30, 30, 30, 30, 30, 60, 30, 30, 30, 35, 30 } };
		basicPanel.setLayout(new TableLayout(sizeBasic));
		basicPanel.add(
				slickerFactory.createLabel("<html><h1>Configure cost</h1><p>Check appropriate options</p></html>"),
				"0, 0, 1, 0, l, t");

		// init basic instance
		advancedPanel = new JPanel();
		advancedPanel.setBackground(new Color(200, 200, 200));

		basIdentifyInvi = slickerFactory.createCheckBox("Identify unobservable activities", true);
		basIdentifyReal = slickerFactory.createCheckBox("Identify skipped activities", true);
		basMaxInstance = slickerFactory.createCheckBox("Use max instances limitation", true);

		basicPanel.add(slickerFactory
				.createLabel("<html><h2>How would you like the conformance checking to be performed?</h2></html>"),
				"0, " + String.valueOf(basicRowIndex) + ", 1, " + String.valueOf(basicRowIndex));
		basicRowIndex++;
		basicPanel.add(basIdentifyInvi, "0, " + String.valueOf(basicRowIndex++));
		basicPanel.add(basIdentifyReal, "0, " + String.valueOf(basicRowIndex++));
		basicRowIndex++;

		basicPanel.add(slickerFactory.createLabel("<html><h2>Additional replay configuration</h2></html>"), "0, "
				+ String.valueOf(basicRowIndex++));
		limMaxEvents = slickerFactory.createNiceIntegerSlider("", 1000, MAXLIMMAXNUMINSTANCES, DEFLIMMAXNUMINSTANCES,
				Orientation.HORIZONTAL);
		limMaxEvents.setPreferredSize(new Dimension(300, 20));
		limMaxEvents.setMaximumSize(new Dimension(300, 20));
		basicPanel.add(basMaxInstance, "0, " + String.valueOf(basicRowIndex));
		basicPanel.add(limMaxEvents, "1, " + String.valueOf(basicRowIndex++));

		int rowIndex = 1;

		double size[][];
		if (!isTestingMode) {
			size = new double[][] { { 400, 350 }, { 45, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30 } };
		} else {
			size = new double[][] {
					{ 400, 350 },
					{ 45, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30,
							30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30 } };
		}
		advancedPanel.setLayout(new TableLayout(size));

		advancedPanel.add(slickerFactory.createLabel("<html><h1>Configure cost</h1>"), "0, 0, 1, 0, l, t");

		// max num of explored instances
		maxExpInstSlider = slickerFactory.createNiceIntegerSlider("", 1, MAXLIMMAXNUMINSTANCES, DEFLIMMAXNUMINSTANCES,
				Orientation.HORIZONTAL);
		maxExpInstSlider.setPreferredSize(new Dimension(200, 20));
		maxExpInstSlider.setMaximumSize(new Dimension(200, 20));
		advancedPanel.add(slickerFactory.createLabel("<html><h2>Max # instances</h2></html>"),
				"0, " + String.valueOf(rowIndex));
		advancedPanel.add(maxExpInstSlider, "1, " + String.valueOf(rowIndex++));

		// skipping event cost
		skipEventSlider = slickerFactory
				.createNiceIntegerSlider("", 1, 50, DEFSKIPPEDEVENTCOST, Orientation.HORIZONTAL);
		skipEventSlider.setPreferredSize(new Dimension(200, 20));
		skipEventSlider.setMaximumSize(new Dimension(200, 20));
		skipEventGroup = new ButtonGroup();
		yesSkipEvent = slickerFactory.createRadioButton("Identify inserted activities");
		noSkipEvent = slickerFactory.createRadioButton("Don't identify inserted activities");
		noSkipEvent.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (noInappropriateFiring.isSelected()) {
					JOptionPane
							.showMessageDialog(
									null,
									"Either \"Identifying inserted activities\" or \"Allowing violating sync log+model\" must be allowed",
									"Alert", JOptionPane.ERROR_MESSAGE);
					yesSkipEvent.setSelected(true);
				}
			}
		});

		skipEventGroup.add(yesSkipEvent);
		skipEventGroup.add(noSkipEvent);
		yesSkipEvent.setSelected(true);

		advancedPanel.add(slickerFactory.createLabel("<html><h2>Cost of an Inserted Activity</h2>"), "0, " + rowIndex
				+ ", 1, " + String.valueOf(rowIndex++));
		advancedPanel.add(noSkipEvent, "0, " + String.valueOf(rowIndex++));
		advancedPanel.add(yesSkipEvent, "0, " + String.valueOf(rowIndex));
		advancedPanel.add(skipEventSlider, "1, " + String.valueOf(rowIndex++));

		// initiated execute invi task
		execInviTaskSlider = slickerFactory.createNiceIntegerSlider("", 0, 50, DEFINITIATIVEINVISTASKCOST,
				Orientation.HORIZONTAL);
		execInviTaskSlider.setPreferredSize(new Dimension(200, 20));
		execInviTaskSlider.setMaximumSize(new Dimension(200, 20));
		execInviTaskEventsGroup = new ButtonGroup();
		yesExecInviTask = slickerFactory.createRadioButton("Identify unobservable activities");
		noExecInviTask = slickerFactory.createRadioButton("Don't identify unobservable activities");
		execInviTaskEventsGroup.add(yesExecInviTask);
		execInviTaskEventsGroup.add(noExecInviTask);
		yesExecInviTask.setSelected(true);

		advancedPanel.add(slickerFactory.createLabel("<html><h2>Cost of an Unobservable Activities</h2>"), "0, "
				+ rowIndex + ", 1, " + String.valueOf(rowIndex++));
		advancedPanel.add(noExecInviTask, "0, " + String.valueOf(rowIndex++));
		advancedPanel.add(yesExecInviTask, "0, " + String.valueOf(rowIndex));
		advancedPanel.add(execInviTaskSlider, "1, " + String.valueOf(rowIndex++));

		// initiated execute real task
		execRealTaskSlider = slickerFactory.createNiceIntegerSlider("", 1, 50, DEFINITIATIVEREALTASKCOST,
				Orientation.HORIZONTAL);
		execRealTaskSlider.setPreferredSize(new Dimension(200, 20));
		execRealTaskSlider.setMaximumSize(new Dimension(200, 20));
		execRealTaskEventsGroup = new ButtonGroup();
		yesExecRealTask = slickerFactory.createRadioButton("Identify skipped activities");
		noExecRealTask = slickerFactory.createRadioButton("Don't identify skipped activities");
		execRealTaskEventsGroup.add(yesExecRealTask);
		execRealTaskEventsGroup.add(noExecRealTask);
		yesExecRealTask.setSelected(true);

		advancedPanel.add(slickerFactory.createLabel("<html><h2>Cost of a Skipped Activity</h2>"), "0, " + rowIndex
				+ ", 1, " + String.valueOf(rowIndex++));
		advancedPanel.add(noExecRealTask, "0, " + String.valueOf(rowIndex++));
		advancedPanel.add(yesExecRealTask, "0, " + String.valueOf(rowIndex));
		advancedPanel.add(execRealTaskSlider, "1, " + String.valueOf(rowIndex++));

		// inappropriate firing
		unsatisfiedEventsSlider = slickerFactory.createNiceIntegerSlider("", 1, 50, DEFINAPPROPRIATETRANSFIRECOST,
				Orientation.HORIZONTAL);
		unsatisfiedEventsSlider.setPreferredSize(new Dimension(200, 20));
		unsatisfiedEventsSlider.setMaximumSize(new Dimension(200, 20));

		inappropriateFiringGroup = new ButtonGroup();
		yesInappropriateFiring = slickerFactory.createRadioButton("Allow violating sync log+model move");
		noInappropriateFiring = slickerFactory.createRadioButton("Don't allow violating sync log+model move");
		noInappropriateFiring.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (noSkipEvent.isSelected()) {
					JOptionPane
							.showMessageDialog(
									null,
									"Either \"Identifying inserted activities\" or \"Allowing violating sync log+model\" must be allowed",
									"Alert", JOptionPane.ERROR_MESSAGE);
					yesInappropriateFiring.setSelected(true);
				}
			}
		});
		inappropriateFiringGroup.add(yesInappropriateFiring);
		inappropriateFiringGroup.add(noInappropriateFiring);
		noInappropriateFiring.setSelected(true);

		advancedPanel.add(
				slickerFactory.createLabel("<html><h2>Cost of a Violating Log+Model Synchronous Activity</h2></html>"),
				"0, " + rowIndex + ", 1, " + String.valueOf(rowIndex++));
		advancedPanel.add(noInappropriateFiring, "0, " + String.valueOf(rowIndex++));
		advancedPanel.add(yesInappropriateFiring, "0, " + String.valueOf(rowIndex));
		advancedPanel.add(unsatisfiedEventsSlider, "1, " + String.valueOf(rowIndex++));

		if (isTestingMode) {
			this.isTestingMode = true;

			// selection of testing mode (event removal or prefix)
			testTypeGroup = new ButtonGroup();
			prefixType = slickerFactory.createRadioButton("Prefix testing (small prefixes to whole trace)");
			invertedPrefixType = slickerFactory
					.createRadioButton("Inverted prefix testing (whole trace to small prefixes)");
			eventRemovalType = slickerFactory.createRadioButton("Event removal testing");
			testTypeGroup.add(prefixType);
			testTypeGroup.add(invertedPrefixType);
			testTypeGroup.add(eventRemovalType);
			prefixType.setSelected(true);

			advancedPanel.add(slickerFactory.createLabel("<html><h2>Select testing type</h2></html>"),
					"0, " + String.valueOf(rowIndex++));
			advancedPanel.add(eventRemovalType, "0, " + String.valueOf(rowIndex++));
			advancedPanel.add(prefixType, "0, " + String.valueOf(rowIndex++));
			advancedPanel.add(invertedPrefixType, "0, " + String.valueOf(rowIndex++));

			fc = new JFileChooser();

			fl = new JTextField();
			fl.setPreferredSize(new Dimension(300, 20));
			fl.setEditable(false);

			fs = slickerFactory.createButton("Browse...");
			fs.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					int i = fc.showSaveDialog(new JFrame());
					if (i == JFileChooser.APPROVE_OPTION) {
						// TODO: check if file is empty, set default file name
						// and location
						fl.setText(fc.getSelectedFile().getAbsolutePath() + ".csv");
					}
				}
			});
			JPanel inPanel = slickerFactory.createGradientPanel();
			inPanel.add(fl);
			inPanel.add(fs);

			advancedPanel.add(slickerFactory.createLabel("<html><h2>Output file location</h2></html>"),
					"0, " + String.valueOf(rowIndex++));
			advancedPanel.add(inPanel, "0, " + String.valueOf(rowIndex++));
		} else {
			this.isTestingMode = false;
		}

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
		Object[] parameters = new Object[15];

		if (useBasic) {
			parameters[ParamSettingCancellationAwareAStarAlgorithm.INAPPROPRIATETRANSFIRECOST] = DEFINAPPROPRIATETRANSFIRECOST;
			parameters[ParamSettingCancellationAwareAStarAlgorithm.ALLOWINAPPROPRIATEFIRING] = false;
			parameters[ParamSettingCancellationAwareAStarAlgorithm.REPLAYEDEVENTCOST] = DEFREPLAYEDEVENTCOST;
			parameters[ParamSettingCancellationAwareAStarAlgorithm.HEURISTICDISTANCECOST] = DEFREPLAYEDEVENTCOST;

			parameters[ParamSettingCancellationAwareAStarAlgorithm.SKIPPEDEVENTCOST] = DEFSKIPPEDEVENTCOST;
			parameters[ParamSettingCancellationAwareAStarAlgorithm.ALLOWEVENTSKIP] = true;

			parameters[ParamSettingCancellationAwareAStarAlgorithm.INITIATIVEINVISTASKCOST] = basIdentifyInvi
					.isSelected() ? DEFINITIATIVEINVISTASKCOST : 0;
			parameters[ParamSettingCancellationAwareAStarAlgorithm.ALLOWINVITASK] = basIdentifyInvi.isSelected();

			parameters[ParamSettingCancellationAwareAStarAlgorithm.INITIATIVEREALTASKCOST] = yesExecRealTask
					.isSelected() ? DEFINITIATIVEREALTASKCOST : 0;
			parameters[ParamSettingCancellationAwareAStarAlgorithm.ALLOWREALTASK] = basIdentifyReal.isSelected();

			parameters[ParamSettingCancellationAwareAStarAlgorithm.MAXEXPLOREDINSTANCESINTVAL] = basMaxInstance
					.isSelected() ? limMaxEvents.getValue() : MAXLIMMAXNUMINSTANCES;

			parameters[ParamSettingCancellationAwareAStarAlgorithm.ALLOWTASKEXECWOTOKENS] = false;

		} else {
			parameters[ParamSettingCancellationAwareAStarAlgorithm.ALLOWINAPPROPRIATEFIRING] = yesInappropriateFiring
					.isSelected();
			parameters[ParamSettingCancellationAwareAStarAlgorithm.INAPPROPRIATETRANSFIRECOST] = yesInappropriateFiring
					.isSelected() ? unsatisfiedEventsSlider.getValue() : 0;

			parameters[ParamSettingCancellationAwareAStarAlgorithm.REPLAYEDEVENTCOST] = DEFREPLAYEDEVENTCOST;
			parameters[ParamSettingCancellationAwareAStarAlgorithm.HEURISTICDISTANCECOST] = DEFREPLAYEDEVENTCOST;

			parameters[ParamSettingCancellationAwareAStarAlgorithm.SKIPPEDEVENTCOST] = yesSkipEvent.isSelected() ? skipEventSlider
					.getValue() : 0;
			parameters[ParamSettingCancellationAwareAStarAlgorithm.ALLOWEVENTSKIP] = yesSkipEvent.isSelected();

			parameters[ParamSettingCancellationAwareAStarAlgorithm.INITIATIVEINVISTASKCOST] = yesExecInviTask
					.isSelected() ? execInviTaskSlider.getValue() : 0;
			parameters[ParamSettingCancellationAwareAStarAlgorithm.ALLOWINVITASK] = yesExecInviTask.isSelected();

			parameters[ParamSettingCancellationAwareAStarAlgorithm.INITIATIVEREALTASKCOST] = yesExecRealTask
					.isSelected() ? execRealTaskSlider.getValue() : 0;
			parameters[ParamSettingCancellationAwareAStarAlgorithm.ALLOWREALTASK] = yesExecRealTask.isSelected();

			parameters[ParamSettingCancellationAwareAStarAlgorithm.MAXEXPLOREDINSTANCESINTVAL] = maxExpInstSlider
					.getValue();

			parameters[ParamSettingCancellationAwareAStarAlgorithm.ALLOWTASKEXECWOTOKENS] = false;
		}

		parameters[ParamSettingCancellationAwareAStarAlgorithm.ISTESTINGMODEINTVAL] = isTestingMode;
		return parameters;
	}

	@Override
	public Object getParameterValue(int paramVariableValIndex) {
		if (paramVariableValIndex == ParamSettingCancellationAwareAStarAlgorithm.MAXEXPLOREDINSTANCESINTVAL) {
			if (useBasic) {
				if (basMaxInstance.isSelected()) {
					return limMaxEvents.getValue();
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
