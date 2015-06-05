/**
 * 
 */
package org.processmining.plugins.flex.replayer.performance;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.Progress;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.framework.util.Pair;
import org.processmining.models.connections.flexiblemodel.FlexCodecConnection;
import org.processmining.models.connections.flexiblemodel.FlexEndTaskNodeConnection;
import org.processmining.models.connections.flexiblemodel.FlexLogConnection;
import org.processmining.models.connections.flexiblemodel.FlexPerfRepInfoConnection;
import org.processmining.models.connections.flexiblemodel.FlexRepResultConnection;
import org.processmining.models.connections.flexiblemodel.FlexSpecialNodesConnection;
import org.processmining.models.connections.flexiblemodel.FlexStartTaskNodeConnection;
import org.processmining.models.connections.flexiblemodel.conversion.ReplayFlexOfOrigFlexConnection;
import org.processmining.models.flexiblemodel.EndTaskNodesSet;
import org.processmining.models.flexiblemodel.Flex;
import org.processmining.models.flexiblemodel.FlexNode;
import org.processmining.models.flexiblemodel.StartTaskNodesSet;
import org.processmining.plugins.flex.replayer.algorithms.IFlexLogReplayAlgorithm;
import org.processmining.plugins.flex.replayer.performance.util.FlexPerfCalculator;
import org.processmining.plugins.flex.replayer.performance.util.FlexSpecialNodes;
import org.processmining.plugins.flex.replayer.performance.util.FlexToFlexMapping;
import org.processmining.plugins.flex.replayer.performance.util.OriginalFlexToILifecycleMap;
import org.processmining.plugins.flex.replayer.util.FlexBinding;
import org.processmining.plugins.flex.replayer.util.FlexCodec;
import org.processmining.plugins.flex.replayresult.FlexRepResult;
import org.processmining.plugins.flex.replayresult.performance.FlexPerfRepInfo;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;
import org.processmining.plugins.replayer.util.StepTypes;

/**
 * @author arya
 * 
 */
@Plugin(name = "Replay a Log on Flexible model for Performance Analysis", returnLabels = { "Performance analysis result" }, returnTypes = { FlexPerfRepInfo.class }, parameterLabels = {
		"Flexible model", "Event Log", "Mapping", "Start task nodes", "End task nodes", "Replay Algorithm", "Parameters" }, help = "Replay an event log on flexible model for performance analysis.", userAccessible = true, mostSignificantResult = -1)
public class FlexDynamicPerformanceReplay {

