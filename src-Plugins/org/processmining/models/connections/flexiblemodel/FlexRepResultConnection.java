/**
 * 
 */
package org.processmining.models.connections.flexiblemodel;

import org.deckfour.xes.model.XLog;
import org.processmining.framework.connections.impl.AbstractConnection;
import org.processmining.models.flexiblemodel.EndTaskNodesSet;
import org.processmining.models.flexiblemodel.Flex;
import org.processmining.models.flexiblemodel.StartTaskNodesSet;
import org.processmining.plugins.flex.replayresult.FlexRepResult;

/**
 * @author aadrians
 * 
 */
public class FlexRepResultConnection extends AbstractConnection {
	public final static String FLEX = "Flex";
	public final static String STARTTASKNODESET = "StartTaskNodesSet";
	public final static String LOG = "Log";
	public final static String FLEXREPRESULT = "FlexReplayResult";
	public final static String ENDTASKNODESET = "EndTaskNodesSet";

	public FlexRepResultConnection(String label, Flex flex, StartTaskNodesSet startTaskNodesSet,
			EndTaskNodesSet endTaskNodesSet, XLog log, FlexRepResult repResult) {
		super(label);
		put(FLEX, flex);
		put(STARTTASKNODESET, startTaskNodesSet);
		put(ENDTASKNODESET, endTaskNodesSet);
		put(LOG, log);
		put(FLEXREPRESULT, repResult);
	}
}
