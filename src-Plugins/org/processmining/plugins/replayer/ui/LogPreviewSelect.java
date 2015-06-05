/**
 * 
 */
package org.processmining.plugins.replayer.ui;

import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.plugins.log.ui.logdialog.LogPreviewUI;

/**
 * @author aadrians
 *
 */
public class LogPreviewSelect extends LogPreviewUI {
	private static final long serialVersionUID = -1588104595532818626L;

	public LogPreviewSelect(XLog log, XLogInfo summary) {
		super(log, summary);
	}

	public XTrace getSelectedTrace(){
		int[] selectedIndices = instancesList.getSelectedIndices();
		if (selectedIndices.length > 0){
			return log.get(selectedIndices[0]);
		} else {
			return log.get(0);
		}
	}
}
