/**
 * 
 */
package org.processmining.plugins.flex.replayer;

import java.util.Collection;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.framework.util.Pair;
import org.processmining.models.connections.flexiblemodel.FlexStartTaskNodeConnection;
import org.processmining.models.flexiblemodel.Flex;
import org.processmining.models.flexiblemodel.FlexNode;
import org.processmining.models.flexiblemodel.SetFlex;
import org.processmining.models.flexiblemodel.StartTaskNodesSet;
import org.processmining.plugins.flex.replayer.algorithms.IFlexLogReplayAlgorithm;
import org.processmining.plugins.flex.replayer.ui.FlexReplayerUI;
import org.processmining.plugins.flex.replayer.ui.ReplayStep;
import org.processmining.plugins.replayer.replayresult.CaseReplayResult;
import org.processmining.plugins.replayer.ui.CaseSelectorUI;

/**
 * @author aadrians
 * 
 */
@Plugin(name = "Replay a Case on Flexible model for Conformance Analysis", returnLabels = { "Case Replay Result", "Start Task Nodes" }, returnTypes = { CaseReplayResult.class , StartTaskNodesSet.class }, parameterLabels = {
		"Flexible model", "Start Task Nodes Set", "Event Log" }, help = "Replay a case in a log on Flexible model to check conformance and state space expansion.", userAccessible = true)
public class FlexCaseReplayer {
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "Arya Adriansyah", email = "a.adriansyah@tue.nl", pack = "Replayer")
	@PluginVariant(variantLabel = "From Flexible model and Log", requiredParameterLabels = { 0, 2 })
	public Object[] replayCase(final UIPluginContext context, Flex flex, XLog log) throws Exception {
		// obtain Start Task Nodes
		StartTaskNodesSet startTaskNodeSet = null;
		try {
			FlexStartTaskNodeConnection conn = context.getConnectionManager().getFirstConnection(
					FlexStartTaskNodeConnection.class, context, flex);

			startTaskNodeSet = conn.getObjectWithRole(FlexStartTaskNodeConnection.STARTTASKNODES);
			return replayCase(context, flex, startTaskNodeSet, log);
		} catch (Exception exc) {
			context.log("No Starting Task Node is found, try to construct empty one");
			startTaskNodeSet = new StartTaskNodesSet();
			startTaskNodeSet.add(new SetFlex());
			context.addConnection(new FlexStartTaskNodeConnection("Connection between " + flex.getLabel()
					+ " and its start task nodes", flex, startTaskNodeSet));
			context.log("Starting Task Node is successfully constructed");
			return replayCase(context, flex, startTaskNodeSet, log);
		}
	}

	@SuppressWarnings("unchecked")
	@PluginVariant(variantLabel = "From Flexible model, Log, and StartTaskNode", requiredParameterLabels = { 0, 1, 2 })
	public Object[] replayCase(final UIPluginContext context, Flex flex, StartTaskNodesSet startTaskNodesSet,
			XLog log) throws Exception {
		// get configuration values from user
		// checking for connection is performed inside FlexReplayerGUI class
		FlexReplayerUI flexReplayerUI = new FlexReplayerUI(context);

		CaseSelectorUI caseSelectorUI = new CaseSelectorUI(false);
		caseSelectorUI.initComponents(log);

		ReplayStep[] addition = new ReplayStep[1];
		addition[0] = caseSelectorUI;

		Object[] resultConfiguration = flexReplayerUI.getExtendedConfiguration(flex, log, false, addition);
		if (resultConfiguration == null){
			context.getFutureResult(0).cancel(true);
			context.getFutureResult(1).cancel(true);
			return null;
		}
		
		// if all parameters are set, show additional case selection screen
		if (resultConfiguration[FlexReplayerUI.MAPPING] != null) {
			// get number of variable in the first
			int[] paramNumber = flexReplayerUI.getParamNum();

			// get all parameters
			Object[] allParameters = (Object[]) resultConfiguration[FlexReplayerUI.PARAMETERS];

			XTrace trace = (XTrace) allParameters[paramNumber[0] + CaseSelectorUI.XTRACE];
			IFlexLogReplayAlgorithm selectedAlg = (IFlexLogReplayAlgorithm) resultConfiguration[FlexReplayerUI.ALGORITHM];

			// get classes
			XLogInfo summary = XLogInfoFactory.createLogInfo(log);
			XEventClasses classes = summary.getEventClasses();

			CaseReplayResult res = selectedAlg.replayXTrace(context, flex, startTaskNodesSet, trace, summary, classes,
					(Collection<Pair<FlexNode, XEventClass>>) resultConfiguration[FlexReplayerUI.MAPPING],
					(Object[]) resultConfiguration[FlexReplayerUI.PARAMETERS]);
			context.getFutureResult(0).setLabel(
					"Result of replaying case " + XConceptExtension.instance().extractName(trace) + " on "
							+ flex.getLabel() + " using " + selectedAlg.toString());
			return new Object[] {res, startTaskNodesSet};

		} else {
			context.log("replay is not performed because not enough parameter is submitted");
			context.getFutureResult(0).cancel(true);
			return null;
		}
	}
}
