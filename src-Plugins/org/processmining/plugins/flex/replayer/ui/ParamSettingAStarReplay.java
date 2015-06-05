/**
 * 
 */
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
 * @author aadrians
 *
 */
public class ParamSettingAStarReplay extends ParamSettingStep {
	// identifier
	private static final long serialVersionUID = -8861086035928110042L;
	
	// name of variable
	public static int ISTESTINGMODEINTVAL = 0; // 0 if replay mode, 1 if testing mode
	public static int MAXPOSSIBLESTATESINTVAL = 1; // boundary of maximum possible states if replay is going to be performed
	public static int MAXEXPLOREDINSTANCESINTVAL = 2; // maximum possible instance explored before exploration stops
	public static int FILELOCATIONSTRVAL = 3; //location of file to store testing result
	public static int ANALYSISTYPEINTVAL = 4; // analysis type (REMOVEEVENTANALYSIS/PREFIXANALYSIS/INVERTPREFIXANALYSIS)
	
	// testing mode/replay mode
	private boolean isTestingMode = false;
		
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

	
	public ParamSettingAStarReplay(boolean isTestingMode) {
		super(isTestingMode);		
		initComponents(isTestingMode);
	}

	private void initComponents(boolean isTestingMode) {
		// init instance
		SlickerFactory slickerFactory = SlickerFactory.instance();
		int rowIndex = 1;

		double size[][] = {
				{ TableLayoutConstants.FILL, TableLayoutConstants.FILL },
				{ 50, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30 } };
		setLayout(new TableLayout(size));
		add(slickerFactory.createLabel("<html><h1>Replay parameter</h1>"),
				"0, 0, l, t");

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

		add(
				slickerFactory
						.createLabel("<html><h2>Skip if sequence has X possible instances?</h2></html>"),
				"0, " + String.valueOf(rowIndex++));
		add(noSkipLimEstim, "0, " + String.valueOf(rowIndex++));
		add(yesSkipLimEstim, "0, " + String.valueOf(rowIndex));
		add(skipLimEstimSlider, "1, " + String.valueOf(rowIndex++));

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
	public boolean precondition() { return true;	}

	@Override
	public void readSettings(IFlexLogReplayAlgorithm algorithm) {}

	public int getAnalysisType() {
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

	/**
	 * Get boundary of maximum possible states if replay is going to be
	 * performed
	 * 
	 * @return -1 if unlimited
	 */
	private int getSkipLimEstim() {
		if (yesSkipLimEstim.isSelected()) {
			return skipLimEstimSlider.getValue();
		} else {
			return -1;
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
			if (paramVariableValIndex == ParamSettingAStarReplay.MAXPOSSIBLESTATESINTVAL){
				return getSkipLimEstim();
			} else 
				if (paramVariableValIndex == ParamSettingAStarReplay.FILELOCATIONSTRVAL){
					return getFileLocation();
				} else
					if (paramVariableValIndex == ParamSettingAStarReplay.ISTESTINGMODEINTVAL){
						return this.isTestingMode;
					} else 
						return getAnalysisType();

	}

	@Override
	public Object[] getAllParameters() {
		Object[] parameters = new Object[5];
		parameters[ParamSettingAStarReplay.MAXEXPLOREDINSTANCESINTVAL] = getMaxExpInst();
		parameters[ParamSettingAStarReplay.MAXPOSSIBLESTATESINTVAL] = getSkipLimEstim();
		parameters[ParamSettingAStarReplay.FILELOCATIONSTRVAL] = getFileLocation();
		parameters[ParamSettingAStarReplay.ISTESTINGMODEINTVAL] = this.isTestingMode;
		parameters[ParamSettingAStarReplay.ANALYSISTYPEINTVAL] = getAnalysisType();
		return parameters;
	}
}
