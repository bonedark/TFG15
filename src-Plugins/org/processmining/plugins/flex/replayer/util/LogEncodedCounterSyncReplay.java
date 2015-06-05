/**
 * 
 */
package org.processmining.plugins.flex.replayer.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.processmining.plugins.replayer.replayresult.SyncReplayResult;
import org.processmining.plugins.replayer.util.StepTypes;

/**
 * @author aadrians
 *
 */
public class LogEncodedCounterSyncReplay {
	private Map<List<Short>, SyncReplayResult> inputFreq; 

	public LogEncodedCounterSyncReplay() {
		inputFreq = new HashMap<List<Short>, SyncReplayResult>();
	}

	public synchronized void createKey(List<Short> listEncodedEvtClass) {
		inputFreq.put(listEncodedEvtClass, null);
	}

	public boolean containsKey(List<Short> listEncodedEvtClass) {
		return inputFreq.containsKey(listEncodedEvtClass);
	}

	public synchronized boolean contains(List<Short> eventEncodedClassLst) {
		return inputFreq.containsKey(eventEncodedClassLst);
	}

	public synchronized void inc(List<Short> eventEncodedClassLst, int traceIndex) {
		inputFreq.get(eventEncodedClassLst).addNewCase(traceIndex);
	}

	public synchronized void add(List<Short> listEncodedEvtClass, List<Object> nodeInstanceObj, 
			List<StepTypes> stepTypesLst, int traceIndex, boolean isReliable){
		SyncReplayResult newRes = new SyncReplayResult(nodeInstanceObj, stepTypesLst, traceIndex);
		inputFreq.put(listEncodedEvtClass, newRes);
		newRes.setReliable(isReliable);
	}

	public synchronized Collection<SyncReplayResult> getResult() {
		return inputFreq.values();
	}
}
