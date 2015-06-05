package org.processmining.models.connections.flexiblemodel;

import java.util.Collection;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.model.XLog;
import org.processmining.framework.connections.annotations.ConnectionObjectFactory;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.framework.util.Pair;
import org.processmining.models.connections.AbstractLogModelConnection;
import org.processmining.models.flexiblemodel.Flex;
import org.processmining.models.flexiblemodel.FlexEdge;
import org.processmining.models.flexiblemodel.FlexNode;

/**
 * Connection between flexible mode and log
 * 
 * @author arya
 * @email arya.adriansyah@gmail.com
 * @version Nov 19, 2009
 */
@Plugin(name = "Log Flexible Model Connection Factory", parameterLabels = { "Log", "Event Classes", "Flexible model",
		"Relations" }, returnTypes = FlexLogConnection.class, returnLabels = "Log flexible model connection", userAccessible = false)
@ConnectionObjectFactory
public class FlexLogConnection extends
		AbstractLogModelConnection<FlexNode, FlexEdge<? extends FlexNode, ? extends FlexNode>> {
	/**
	 * Default constructor
	 * 
	 * @param log
	 * @param classes
	 * @param graph
	 * @param relations
	 */
	public FlexLogConnection(XLog log, XEventClasses classes, Flex graph,
			Collection<Pair<FlexNode, XEventClass>> relations) {
		super(log, classes, graph, graph.getNodes(), relations);
	}

	/**
	 * Static factory to produce LogFlexConnection
	 * 
	 * @param context
	 * @param log
	 * @param classes
	 * @param graph
	 * @param relations
	 * @return
	 */
	@PluginVariant(requiredParameterLabels = { 0, 1, 2, 3 })
	public static FlexLogConnection logFlexConnectionFactory(PluginContext context, XLog log, XEventClasses classes,
			Flex graph, Collection<Pair<FlexNode, XEventClass>> relations) {
		FlexLogConnection logFlexConnection = new FlexLogConnection(log, classes, graph, relations);
		return logFlexConnection;
	}
}
