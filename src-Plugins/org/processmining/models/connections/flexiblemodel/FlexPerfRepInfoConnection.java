/**
 * 
 */
package org.processmining.models.connections.flexiblemodel;

import org.deckfour.xes.model.XLog;
import org.processmining.framework.connections.impl.AbstractConnection;
import org.processmining.models.flexiblemodel.Flex;
import org.processmining.models.flexiblemodel.StartTaskNodesSet;
import org.processmining.plugins.flex.replayresult.performance.FlexPerfRepInfo;

/**
 * @author aadrians
 * 
 */
public class FlexPerfRepInfoConnection extends AbstractConnection {
	public final static String FLEX = "Flex";
	public final static String STARTTASKNODESET = "StartTaskNodesSet";
	public final static String LOG = "Log";
	public final static String FLEXPERFREPINFO = "FlexPerformanceReplayInfo";

	public FlexPerfRepInfoConnection(String label, Flex originalModel, StartTaskNodesSet startTaskNodesSet, XLog log,
			FlexPerfRepInfo flexPerformanceReplayInfo) {
		super(label);
		put(FLEX, originalModel);
		put(STARTTASKNODESET, startTaskNodesSet);
		put(LOG, log);
		put(FLEXPERFREPINFO, flexPerformanceReplayInfo);
	}
}