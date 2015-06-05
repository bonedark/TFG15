/**
 * 
 */
package org.processmining.plugins.replayer.ui;

import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XLog;
import org.processmining.plugins.flex.replayer.algorithms.IFlexLogReplayAlgorithm;
import org.processmining.plugins.flex.replayer.ui.ParamSettingStep;

/**
 * @author aadrians
 *
 */
public class CaseSelectorUI extends ParamSettingStep {
	private static final long serialVersionUID = -6501920213599223872L;
	
	// name of variable
	public static int XTRACE = 0; // trace of the log
	
	// component
	private LogPreviewSelect logPreviewSelect;

	public CaseSelectorUI(boolean isTestingMode){
		super(isTestingMode);
	}

	public void initComponents(XLog log) {
		XLogInfo xLogInfo = XLogInfoFactory.createLogInfo(log);
		logPreviewSelect = new LogPreviewSelect(log, xLogInfo);
		add(logPreviewSelect);
	}

	@Override
	public boolean precondition() { return true; }

	@Override
	public void readSettings(IFlexLogReplayAlgorithm algorithm) { }

	@Override
	public Object[] getAllParameters() {
		// get selected case
		Object[] result = new Object[1];
		result[0] = logPreviewSelect.getSelectedTrace();
		return result;
	}

	@Override
	public Object getParameterValue(int paramVariableValIndex) {
		if (paramVariableValIndex == CaseSelectorUI.XTRACE){
			return logPreviewSelect.getSelectedTrace();
		} else {
			return null;
		}
	}
	
}
