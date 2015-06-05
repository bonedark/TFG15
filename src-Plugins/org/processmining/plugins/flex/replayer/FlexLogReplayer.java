package org.processmining.plugins.flex.replayer;

import java.util.Collection;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.Progress;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.framework.plugin.events.Logger.MessageLevel;
import org.processmining.framework.util.Pair;
import org.processmining.models.connections.flexiblemodel.FlexCodecConnection;
import org.processmining.models.connections.flexiblemodel.FlexEndTaskNodeConnection;
import org.processmining.models.connections.flexiblemodel.FlexRepResultConnection;
import org.processmining.models.connections.flexiblemodel.FlexStartTaskNodeConnection;
import org.processmining.models.flexiblemodel.EndTaskNodesSet;
import org.processmining.models.flexiblemodel.Flex;
import org.processmining.models.flexiblemodel.FlexNode;
import org.processmining.models.flexiblemodel.SetFlex;
import org.processmining.models.flexiblemodel.StartTaskNodesSet;
import org.processmining.plugins.flex.replayer.algorithms.IFlexLogReplayAlgorithm;
import org.processmining.plugins.flex.replayer.ui.FlexReplayerUI;
import org.processmining.plugins.flex.replayer.util.FlexCodec;
import org.processmining.plugins.flex.replayresult.FlexRepResult;

/**
 * @author arya
 * @email arya.adriansyah@gmail.com
 * @version Nov 20, 2009
 */
@Plugin(name = "Replay a Log on Flexible model for Conformance Analysis", returnLabels = { "Conformance analysis result",
		"Start Task Nodes" }, returnTypes = { FlexRepResult.class, StartTaskNodesSet.class }, parameterLabels = {
		"Flexible model", "Start Task Nodes", "Event Log", "End Task Nodes", "Mapping", "Replay Algorithm", "Parameters" }, help = "Replay an event log on Flexible model for conformance analysis.", userAccessible = true)
public class FlexLogReplayer {

