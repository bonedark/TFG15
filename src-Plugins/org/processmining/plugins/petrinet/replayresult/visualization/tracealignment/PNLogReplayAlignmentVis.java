/**
 * 
 */
package org.processmining.plugins.petrinet.replayresult.visualization.tracealignment;

/**
 * @author aadrians Nov 10, 2011
 * 
 */
/**
 * 
 */
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.swing.JComponent;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.PluginContextID;
import org.processmining.framework.plugin.PluginExecutionResult;
import org.processmining.framework.plugin.PluginParameterBinding;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.framework.plugin.events.Logger;
import org.processmining.models.connections.petrinets.PNRepResultAllRequiredParamConnection;
import org.processmining.plugins.guidetreeminer.GuideTreeMinerInput;
import org.processmining.plugins.guidetreeminer.tree.GuideTree;
import org.processmining.plugins.guidetreeminer.types.AHCJoinType;
import org.processmining.plugins.guidetreeminer.types.DistanceMetricType;
import org.processmining.plugins.guidetreeminer.types.GTMFeature;
import org.processmining.plugins.guidetreeminer.types.GTMFeatureType;
import org.processmining.plugins.guidetreeminer.types.LearningAlgorithmType;
import org.processmining.plugins.guidetreeminer.types.SimilarityDistanceMetricType;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;
import org.processmining.plugins.replayer.util.StepTypes;
import org.processmining.plugins.tracealignment.TraceAlignmentWithGuideTreeInput;
import org.processmining.plugins.tracealignment.enumtypes.AlignmentAlgorithm;
import org.processmining.plugins.tracealignment.enumtypes.ScoringMatrix;
import org.processmining.plugins.tracealignment.tree.AlignmentTree;
import org.processmining.plugins.tracealignment.visualization.AlignmentFrame;

/**
 * 
 * @author aadrians
 * @mail a.adriansyah@tue.nl
 * @since Nov 10, 2011
 */
@Plugin(name = "View Trace Alignment", returnLabels = { "Visualized as Trace Alignment" }, returnTypes = { JComponent.class }, parameterLabels = { "Log-Model alignment" }, userAccessible = false)
@Visualizer
public class PNLogReplayAlignmentVis {
	@PluginVariant(requiredParameterLabels = { 0 })
	public JComponent visualize(final PluginContext context, PNRepResult logReplayResult) {
		System.gc();
		try {
			// find previous log
			PNRepResultAllRequiredParamConnection conn = context.getConnectionManager().getFirstConnection(
					PNRepResultAllRequiredParamConnection.class, context, logReplayResult);
			XLog log = conn.getObjectWithRole(PNRepResultAllRequiredParamConnection.LOG);

			// create a log based on alignment
			XFactory factory = XFactoryRegistry.instance().currentDefault();
			XConceptExtension conceptExtension = XConceptExtension.instance();

			// result
			XLog outputLog = factory.createLog();

			// create event class for each pair of movement + log 
			for (SyncReplayResult repRes : logReplayResult) {
				// store all result
				XTrace[] result = new XTrace[repRes.getTraceIndex().size()];
				for (int i = 0; i < result.length; i++) {
					result[i] = factory.createTrace();
				}

				Iterator<Object> it = repRes.getNodeInstance().iterator();
				for (StepTypes stepType : repRes.getStepTypes()) {
					switch (stepType) {
						case L :
							addNewEvent(factory, conceptExtension, result, it.next(), "[Move Log]");
							break;
						case LMGOOD :
							addNewEvent(factory, conceptExtension, result, it.next(), "[Move Log+Model]");
							break;
						case MINVI :
							addNewEvent(factory, conceptExtension, result, it.next(), "[Move Model (invi)]");
							break;
						case MREAL :
							addNewEvent(factory, conceptExtension, result, it.next(), "[Move Model]");
							break;
						case LMNOGOOD :
							addNewEvent(factory, conceptExtension, result, it.next(), "[Move Log+Model (violating)]");
							break;
					}
				}

				int i = 0;
				for (Integer traceIndex : repRes.getTraceIndex()) {
					XAttribute traceNameAttr = factory.createAttributeLiteral("concept:name",
							conceptExtension.extractName(log.get(traceIndex)), conceptExtension);
					result[i].getAttributes().put("concept:name", traceNameAttr);
					outputLog.add(result[i]);
					i++;
				}
			}

			// call trace alignment
			AlignmentTree alignmentTree = callTraceAlignmentPlugin(context, outputLog);

			// return visualization
			return new AlignmentFrame(alignmentTree);
		} catch (Exception exc) {
			context.log("No net can be found for this log replay result");
			return null;
		}
	}

