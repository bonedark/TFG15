/**
 * 
 */
package org.processmining.plugins.replayer.tester;

import java.util.HashMap;
import java.util.Map;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.connections.flexiblemodel.FlexEndTaskNodeConnection;
import org.processmining.models.connections.flexiblemodel.FlexPerfRepInfoConnection;
import org.processmining.models.connections.flexiblemodel.FlexStartTaskNodeConnection;
import org.processmining.models.flexiblemodel.EndTaskNodesSet;
import org.processmining.models.flexiblemodel.Flex;
import org.processmining.models.flexiblemodel.FlexNode;
import org.processmining.models.flexiblemodel.StartTaskNodesSet;
import org.processmining.plugins.flex.replayer.algorithms.ExtendedCostBasedAStarLogReplayAlgorithm;
import org.processmining.plugins.flex.replayresult.performance.FlexPerfRepInfo;

/**
 * @author aadrians
 * 
 */
@Plugin(name = "Test non-GUI performance analysis", returnLabels = { "Flexible model", "Start task node", "End task node" }, returnTypes = {
		Flex.class, StartTaskNodesSet.class, EndTaskNodesSet.class }, parameterLabels = { "Flexible model", "Log" }, help = "testing for performance analysis", userAccessible = true, mostSignificantResult = 1)
public class FlexReplayerCommandLine {

	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "Arya Adriansyah", email = "a.adriansyah@tue.nl", pack = "Replayer")
	@PluginVariant(variantLabel = "From Flexible model and Log", requiredParameterLabels = { 0, 1 })
	public Object[] testing(final PluginContext context, Flex cnet, XLog log) throws Exception {
		// mapping between event class and model 
		// assuming that an event class is mapped to a node with the same name
		XEventClassifier stdClassifier = XLogInfoImpl.NAME_CLASSIFIER;
		XLogInfo summaryName = XLogInfoFactory.createLogInfo(log, stdClassifier);
		XEventClasses nameEventClasses = summaryName.getEventClasses();

		Map<FlexNode, XEventClass> mappings = new HashMap<FlexNode, XEventClass>();
		for (XEventClass evClass : nameEventClasses.getClasses()) {
			for (FlexNode node : cnet.getNodes()) {
				if (node.getLabel().equals(evClass.toString())) {
					mappings.put(node, evClass);
				}
			}
		}

		// setting algorithm parameters
		Object[] algParameters = new Object[15];
		algParameters[ExtendedCostBasedAStarLogReplayAlgorithm.ISTESTINGMODEINTVAL] = 0;
		algParameters[ExtendedCostBasedAStarLogReplayAlgorithm.MAXEXPLOREDINSTANCESINTVAL] = 20000;
		algParameters[ExtendedCostBasedAStarLogReplayAlgorithm.INAPPROPRIATETRANSFIRECOST] = 0;
		algParameters[ExtendedCostBasedAStarLogReplayAlgorithm.HEURISTICDISTANCECOST] = 1;
		algParameters[ExtendedCostBasedAStarLogReplayAlgorithm.SKIPPEDEVENTCOST] = 5;
		algParameters[ExtendedCostBasedAStarLogReplayAlgorithm.REPLAYEDEVENTCOST] = 1;
		algParameters[ExtendedCostBasedAStarLogReplayAlgorithm.INITIATIVEINVISTASKCOST] = 0;
		algParameters[ExtendedCostBasedAStarLogReplayAlgorithm.INITIATIVEREALTASKCOST] = 2;
		algParameters[ExtendedCostBasedAStarLogReplayAlgorithm.FILELOCATIONSTRVAL] = "";
		algParameters[ExtendedCostBasedAStarLogReplayAlgorithm.ANALYSISTYPEINTVAL] = 0; // analysis type

		// permittable replay actions
		algParameters[ExtendedCostBasedAStarLogReplayAlgorithm.ALLOWINVITASK] = true;
		algParameters[ExtendedCostBasedAStarLogReplayAlgorithm.ALLOWREALTASK] = true;
		algParameters[ExtendedCostBasedAStarLogReplayAlgorithm.ALLOWEVENTSKIP] = true;
		algParameters[ExtendedCostBasedAStarLogReplayAlgorithm.ALLOWTASKEXECWOTOKENS] = false;
		algParameters[ExtendedCostBasedAStarLogReplayAlgorithm.ALLOWINAPPROPRIATEFIRING] = false;

		// extract start and end task node
		StartTaskNodesSet startTaskNode = null;
		try {
			FlexStartTaskNodeConnection connStartTask = context.getConnectionManager().getFirstConnection(
					FlexStartTaskNodeConnection.class, context, cnet);
			startTaskNode = connStartTask.getObjectWithRole(FlexStartTaskNodeConnection.STARTTASKNODES);
		} catch (ConnectionCannotBeObtained exc) {
			System.out.println("no start task node");
			startTaskNode = new StartTaskNodesSet();
		}
		EndTaskNodesSet endTaskNode = null;
		try {
			FlexEndTaskNodeConnection connStartTask = context.getConnectionManager().getFirstConnection(
					FlexEndTaskNodeConnection.class, context, cnet);
			endTaskNode = connStartTask.getObjectWithRole(FlexEndTaskNodeConnection.ENDTASKNODES);
		} catch (ConnectionCannotBeObtained exc) {
			System.out.println("no end task node");
			endTaskNode = new EndTaskNodesSet();
		}

		// call performance analysis
		try {
			context.tryToFindOrConstructFirstObject(FlexPerfRepInfo.class, FlexPerfRepInfoConnection.class,
					FlexPerfRepInfoConnection.FLEXPERFREPINFO, cnet, log, mappings, startTaskNode, endTaskNode,
					new ExtendedCostBasedAStarLogReplayAlgorithm(), algParameters);
		} catch (ConnectionCannotBeObtained conExc) {
			conExc.printStackTrace();
		}
		return new Object[] { cnet, startTaskNode, endTaskNode };
	}
}