	/**
	 * Main replay method in which only Flexible model and log are provided.
	 * 
	 * @param context
	 * @param flex
	 * @param log
	 * @return
	 * @throws Exception
	 */
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "Arya Adriansyah", email = "a.adriansyah@tue.nl", pack = "Replayer")
	@PluginVariant(variantLabel = "From Flexible model and Log", requiredParameterLabels = { 0, 2 })
	public Object[] replayLog(final UIPluginContext context, Flex flex, XLog log) throws Exception {
		// obtain Start Task Nodes
		StartTaskNodesSet startTaskNodeSet = null;
		try {
			FlexStartTaskNodeConnection conn = context.getConnectionManager().getFirstConnection(
					FlexStartTaskNodeConnection.class, context, flex);

			startTaskNodeSet = conn.getObjectWithRole(FlexStartTaskNodeConnection.STARTTASKNODES);
		} catch (Exception exc) {
			context.log("No Starting Task Node is found, try to construct empty one");

			// search for a starting task node
			startTaskNodeSet = new StartTaskNodesSet();
			startTaskNodeSet.add(new SetFlex());
			context.addConnection(new FlexStartTaskNodeConnection("Connection between " + flex.getLabel()
					+ " and its start task nodes", flex, startTaskNodeSet));
			context.log("Starting Task Node is successfully constructed");
		}
		return replayLog(context, flex, startTaskNodeSet, log);
	}

	/**
	 * Secondary method in which start task nodes is provided
	 * @param context
	 * @param flex
	 * @param startTaskNodeSet
	 * @param log
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	// this is needed because we cast configuration result
	@PluginVariant(variantLabel = "From Flexible model, start task nodes, and Log", requiredParameterLabels = { 0, 1, 2 })
	public Object[] replayLog(final UIPluginContext context, Flex flex, StartTaskNodesSet startTaskNodeSet, XLog log)
			throws Exception {
		// check connection
		FlexStartTaskNodeConnection conn = context.getConnectionManager().getFirstConnection(
				FlexStartTaskNodeConnection.class, context, flex, startTaskNodeSet);
		if (conn != null) {

			// get end task nodes
			EndTaskNodesSet endTaskNodesSet = null;
			try {
				FlexEndTaskNodeConnection endTaskConn = context.getConnectionManager().getFirstConnection(
						FlexEndTaskNodeConnection.class, context, flex);
				endTaskNodesSet = endTaskConn.getObjectWithRole(FlexEndTaskNodeConnection.ENDTASKNODES);
			} catch (ConnectionCannotBeObtained connException) {
				endTaskNodesSet = new EndTaskNodesSet();
			}

			// get configuration values from user
			// checking for connection is performed inside FlexReplayerGUI class
			FlexReplayerUI flexReplayerUI = new FlexReplayerUI(context);
			Object[] resultConfiguration = flexReplayerUI.getConfiguration(flex, log, false);

			if (resultConfiguration == null) {
				context.getFutureResult(0).cancel(true);
				context.getFutureResult(1).cancel(true);
				return null;
			}

			// if all paramaters are set, replay log
			if (resultConfiguration[FlexReplayerUI.MAPPING] != null) {
				context.log("replay is performed. All parameters are set.");

				// we need flex model, log, mapping, algorithm, and parameters
				Object[] repResult = replayLogAssumingConnection(context, flex, startTaskNodeSet, log, endTaskNodesSet,
						(Collection<Pair<FlexNode, XEventClass>>) resultConfiguration[FlexReplayerUI.MAPPING],
						(IFlexLogReplayAlgorithm) resultConfiguration[FlexReplayerUI.ALGORITHM],
						(Object[]) resultConfiguration[FlexReplayerUI.PARAMETERS]);
				
				FlexRepResult result = (FlexRepResult) repResult[0]; 
				return new Object[] { result, startTaskNodeSet };

			} else {
				context.log("replay is not performed because not enough parameter is submitted");
				context.getFutureResult(0).cancel(true);
				context.getFutureResult(1).cancel(true);
				return null;
			}
		} else {
			context.getFutureResult(0).cancel(true);
			context.getFutureResult(1).cancel(true);
			return null;
		}
	}

	/**
	 * Replay log without any checking for connections. Codec is constructed as
	 * necessary, and it is preserved in ProM object pool. Connection to replay
	 * result is also generated
	 * 
	 * @param context
	 * @param flex
	 * @param startTaskNodesSet
	 * @param log
	 * @param endTaskNodesSet
	 * @param mapping
	 * @param iFlexLogReplayAlgorithm
	 * @param parameters
	 * @return
	 */
	@PluginVariant(variantLabel = "Complete parameters, assuming all connection exists", requiredParameterLabels = { 0,
			1, 2, 3, 4, 5, 6 })
	public Object[] replayLogAssumingConnection(PluginContext context, Flex flex,
			StartTaskNodesSet startTaskNodesSet, XLog log, EndTaskNodesSet endTaskNodesSet,
			Collection<Pair<FlexNode, XEventClass>> mapping, IFlexLogReplayAlgorithm iFlexLogReplayAlgorithm,
			Object[] parameters) {
		long startComputation = System.currentTimeMillis();

		// progress update
		Progress progress = context.getProgress();
		progress.setMinimum(0);
		progress.setMaximum(log.size() + 4);
		progress.setIndeterminate(false);

		FlexCodec codec = null;
		try {
			codec = (FlexCodec) context.getConnectionManager()
					.getFirstConnection(FlexCodecConnection.class, context, flex)
					.getObjectWithRole(FlexCodecConnection.FLEXCODEC);
		} catch (ConnectionCannotBeObtained e) {
			codec = new FlexCodec(flex);
			context.getProvidedObjectManager()
					.createProvidedObject("Flex Codec for " + flex.getLabel(), codec, context);
			context.addConnection(new FlexCodecConnection("Connection to FlexCodec of " + flex.getLabel(), flex, codec));
		} catch (Exception e) {
			e.printStackTrace();
			codec = new FlexCodec(flex);
			context.getProvidedObjectManager()
					.createProvidedObject("Flex Codec for " + flex.getLabel(), codec, context);
			context.addConnection(new FlexCodecConnection("Connection to FlexCodec of " + flex.getLabel(), flex, codec));
		}

		// for each trace, replay according to the algorithm
		FlexRepResult result = iFlexLogReplayAlgorithm.replayLog(context, flex, codec, startTaskNodesSet, endTaskNodesSet, log, mapping,
				parameters);

		long endComputation = System.currentTimeMillis();

		// set label
		context.log("Replay is performed in " + ((endComputation - startComputation) / (double) 1000) + "seconds",
				MessageLevel.DEBUG);

		context.addConnection(new FlexRepResultConnection("Connection to result of replaying "
				+ XConceptExtension.instance().extractName(log) + " on " + flex.getLabel(), flex,
				startTaskNodesSet, endTaskNodesSet, log, result));

		context.getFutureResult(0).setLabel(
				"Conformance analysis result between " + flex.getLabel() + " and "
						+ XConceptExtension.instance().extractName(log)
						+ " using " + iFlexLogReplayAlgorithm.toString());
		context.getFutureResult(1).setLabel("Start task node of " + flex.getLabel());

		return new Object[] { result, startTaskNodesSet };
	}
}