	/**
	 * @param factory
	 * @param conceptExtension
	 * @param result
	 * @param it
	 */
	private void addNewEvent(XFactory factory, XConceptExtension conceptExtension, XTrace[] result, Object obj,
			String sign) {
		for (int i = 0; i < result.length; i++) {
			XEvent outputEvent = factory.createEvent();
			XAttribute name = factory.createAttributeLiteral("concept:name", obj.toString() + sign, conceptExtension);
			outputEvent.getAttributes().put("concept:name", name);
			XAttribute transition = factory.createAttributeLiteral("lifecycle:transition", "complete",
					XConceptExtension.instance());
			outputEvent.getAttributes().put("lifecycle:transition", transition);
			result[i].add(outputEvent);
		}
	}

	class ChildLogger implements Logger {

		public void log(String message, PluginContextID contextID, MessageLevel messageLevel) {
			// ignore
		}

		public void log(Throwable t, PluginContextID contextID) {
			// ignore
		}

	}

	private AlignmentTree callTraceAlignmentPlugin(final PluginContext context, XLog log) {
		ChildLogger logger = new ChildLogger();

		final PluginContext child = context.createChildContext("trace-alignment");
		context.getPluginLifeCycleEventListeners().firePluginCreated(child);
		child.getLoggingListeners().add(logger);

		GuideTree guideTree = null;
		GuideTreeMinerInput guideTreeMinerInput = new GuideTreeMinerInput();
		guideTreeMinerInput.setFeatureType(GTMFeatureType.Alphabet);
		guideTreeMinerInput.addFeature(GTMFeature.MRA);
		guideTreeMinerInput.setSimilarityDistanceMetricType(SimilarityDistanceMetricType.Distance);
		guideTreeMinerInput.setDistanceMetricType(DistanceMetricType.Euclidean);
		guideTreeMinerInput.setNominalFeatureCount(false);
		guideTreeMinerInput.setBaseFeatures(false);
		guideTreeMinerInput.setLearningAlgorithmType(LearningAlgorithmType.AHC);
		guideTreeMinerInput.setAhcJoinType(AHCJoinType.MinVariance);
		guideTreeMinerInput.setNumberOfClusters(3);

		Set<PluginParameterBinding> plugins = context.getPluginManager().getPluginsAcceptingOrdered(child.getClass(),
				false, log.getClass(), guideTreeMinerInput.getClass());
		for (PluginParameterBinding pBinding : plugins) {
			if (pBinding.getPlugin().getName().equals("Guide Tree Miner")) {
				PluginExecutionResult guideTreeResult = pBinding.invoke(child, log, guideTreeMinerInput);

				try {
					guideTreeResult.synchronize();
				} catch (CancellationException e) {
					context.getFutureResult(0).cancel(true);
				} catch (ExecutionException e) {
					context.getFutureResult(0).cancel(true);
				} catch (InterruptedException e) {
					context.getFutureResult(0).cancel(true);
				}

				guideTree = guideTreeResult.getResult(0);
				break;
			}
		}

		if (guideTree != null) {
			// call alignment tree
			final PluginContext anotherChild = context.createChildContext("trace-alignment");
			context.getPluginLifeCycleEventListeners().firePluginCreated(anotherChild);
			anotherChild.getLoggingListeners().add(logger);

			TraceAlignmentWithGuideTreeInput traceAlignmentInput = new TraceAlignmentWithGuideTreeInput();
			traceAlignmentInput.setIsDeriveSubstitutionIndelScores(true);
			traceAlignmentInput.setScoringMatrix(ScoringMatrix.Derive);
			traceAlignmentInput.setIsIncrementLikeSubstitutionScores(true);
			traceAlignmentInput.setIncrementLikeSubstitutionScoreValue(4);
			traceAlignmentInput.setIsScaleIndelScores(false);
			traceAlignmentInput.setAlignmentAlgorithm(AlignmentAlgorithm.ProfileAlignment);

			Set<PluginParameterBinding> plugins2 = context.getPluginManager().getPluginsAcceptingOrdered(
					anotherChild.getClass(), false, guideTree.getClass(), traceAlignmentInput.getClass());
			for (PluginParameterBinding pBinding2 : plugins2) {
				if (pBinding2.getPlugin().getName().equals("Trace Alignment (with Guide Tree)")) {
					PluginExecutionResult traceAlignmentResult = pBinding2.invoke(anotherChild, guideTree,
							traceAlignmentInput);

					try {
						traceAlignmentResult.synchronize();
					} catch (CancellationException e) {
						context.getFutureResult(0).cancel(true);
					} catch (ExecutionException e) {
						context.getFutureResult(0).cancel(true);
					} catch (InterruptedException e) {
						context.getFutureResult(0).cancel(true);
					}

					return traceAlignmentResult.getResult(0);
				}
			}
		}
		context.getFutureResult(0).cancel(true);
		return null;
	}
}
