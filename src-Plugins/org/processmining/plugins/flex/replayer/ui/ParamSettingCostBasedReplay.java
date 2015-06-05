package org.processmining.plugins.flex.replayer.ui;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.processmining.plugins.flex.replayer.algorithms.IFlexLogReplayAlgorithm;

import com.fluxicon.slickerbox.components.NiceIntegerSlider;
import com.fluxicon.slickerbox.components.NiceSlider.Orientation;
import com.fluxicon.slickerbox.factory.SlickerFactory;

/**
 * @author arya
 * @email arya.adriansyah@gmail.com
 * @version Dec 1, 2009
 */
public class ParamSettingCostBasedReplay extends ParamSettingStep {
	private static final long serialVersionUID = -4318663984761515562L;

	// name of variable
	public static int ISTESTINGMODEINTVAL = 0; // 0 if replay mode, 1 if testing mode
	public static int MAXPOSSIBLESTATESINTVAL = 1; // boundary of maximum possible states if replay is going to be performed
	public static int MAXEXPLOREDINSTANCESINTVAL = 2; // maximum possible instance explored before exploration stops
	
	// cost variables
	public static int UNSATISFIEDEVENTSCOST = 3;	// event that executed without proper predecessors
	public static int HEURISTICDISTANCECOST = 4; // number of events still left to be replayed
	public static int SKIPPEDEVENTCOST = 5;  // number of events that are ignored in replay
	public static int REPLAYEDEVENTCOST = 6;  // number of replayed events
	public static int INITIATIVEINVISTASKCOST = 7;  // number of invisible tasks that are executed without any occurrence of its corresponding events.
	public static int INITIATIVEREALTASKCOST = 8;  // number of tasks that are executed without any occurrence of its corresponding events.
	public static int ACCUMULATEDUNHANDLEDARCSCOST = 9; // accumulation of number of accumulated unhandled events
	public static int UNACCUMULATEDUNHANDLEDARCSCOST = 10; // unaccumulated unhandled arcs cost
	
	public static int FILELOCATIONSTRVAL = 11; //location of file to store testing result
	public static int ANALYSISTYPEINTVAL = 12; // analysis type (REMOVEEVENTANALYSIS/PREFIXANALYSIS/INVERTPREFIXANALYSIS)
	
	// testing mode/replay mode
	private boolean isTestingMode = false;
	
	// UnsatisfiedEvents
	private ButtonGroup unsatisfiedEventsGroup;
	private JRadioButton yesUnsatisfiedEvents;
	private JRadioButton noUnsatisfiedEvents;
	private NiceIntegerSlider unsatisfiedEventsSlider;

	// heuristic distance to finish
	private ButtonGroup heurDistanceGroup;
	private JRadioButton yesHeurDistance;
	private JRadioButton noHeurDistance;
	private NiceIntegerSlider heurDistanceSlider;

	// replayed event
	private ButtonGroup replayedEvtGroup;
	private JRadioButton yesReplayedEvtCost;
	private JRadioButton noReplayedEvtCost;
	private NiceIntegerSlider replayedEvtSlider;

	// accumulated UnhandledArcs
	private ButtonGroup notAccUnhandledArcsGroup;
	private JRadioButton yesAccUnhandledArcs;
	private JRadioButton noAccUnhandledArcs;
	private NiceIntegerSlider accUnhandledArcsSlider;

	// skipping an event
	private ButtonGroup skipEventGroup;
	private JRadioButton yesSkipEvent;
	private JRadioButton noSkipEvent;
	private NiceIntegerSlider skipEventSlider;
	
	// not accumulated UnhandledArcs
	private ButtonGroup unhandledArcsGroup;
	private JRadioButton yesUnhandledArcs;
	private JRadioButton noUnhandledArcs;
	private NiceIntegerSlider unhandledArcsSlider;

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
	private ButtonGroup maxExpInstGroup;
	private JRadioButton yesMaxExpInst;
	private JRadioButton noMaxExpInst;
	private NiceIntegerSlider maxExpInstSlider;

	// use estimation to skip sequence
	private ButtonGroup skipLimEstimGroup;
	private JRadioButton yesSkipLimEstim;
	private JRadioButton noSkipLimEstim;
	private NiceIntegerSlider skipLimEstimSlider;

	// file output location (only for analysis)
	private JFileChooser fc;
	private JTextField fl;
	private JButton fs;


	public ParamSettingCostBasedReplay(boolean isTestingMode) {
		super(isTestingMode);
		initComponents(isTestingMode);
	}

	public boolean precondition() {
		return true;
	}

