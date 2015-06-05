/**
 * 
 */
package org.processmining.models.connections.flexiblemodel.conversion;

import org.deckfour.xes.model.XLog;
import org.processmining.framework.connections.impl.AbstractConnection;
import org.processmining.models.flexiblemodel.Flex;
import org.processmining.plugins.flex.replayer.performance.util.FlexSpecialNodes;
import org.processmining.plugins.flex.replayer.performance.util.FlexToFlexMapping;
import org.processmining.plugins.flex.replayer.performance.util.OriginalFlexToILifecycleMap;

/**
 * @author aadrians
 * 
 */
public class ReplayFlexOfOrigFlexConnection extends AbstractConnection {

	public final static String LOG = "Log";
	public final static String SPECIALNODES = "SpecialNodes";
	public final static String MAPPINGTOORIGINAL = "Mapping";
	public final static String MAPPINGORIGINALNODETOLIFECYCLETYPE = "MappingOriginalNodeToLifecycleType";
	public final static String ORIGINALMODEL = "OriginalModel";
	public final static String REPLAYMODEL = "ReplayModel";

	public ReplayFlexOfOrigFlexConnection(String label, Flex originalModel, FlexSpecialNodes specialNodes, XLog log, 
			FlexToFlexMapping mapping, OriginalFlexToILifecycleMap originalFlexToILifecycleMap, Flex replayModel) {
		super(label);
		put(LOG, log);
		put(SPECIALNODES, specialNodes);
		put(MAPPINGTOORIGINAL, mapping);
		put(MAPPINGORIGINALNODETOLIFECYCLETYPE, originalFlexToILifecycleMap);
		put(ORIGINALMODEL, originalModel);
		put(REPLAYMODEL, replayModel);
	}
}