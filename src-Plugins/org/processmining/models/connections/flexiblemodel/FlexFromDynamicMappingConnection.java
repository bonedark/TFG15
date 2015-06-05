/**
 * 
 */
package org.processmining.models.connections.flexiblemodel;

import java.util.Map;
import java.util.Set;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.model.XLog;
import org.processmining.framework.connections.impl.AbstractConnection;
import org.processmining.models.flexiblemodel.Flex;
import org.processmining.models.flexiblemodel.FlexNode;

/**
 * @author aadrians
 *
 */
public class FlexFromDynamicMappingConnection extends AbstractConnection {
	public final static String LOG = "Log";
	public final static String FLEX = "Flex";
	public final static String EVENTCLASSES= "EventClasses";
	public final static String MULTIPLEMAPPING = "MultipleMapping";

	public FlexFromDynamicMappingConnection(String label, XLog log, Flex flex, XEventClasses eventClasses, Map<FlexNode, Set<XEventClass>> mapping) {
		super(label);
		put(LOG, log);
		put(FLEX, flex);
		put(EVENTCLASSES, eventClasses);
		put(MULTIPLEMAPPING, mapping);
	}
}