	@PluginVariant(variantLabel = "Complete parameters", requiredParameterLabels = { 0, 1, 2, 3, 4, 5, 6 })
	public FlexPerfRepInfo replayLogAssumingConnection(PluginContext context, Flex originalModel, XLog log,
			Map<FlexNode, XEventClass> mapping, StartTaskNodesSet originalStartTaskNodes, EndTaskNodesSet originalEndTaskNodes,
			IFlexLogReplayAlgorithm selectedAlg, Object[] parameters) {
		// create Flexible model that represents the original model (if not exist yet)
		// check if previously created flex is available
		FlexCodec originalModelCodec = null;
		try {
			originalModelCodec = context.tryToFindOrConstructFirstObject(FlexCodec.class, FlexCodecConnection.class,
					FlexCodecConnection.FLEXCODEC, originalModel);

		} catch (ConnectionCannotBeObtained exc) {
			abortPlugin(context, "Cannot construct Flexible model to be mapped, no codec exists.");
			return null;
		}
		
		// get special nodes
		FlexSpecialNodes specialNodes = null;
		try {
			specialNodes = context.tryToFindOrConstructFirstObject(FlexSpecialNodes.class, FlexSpecialNodesConnection.class, FlexSpecialNodesConnection.FLEXSPECIALNODES, originalModel);
		} catch (ConnectionCannotBeObtained exc){
			specialNodes = new FlexSpecialNodes();
		}
		
		// use the special nodes to construct these two objects
		Flex replayModel = null; // only exists if we have two models, and how to replay mapping result from one model to other model
		FlexToFlexMapping mappingToOriginalModel = null;
		StartTaskNodesSet startTaskNodeSetReplayModel = null;
		EndTaskNodesSet endTaskNodeSetReplayModel = null;
		OriginalFlexToILifecycleMap originalFlexToILifecycleMap = null;
		FlexLogConnection flexLogConn = null;
		try {
			mappingToOriginalModel = context.tryToFindOrConstructFirstObject(FlexToFlexMapping.class,
					ReplayFlexOfOrigFlexConnection.class,
					ReplayFlexOfOrigFlexConnection.MAPPINGTOORIGINAL, originalModel, specialNodes, log, originalStartTaskNodes, originalEndTaskNodes, mapping);

			ReplayFlexOfOrigFlexConnection conn = context.getConnectionManager().getFirstConnection(
					ReplayFlexOfOrigFlexConnection.class, context, log, originalModel, mappingToOriginalModel);
			replayModel = conn.getObjectWithRole(ReplayFlexOfOrigFlexConnection.REPLAYMODEL);
			originalFlexToILifecycleMap = conn
					.getObjectWithRole(ReplayFlexOfOrigFlexConnection.MAPPINGORIGINALNODETOLIFECYCLETYPE);
			
			// obtain startTaskNodesSet
			startTaskNodeSetReplayModel = context.tryToFindOrConstructFirstObject(StartTaskNodesSet.class,
					FlexStartTaskNodeConnection.class, FlexStartTaskNodeConnection.STARTTASKNODES, replayModel);

			// obtain endTaskNodesSet
			endTaskNodeSetReplayModel = context.tryToFindOrConstructFirstObject(EndTaskNodesSet.class,
					FlexEndTaskNodeConnection.class, FlexEndTaskNodeConnection.ENDTASKNODES, replayModel);
			
			// obtain mapping for replay 
			flexLogConn = context.getConnectionManager().getFirstConnection(FlexLogConnection.class, context, log, replayModel);
		} catch (ConnectionCannotBeObtained exc) {
			abortPlugin(context, "Cannot construct Flexible model for replay");
			return null;
		}
		
		// construct mapping between event class to transition of replay model
		Collection<Pair<FlexNode, XEventClass>> mappingNode2EvClassReplayModel = new HashSet<Pair<FlexNode, XEventClass>>();
		Collection<FlexNode> mappableNodes = flexLogConn.getMappableNodes();
		if (mappableNodes != null){
			for (FlexNode mappableNode : mappableNodes){
				Set<XEventClass> setEvts = flexLogConn.getActivitiesFor(mappableNode);
				if (setEvts != null){
					for (XEventClass evClass : setEvts){
						mappingNode2EvClassReplayModel.add(new Pair<FlexNode, XEventClass>(mappableNode, evClass));
					}
				}
			}
		}
		
		// replay log in replayModel
		// start update progress
		Progress progress = context.getProgress();
		progress.setIndeterminate(false);
		progress.setMinimum(0);
		progress.setMaximum(log.size() + 2000);

		FlexRepResult replayResult = null;
		try {
			replayResult = context.tryToFindOrConstructFirstObject(FlexRepResult.class, FlexRepResultConnection.class,
					FlexRepResultConnection.FLEXREPRESULT, replayModel, log, startTaskNodeSetReplayModel, endTaskNodeSetReplayModel, mappingNode2EvClassReplayModel, selectedAlg, parameters);
		} catch (ConnectionCannotBeObtained e) {
			abortPlugin(context, "Can't create replay result object");
			return null;
		}
		progress.setValue(2000);

		// obtain codec and node instance mapping for object
		FlexCodec codecReplayModel = null;
		
		try {
			codecReplayModel = context.tryToFindOrConstructFirstObject(FlexCodec.class,
					FlexCodecConnection.class, FlexCodecConnection.FLEXCODEC, replayModel);
		} catch (Exception exc){
			abortPlugin(context, "Cannot construct  codec for replay model");
		}

		// calculate transition
		FlexPerfCalculator calc = new FlexPerfCalculator(mappingToOriginalModel, originalModelCodec, null, specialNodes);
		XTimeExtension extractor = XTimeExtension.instance();

		for (SyncReplayResult caseResult : replayResult) {
			if (caseResult.isReliable()){
				for (int traceIndex : caseResult.getTraceIndex()) {
					XTrace trace = log.get(traceIndex);
					// get timestamp of the first event
					for (XEvent event : trace) {
						if (extractor.extractTimestamp(event) != null) {
							calc.setFirstTimestampOfCase(extractor.extractTimestamp(event));
							break;
						}
					}
	
					calc.setCodecReplayModel(codecReplayModel);
					calc.setMappingToOriginalModel(mappingToOriginalModel);
					calc.setOriginalFlexToLifecycleMap(originalFlexToILifecycleMap);
	
					Iterator<XEvent> traceIt = trace.iterator();
					Iterator<Object> flexBindingIt = caseResult.getNodeInstance().iterator();
					Iterator<StepTypes> stepTypeIt = caseResult.getStepTypes().iterator();
	
					while (stepTypeIt.hasNext()) {
						StepTypes currStepType = stepTypeIt.next(); 
						switch (currStepType) {
							case LMGOOD :
								Date timestamp = extractor.extractTimestamp(traceIt.next());
								if (timestamp != null) {
									calc.addBinding(((FlexBinding) flexBindingIt.next()), timestamp.getTime());
								} else {
									calc.addBindingInvi(((FlexBinding) flexBindingIt.next()));
								}
								break;
							case MINVI :
								calc.addBindingInvi((FlexBinding) flexBindingIt.next());
								break;
							case MREAL :
								// update with prediction on the moment it happens
								calc.addBindingInvi((FlexBinding) flexBindingIt.next());
								break;
							case L :
								// ignored
								traceIt.next();
								flexBindingIt.next();
								break;
							default :
								// exception, because other type is used
								return null;
						}
					}
	
					calc.finishOneCase();
				}
			} else {
				calc.addUnreliableCaseIndexes(caseResult);
			}
		}

		FlexPerfRepInfo repInfo = calc.finalizePerfRepInfo();

		// add connection from flex to info
		context.addConnection(new FlexPerfRepInfoConnection("Connection to performance from replaying "
				+ XConceptExtension.instance().extractName(log) + " on " + originalModel.getLabel(), originalModel,
				originalStartTaskNodes, log, repInfo));

		context.getFutureResult(0).setLabel(
				"Performance info from replaying " + XConceptExtension.instance().extractName(log) + " on "
						+ originalModel.getLabel());

		return repInfo;

	}

