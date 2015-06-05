/**
 * 
 */
package org.processmining.plugins.flex.replayer.performance.util;

import java.util.HashMap;

import org.processmining.framework.util.Pair;
import org.processmining.models.flexiblemodel.FlexNode;
import org.processmining.plugins.replayer.util.LifecycleTypes;

/**
 * Mapping from a binding in flex (input nodes, node, and output nodes), to a pair of (encoded node, lifecycle that is represented by the node)
 * @author aadrians
 *
 */
public class FlexToFlexMapping extends HashMap<FlexNode, Pair<FlexNode, LifecycleTypes>> {
	private static final long serialVersionUID = 2777826749347236598L;
}