	public void readSettings(IFlexLogReplayAlgorithm algorithm) {}

	private void initComponents(boolean isTestingMode) {
		// init instance
		SlickerFactory slickerFactory = SlickerFactory.instance();
		int rowIndex = 1;

		double size[][]; 
		if (!isTestingMode){
			size = new double[][] { { TableLayoutConstants.FILL, TableLayoutConstants.FILL },
				{ 100, 30, 
			30, 30, 30, 30, 
			30, 30, 30, 30, 
			30, 30, 30, 30, 
			30, 30, 30, 30,
			30, 30, 30, 30,
			30, 30, 30, 30,
			30, 30, 30, 30, 30, 30 } };
		} else {
			size = new double[][] { { TableLayoutConstants.FILL, TableLayoutConstants.FILL },
					{ 120, 30, 
				30, 30, 30, 30, 
				30, 30, 30, 30, 
				30, 30, 30, 30, 
				30, 30, 30, 30,
				30, 30, 30, 30,
				30, 30, 30, 30,
				30, 30, 30, 30,
				30, 30, 30, 30 } };
		}
		setLayout(new TableLayout(size));
		
		String body = "<p>The cost of heuristic distance must be less/equal to the cost of replaying one event</p>";
		add(slickerFactory.createLabel("<html><h1>Configure cost</h1>" + body), "0, 0, 1, 0, l, t");
		
		// max num of explored instances
		maxExpInstSlider = slickerFactory.createNiceIntegerSlider("", 1000,
				500000, 20000, Orientation.HORIZONTAL);
		maxExpInstSlider.setPreferredSize(new Dimension(220, 20));
		maxExpInstSlider.setMaximumSize(new Dimension(220, 20));
		maxExpInstGroup = new ButtonGroup();
		yesMaxExpInst = slickerFactory.createRadioButton("Use limit");
		noMaxExpInst = slickerFactory
				.createRadioButton("No limit (set to MAXINT)");
		maxExpInstGroup.add(yesMaxExpInst);
		maxExpInstGroup.add(noMaxExpInst);
		yesMaxExpInst.setSelected(true);

		add(slickerFactory
				.createLabel("<html><h2>Max explored instances</h2></html>"),
				"0, " + String.valueOf(rowIndex++));
		add(noMaxExpInst, "0, " + String.valueOf(rowIndex++));
		add(yesMaxExpInst, "0, " + String.valueOf(rowIndex));
		add(maxExpInstSlider, "1, " + String.valueOf(rowIndex++));

		// skipping if estimated to be bounded
		skipLimEstimSlider = slickerFactory.createNiceIntegerSlider("", 1000,
				Integer.MAX_VALUE, Integer.MAX_VALUE, Orientation.HORIZONTAL);
		skipLimEstimSlider.setPreferredSize(new Dimension(220, 20));
		skipLimEstimSlider.setMaximumSize(new Dimension(220, 20));
		skipLimEstimGroup = new ButtonGroup();
		yesSkipLimEstim = slickerFactory.createRadioButton("Use limit");
		noSkipLimEstim = slickerFactory.createRadioButton("No limit");
		skipLimEstimGroup.add(yesSkipLimEstim);
		skipLimEstimGroup.add(noSkipLimEstim);
		noSkipLimEstim.setSelected(true);

		add(slickerFactory
						.createLabel("<html><h2>Skip if sequence has X possible instances?</h2></html>"),
				"0, " + String.valueOf(rowIndex++));
		add(noSkipLimEstim, "0, " + String.valueOf(rowIndex++));
		add(yesSkipLimEstim, "0, " + String.valueOf(rowIndex));
		add(skipLimEstimSlider, "1, " + String.valueOf(rowIndex++));

		// unsatisfied events cost
		unsatisfiedEventsSlider = slickerFactory.createNiceIntegerSlider("", 0, 3000, 1500, Orientation.HORIZONTAL);
		unsatisfiedEventsSlider.setPreferredSize(new Dimension(220, 20));
		unsatisfiedEventsGroup = new ButtonGroup();
		yesUnsatisfiedEvents = slickerFactory.createRadioButton("Use # unsatisfied events cost");
		noUnsatisfiedEvents = slickerFactory.createRadioButton("Don't use # unsatisfied events cost");
		unsatisfiedEventsGroup.add(yesUnsatisfiedEvents);
		unsatisfiedEventsGroup.add(noUnsatisfiedEvents);
		yesUnsatisfiedEvents.setSelected(true);

		add(slickerFactory.createLabel("<html><h2># Unsatisfied events cost</h2></html>"), "0, "
				+ String.valueOf(rowIndex++));
		add(noUnsatisfiedEvents, "0, " + String.valueOf(rowIndex++));
		add(yesUnsatisfiedEvents, "0, " + String.valueOf(rowIndex));
		add(unsatisfiedEventsSlider, "1, " + String.valueOf(rowIndex++));

		// heuristic distance cost 
		heurDistanceSlider = slickerFactory.createNiceIntegerSlider("", 0, 3000, 1000, Orientation.HORIZONTAL);
		heurDistanceSlider.setPreferredSize(new Dimension(220, 20));
		heurDistanceGroup = new ButtonGroup();
		yesHeurDistance = slickerFactory.createRadioButton("Use # events to be replayed cost");
		noHeurDistance = slickerFactory.createRadioButton("Don't use # events to be replayed cost");
		heurDistanceGroup.add(yesHeurDistance);
		heurDistanceGroup.add(noHeurDistance);
		yesHeurDistance.setSelected(true);

		add(slickerFactory.createLabel("<html><h2># Events to be replayed cost</h2></html>"), "0, "
				+ String.valueOf(rowIndex++));
		add(noHeurDistance, "0, " + String.valueOf(rowIndex++));
		add(yesHeurDistance, "0, " + String.valueOf(rowIndex));
		add(heurDistanceSlider, "1, " + String.valueOf(rowIndex++));

		// replayed events 
		replayedEvtSlider = slickerFactory.createNiceIntegerSlider("", 0, 3000, 1000, Orientation.HORIZONTAL);
		replayedEvtSlider.setPreferredSize(new Dimension(220, 20));
		replayedEvtGroup = new ButtonGroup();
		yesReplayedEvtCost = slickerFactory.createRadioButton("Use replayed events cost");
		noReplayedEvtCost = slickerFactory.createRadioButton("Don't use replayed events cost");
		replayedEvtGroup.add(yesReplayedEvtCost);
		replayedEvtGroup.add(noReplayedEvtCost);
		yesReplayedEvtCost.setSelected(true);

		add(slickerFactory.createLabel("<html><h2># Replayed events cost</h2></html>"), "0, "
				+ String.valueOf(rowIndex++));
		add(noReplayedEvtCost, "0, " + String.valueOf(rowIndex++));
		add(yesReplayedEvtCost, "0, " + String.valueOf(rowIndex));
		add(replayedEvtSlider, "1, " + String.valueOf(rowIndex++));

		// not accumulated unhandled arcs cost
		unhandledArcsSlider = slickerFactory.createNiceIntegerSlider("", 0, 1000, 1, Orientation.HORIZONTAL);
		unhandledArcsSlider.setPreferredSize(new Dimension(220, 20));
		unhandledArcsGroup = new ButtonGroup();
		yesUnhandledArcs = slickerFactory.createRadioButton("Use unhandled arcs cost (not accumulated)");
		noUnhandledArcs = slickerFactory.createRadioButton("Don't use unhandled arcs cost (not accumulated)");
		unhandledArcsGroup.add(yesUnhandledArcs);
		unhandledArcsGroup.add(noUnhandledArcs);
		yesUnhandledArcs.setSelected(true);
		
		add(slickerFactory.createLabel("<html><h2># Unhandled arcs cost</h2>"), "0, " + String.valueOf(rowIndex++));
		add(noUnhandledArcs, "0, " + String.valueOf(rowIndex++));
		add(yesUnhandledArcs, "0, " + String.valueOf(rowIndex));
		add(unhandledArcsSlider, "1, " + String.valueOf(rowIndex++));
		
		// skipping event cost
		skipEventSlider = slickerFactory.createNiceIntegerSlider("", 0, 3000, 3000, Orientation.HORIZONTAL);
		skipEventSlider.setPreferredSize(new Dimension(220, 20));
		skipEventGroup = new ButtonGroup();
		yesSkipEvent = slickerFactory.createRadioButton("Penalize move on log only");
		noSkipEvent = slickerFactory.createRadioButton("Don't penalize move on log only");
		skipEventGroup.add(yesSkipEvent);
		skipEventGroup.add(noSkipEvent);
		yesSkipEvent.setSelected(true);
		
		add(slickerFactory.createLabel("<html><h2>Move on log only cost</h2>"), "0, " + String.valueOf(rowIndex++));
		add(noSkipEvent, "0, " + String.valueOf(rowIndex++));
		add(yesSkipEvent, "0, " + String.valueOf(rowIndex));
		add(skipEventSlider, "1, " + String.valueOf(rowIndex++));
		
		// initiated execute invi task
		execInviTaskSlider = slickerFactory.createNiceIntegerSlider("", 0, 3000, 1, Orientation.HORIZONTAL);
		execInviTaskSlider.setPreferredSize(new Dimension(220, 20));
		execInviTaskEventsGroup = new ButtonGroup();
		yesExecInviTask = slickerFactory.createRadioButton("Penalize move on model only (invisible task)");
		noExecInviTask = slickerFactory.createRadioButton("Don't penalize move on model only (invisible task)");
		execInviTaskEventsGroup.add(yesExecInviTask);
		execInviTaskEventsGroup.add(noExecInviTask);
		yesExecInviTask.setSelected(true);
		
		add(slickerFactory.createLabel("<html><h2>Move on model only cost (invisible task)</h2>"), "0, " + String.valueOf(rowIndex++));
		add(noExecInviTask, "0, " + String.valueOf(rowIndex++));
		add(yesExecInviTask, "0, " + String.valueOf(rowIndex));
		add(execInviTaskSlider, "1, " + String.valueOf(rowIndex++));
		
		// initiated execute real task
		execRealTaskSlider = slickerFactory.createNiceIntegerSlider("", 0, 3000, 1000, Orientation.HORIZONTAL);
		execRealTaskSlider.setPreferredSize(new Dimension(220, 20));
		execRealTaskEventsGroup = new ButtonGroup();
		yesExecRealTask = slickerFactory.createRadioButton("Penalize move on model only (real task)");
		noExecRealTask = slickerFactory.createRadioButton("Don't penalize move on model only (real task)");
		execRealTaskEventsGroup.add(yesExecRealTask);
		execRealTaskEventsGroup.add(noExecRealTask);
		yesExecRealTask.setSelected(true);
		
		add(slickerFactory.createLabel("<html><h2>Move on model only cost (real task)</h2>"), "0, " + String.valueOf(rowIndex++));
		add(noExecRealTask, "0, " + String.valueOf(rowIndex++));
		add(yesExecRealTask, "0, " + String.valueOf(rowIndex));
		add(execRealTaskSlider, "1, " + String.valueOf(rowIndex++));

		// accumulated unhandled arcs
		accUnhandledArcsSlider = slickerFactory.createNiceIntegerSlider("", 0, 1000, 0, Orientation.HORIZONTAL);
		accUnhandledArcsSlider.setPreferredSize(new Dimension(220, 20));
		notAccUnhandledArcsGroup = new ButtonGroup();
		yesAccUnhandledArcs = slickerFactory.createRadioButton("Use accumulated unhandled arcs cost");
		noAccUnhandledArcs = slickerFactory.createRadioButton("Don't use accumulated unhandled arcs cost");
		notAccUnhandledArcsGroup.add(yesAccUnhandledArcs);
		notAccUnhandledArcsGroup.add(noAccUnhandledArcs);
		noAccUnhandledArcs.setSelected(true);
		
		add(slickerFactory.createLabel("<html><h2>Accumulated unhandled arcs cost</h2>"), "0, " + String.valueOf(rowIndex++));
		add(noAccUnhandledArcs, "0, " + String.valueOf(rowIndex++));
		add(yesAccUnhandledArcs, "0, " + String.valueOf(rowIndex));
		add(accUnhandledArcsSlider, "1, " + String.valueOf(rowIndex++));

		if (isTestingMode) {
			this.isTestingMode = true;
			
			// selection of testing mode (event removal or prefix)
			testTypeGroup = new ButtonGroup();
			prefixType = slickerFactory.createRadioButton("Prefix testing (small prefixes to whole trace)");
			invertedPrefixType = slickerFactory.createRadioButton("Inverted prefix testing (whole trace to small prefixes)");
			eventRemovalType = slickerFactory
					.createRadioButton("Event removal testing");
			testTypeGroup.add(prefixType);
			testTypeGroup.add(invertedPrefixType);
			testTypeGroup.add(eventRemovalType);
			prefixType.setSelected(true);

			add(slickerFactory
					.createLabel("<html><h2>Select testing type</h2></html>"),
					"0, " + String.valueOf(rowIndex++));
			add(eventRemovalType, "0, " + String.valueOf(rowIndex++));
			add(prefixType, "0, " + String.valueOf(rowIndex++));
			add(invertedPrefixType, "0, " + String.valueOf(rowIndex++));

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
						//TODO: check if file is empty, set default file name and location
						fl.setText(fc.getSelectedFile().getAbsolutePath()
								+ ".csv");
					}
				}
			});
			JPanel inPanel = slickerFactory.createGradientPanel();
			inPanel.add(fl);
			inPanel.add(fs);

			add(slickerFactory
					.createLabel("<html><h2>Output file location</h2></html>"),
					"0, " + String.valueOf(rowIndex++));
			add(inPanel, "0, " + String.valueOf(rowIndex++));
		} else {
			this.isTestingMode = false;
		}
	}

	@Override
	public Object[] getAllParameters() {
		Object[] parameters = new Object[13];
		
		parameters[ParamSettingCostBasedReplay.UNSATISFIEDEVENTSCOST] = yesUnsatisfiedEvents.isSelected() ? unsatisfiedEventsSlider.getValue() : 0;	// event that executed without proper predecessors
		parameters[ParamSettingCostBasedReplay.HEURISTICDISTANCECOST] = yesHeurDistance.isSelected() ? heurDistanceSlider.getValue() : 0; // number of events still left to be replayed
		parameters[ParamSettingCostBasedReplay.SKIPPEDEVENTCOST] = yesSkipEvent.isSelected() ? skipEventSlider.getValue() : 0;  // number of events that are ignored in replay
		parameters[ParamSettingCostBasedReplay.REPLAYEDEVENTCOST] = yesReplayedEvtCost.isSelected() ? replayedEvtSlider.getValue() : 0;  // number of replayed events
		parameters[ParamSettingCostBasedReplay.INITIATIVEINVISTASKCOST] = yesExecInviTask.isSelected() ? execInviTaskSlider.getValue() : 0; // number of invisible tasks that are executed without any occurrence of its corresponding events.
		parameters[ParamSettingCostBasedReplay.INITIATIVEREALTASKCOST] = yesExecRealTask.isSelected() ? execRealTaskSlider.getValue() : 0; // number of tasks that are executed without any occurrence of its corresponding events.
		parameters[ParamSettingCostBasedReplay.ACCUMULATEDUNHANDLEDARCSCOST] = yesAccUnhandledArcs.isSelected() ? accUnhandledArcsSlider.getValue() : 0;  // accumulation of number of unhandled events
		parameters[ParamSettingCostBasedReplay.UNACCUMULATEDUNHANDLEDARCSCOST] = yesUnhandledArcs.isSelected() ? unhandledArcsSlider.getValue() : 0;  // number of unhandled events
				
		parameters[ParamSettingCostBasedReplay.MAXEXPLOREDINSTANCESINTVAL] = getMaxExpInst();
		parameters[ParamSettingCostBasedReplay.MAXPOSSIBLESTATESINTVAL] = getSkipLimEstim();
		parameters[ParamSettingCostBasedReplay.ISTESTINGMODEINTVAL] = this.isTestingMode;

		parameters[ParamSettingCostBasedReplay.FILELOCATIONSTRVAL] = getFileLocation();
		parameters[ParamSettingCostBasedReplay.ANALYSISTYPEINTVAL] = getAnalysisType();
		return parameters;
	}

	private int getSkipLimEstim() {
		if (yesSkipLimEstim.isSelected()) {
			return skipLimEstimSlider.getValue();
		} else {
			return -1;
		}
	}
	
	private int getAnalysisType() {
		if (!isTestingMode){
			return -1;
		} else {
			if (eventRemovalType.isSelected()) {
				return IFlexLogReplayAlgorithm.REMOVEEVENTANALYSIS;
			} else if (prefixType.isSelected()) {
				return IFlexLogReplayAlgorithm.PREFIXANALYSIS;
			} else {
				return IFlexLogReplayAlgorithm.INVERTPREFIXANALYSIS;
			}
		}
	}
	
	private int getMaxExpInst() {
		if (yesMaxExpInst.isSelected()) {
			return maxExpInstSlider.getValue();
		} else {
			return Integer.MAX_VALUE;
		}
	}

	private String getFileLocation() {
		if (isTestingMode){
			return fl.getText().trim();
		}
		return "";
	}
	
	@Override
	public Object getParameterValue(int paramVariableValIndex) {
		if (paramVariableValIndex == ParamSettingAStarReplay.MAXEXPLOREDINSTANCESINTVAL){
			return getMaxExpInst();
		} else 
			if (paramVariableValIndex == ParamSettingAStarReplay.FILELOCATIONSTRVAL){
				return getFileLocation();
			} else
				if (paramVariableValIndex == ParamSettingAStarReplay.ISTESTINGMODEINTVAL){
					return this.isTestingMode;
				} else { 
					return getAnalysisType();
				}

	}
}