	// this is needed because we cast configuration result
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "Arya Adriansyah", email = "a.adriansyah@tue.nl", pack = "Replayer")
	@PluginVariant(variantLabel = "From Flexible model and a Log", requiredParameterLabels = { 0, 1 })
	public FlexPerfRepInfo replayLog(final UIPluginContext context, Flex originalModel, XLog log) throws Exception {
		// create Flexible model that represents the original model (if not exist yet)
		// check if previously created flex is available
		FlexCodec originalModelCodec = null;
		StartTaskNodesSet originalStartTaskNodes = null;
		try {
			originalModelCodec = context.tryToFindOrConstructFirstObject(FlexCodec.class, FlexCodecConnection.class,
					FlexCodecConnection.FLEXCODEC, originalModel);

			originalStartTaskNodes = context.tryToFindOrConstructFirstObject(StartTaskNodesSet.class,
					FlexStartTaskNodeConnection.class, FlexStartTaskNodeConnection.STARTTASKNODES, originalModel);

		} catch (ConnectionCannotBeObtained exc) {
			abortPlugin(context, "Cannot construct Flexible model to be mapped");
			return null;
		}

		// use the special nodes to construct these two objects
		Flex replayModel = null; // only exists if we have two models, and how to replay mapping result from one model to other model
		FlexToFlexMapping mappingToOriginalModel = null;
		StartTaskNodesSet startTaskNodeSetReplayModel = null;
		OriginalFlexToILifecycleMap originalFlexToILifecycleMap = null;
		try {
			mappingToOriginalModel = context.tryToFindOrConstructFirstObject(FlexToFlexMapping.class,
					ReplayFlexOfOrigFlexConnection.class, ReplayFlexOfOrigFlexConnection.MAPPINGTOORIGINAL,
					originalModel, log);

			ReplayFlexOfOrigFlexConnection conn = context.getConnectionManager().getFirstConnection(
					ReplayFlexOfOrigFlexConnection.class, context, log, originalModel, mappingToOriginalModel);
			replayModel = conn.getObjectWithRole(ReplayFlexOfOrigFlexConnection.REPLAYMODEL);
			originalFlexToILifecycleMap = conn
					.getObjectWithRole(ReplayFlexOfOrigFlexConnection.MAPPINGORIGINALNODETOLIFECYCLETYPE);

			// obtain startTaskNodesSet
			startTaskNodeSetReplayModel = context.tryToFindOrConstructFirstObject(StartTaskNodesSet.class,
					FlexStartTaskNodeConnection.class, FlexStartTaskNodeConnection.STARTTASKNODES, replayModel);

		} catch (ConnectionCannotBeObtained exc) {
			abortPlugin(context, "Cannot construct Flexible model for replay");
			return null;
		}

		// replay log in replayModel
		// start update progress
		Progress progress = context.getProgress();
		progress.setIndeterminate(false);
		progress.setMinimum(0);
		progress.setMaximum(log.size() + 2000);

		FlexRepResult replayResult = null;
		try {
			replayResult = context.tryToFindOrConstructFirstObject(FlexRepResult.class, FlexRepResultConnection.class,
					FlexRepResultConnection.FLEXREPRESULT, replayModel, startTaskNodeSetReplayModel, log);
		} catch (ConnectionCannotBeObtained e) {
			abortPlugin(context, "Can't create replay result object");
			return null;
		}
		progress.setValue(2000);

		// obtain codec and node instance mapping for object
		FlexCodec codecReplayModel = context.tryToFindOrConstructFirstObject(FlexCodec.class,
				FlexCodecConnection.class, FlexCodecConnection.FLEXCODEC, replayModel);

		// calculate transition
		// check for special nodes
		FlexSpecialNodes specialNodes = null;
		try {
			FlexSpecialNodesConnection conn = context.getConnectionManager().getFirstConnection(
					FlexSpecialNodesConnection.class, context, originalModel);
			specialNodes = conn.getObjectWithRole(FlexSpecialNodesConnection.FLEXSPECIALNODES);
		} catch (ConnectionCannotBeObtained exc) {
			specialNodes = new FlexSpecialNodes();
		}

		FlexPerfCalculator calc = new FlexPerfCalculator(mappingToOriginalModel, originalModelCodec, null, specialNodes);
		XTimeExtension extractor = XTimeExtension.instance();

		for (SyncReplayResult caseResult : replayResult) {
			if (caseResult.isReliable()) {
				for (int traceIndex : caseResult.getTraceIndex()) {
					XTrace trace = log.get(traceIndex);
					// get timestamp of the first event
					for (XEvent event : trace) {
						if (extractor.extractTimestamp(event) != null) {
							calc.setFirstTimestampOfCase(extractor.extractTimestamp(event));
							break;
						}
					}

					calc.setCodecReplayModel(codecReplayModel);
					calc.setMappingToOriginalModel(mappingToOriginalModel);
					calc.setOriginalFlexToLifecycleMap(originalFlexToILifecycleMap);

					Iterator<XEvent> traceIt = trace.iterator();
					Iterator<Object> flexBindingIt = caseResult.getNodeInstance().iterator();
					Iterator<StepTypes> stepTypeIt = caseResult.getStepTypes().iterator();

					while (stepTypeIt.hasNext()) {
						StepTypes currStepType = stepTypeIt.next();
						switch (currStepType) {
							case LMGOOD :
								Date timestamp = extractor.extractTimestamp(traceIt.next());
								if (timestamp != null) {
									calc.addBinding(((FlexBinding) flexBindingIt.next()), timestamp.getTime());
								} else {
									calc.addBindingInvi(((FlexBinding) flexBindingIt.next()));
								}
								break;
							case MINVI :
								calc.addBindingInvi((FlexBinding) flexBindingIt.next());
								break;
							case MREAL :
								// update with prediction on the moment it happens
								calc.addBindingInvi((FlexBinding) flexBindingIt.next());
								break;
							case L :
								// ignored
								traceIt.next();
								flexBindingIt.next();
								break;
							default :
								// exception, because other type is used
								return null;
						}
					}

					calc.finishOneCase();
				}
			} else {
				calc.addUnreliableCaseIndexes(caseResult);
			}
		}

		FlexPerfRepInfo repInfo = calc.finalizePerfRepInfo();

		// add connection from flex to info
		context.addConnection(new FlexPerfRepInfoConnection("Connection to performance from replaying "
				+ XConceptExtension.instance().extractName(log) + " on " + originalModel.getLabel(), originalModel,
				originalStartTaskNodes, log, repInfo));

		context.getFutureResult(0).setLabel(
				"Performance info from replaying " + XConceptExtension.instance().extractName(log) + " on "
						+ originalModel.getLabel());

		return repInfo;
	}

	private void abortPlugin(PluginContext context, String message) {
		context.getFutureResult(0).cancel(true);
		context.getFutureResult(1).cancel(true);
		context.log(message);
	}
}
