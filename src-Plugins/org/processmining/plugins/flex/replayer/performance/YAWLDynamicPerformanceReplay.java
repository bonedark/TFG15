/**
 * 
 */
package org.processmining.plugins.flex.replayer.performance;

import java.util.Date;
import java.util.Iterator;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.Progress;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.connections.flexiblemodel.FlexCancellationRegionConnection;
import org.processmining.models.connections.flexiblemodel.FlexCodecConnection;
import org.processmining.models.connections.flexiblemodel.FlexPerfRepInfoConnection;
import org.processmining.models.connections.flexiblemodel.FlexRepResultConnection;
import org.processmining.models.connections.flexiblemodel.FlexSpecialNodesConnection;
import org.processmining.models.connections.flexiblemodel.FlexStartTaskNodeConnection;
import org.processmining.models.connections.flexiblemodel.conversion.FlexOfYAWLConnection;
import org.processmining.models.connections.flexiblemodel.conversion.ReplayFlexOfOrigFlexConnection;
import org.processmining.models.flexiblemodel.CancellationRegion;
import org.processmining.models.flexiblemodel.Flex;
import org.processmining.models.flexiblemodel.StartTaskNodesSet;
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
import org.yawlfoundation.yawl.editor.net.NetGraph;

/**
 * @author aadrians
 * 
 */
@Plugin(name = "Replay a Log on YAWL Model for Performance Analysis", returnLabels = { "Flexible model of YAWL model",
		"Performance analysis result" }, returnTypes = { Flex.class, FlexPerfRepInfo.class }, parameterLabels = {
		"YAWL model", "Event Log" }, help = "Replay an event log on YAWL for performance analysis based on flexible model replay.", userAccessible = true)
public class YAWLDynamicPerformanceReplay {

	// this is needed because we cast configuration result
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "Arya Adriansyah", email = "a.adriansyah@tue.nl", pack="Replayer")
	@PluginVariant(variantLabel = "From YAWL Model and a Log", requiredParameterLabels = { 0, 1 })
	public Object[] replayLog(final UIPluginContext context, NetGraph yawlModel, XLog log) throws Exception {
		// create flexible model that represents the original model (if not exist yet)
		// check if previously created flex is available
		Flex originalModel = null;
		FlexCodec originalModelCodec = null;
		FlexSpecialNodes originalSpecialNodes = null;
		StartTaskNodesSet originalStartTaskNodes = null;
		CancellationRegion originalCancelationRegion = null;
		try {
			originalModel = context.tryToFindOrConstructFirstObject(Flex.class, FlexOfYAWLConnection.class,
					FlexOfYAWLConnection.FLEX, yawlModel);
			
			originalCancelationRegion = context.tryToFindOrConstructFirstObject(CancellationRegion.class,
					FlexCancellationRegionConnection.class, FlexCancellationRegionConnection.CANCELLATIONREGION,
					originalModel);

			originalSpecialNodes = context.tryToFindOrConstructFirstObject(FlexSpecialNodes.class,
					FlexSpecialNodesConnection.class, FlexSpecialNodesConnection.FLEXSPECIALNODES, originalModel);

			originalModelCodec = context.tryToFindOrConstructFirstObject(FlexCodec.class, FlexCodecConnection.class,
					FlexCodecConnection.FLEXCODEC, originalModel);

			originalStartTaskNodes = context.tryToFindOrConstructFirstObject(StartTaskNodesSet.class,
					FlexStartTaskNodeConnection.class, FlexStartTaskNodeConnection.STARTTASKNODES, originalModel);

			
		} catch (ConnectionCannotBeObtained exc) {
			abortPlugin(context, "Cannot construct flexible model to be mapped");
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
					originalModel, originalSpecialNodes, log, originalCancelationRegion);

			ReplayFlexOfOrigFlexConnection conn = context.getConnectionManager().getFirstConnection(
					ReplayFlexOfOrigFlexConnection.class, context, originalModel, log, originalSpecialNodes);
			replayModel = conn.getObjectWithRole(ReplayFlexOfOrigFlexConnection.REPLAYMODEL);
			originalFlexToILifecycleMap = conn
					.getObjectWithRole(ReplayFlexOfOrigFlexConnection.MAPPINGORIGINALNODETOLIFECYCLETYPE);

			// obtain startTaskNodesSet
			startTaskNodeSetReplayModel = context.tryToFindOrConstructFirstObject(StartTaskNodesSet.class,
					FlexStartTaskNodeConnection.class, FlexStartTaskNodeConnection.STARTTASKNODES, replayModel);

		} catch (ConnectionCannotBeObtained exc) {
			abortPlugin(context, "Cannot construct flexible model for replay");
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
		FlexPerfCalculator calc = new FlexPerfCalculator(mappingToOriginalModel, originalModelCodec,
				originalCancelationRegion, originalSpecialNodes);
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
				"Flexible model of " + yawlModel.getName() + "(one-to-one mapping to original model)");
		context.getFutureResult(1).setLabel(
				"Performance info from replaying " + XConceptExtension.instance().extractName(log) + " on "
						+ yawlModel.getName());

		return new Object[] { originalModel, repInfo };
	}

	private void abortPlugin(UIPluginContext context, String message) {
		context.getFutureResult(0).cancel(true);
		context.getFutureResult(1).cancel(true);
		context.log(message);
	}
}
