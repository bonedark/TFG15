/**
 * 
 */
package org.processmining.plugins.replayer.converter;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.swing.JOptionPane;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.framework.util.Pair;
import org.processmining.models.connections.flexiblemodel.FlexCancellationRegionConnection;
import org.processmining.models.connections.flexiblemodel.FlexCodecConnection;
import org.processmining.models.connections.flexiblemodel.FlexEndTaskNodeConnection;
import org.processmining.models.connections.flexiblemodel.FlexLogConnection;
import org.processmining.models.connections.flexiblemodel.FlexStartTaskNodeConnection;
import org.processmining.models.connections.flexiblemodel.conversion.ReplayFlexOfOrigFlexConnection;
import org.processmining.models.flexiblemodel.CancellationRegion;
import org.processmining.models.flexiblemodel.EndTaskNodesSet;
import org.processmining.models.flexiblemodel.Flex;
import org.processmining.models.flexiblemodel.FlexEdge;
import org.processmining.models.flexiblemodel.FlexFactory;
import org.processmining.models.flexiblemodel.FlexNode;
import org.processmining.models.flexiblemodel.SetFlex;
import org.processmining.models.flexiblemodel.StartTaskNodesSet;
import org.processmining.plugins.flex.replayer.performance.ui.SimpleMappingStep;
import org.processmining.plugins.flex.replayer.performance.util.FlexSpecialNodes;
import org.processmining.plugins.flex.replayer.performance.util.FlexToFlexMapping;
import org.processmining.plugins.flex.replayer.performance.util.LifecycleIdentifier;
import org.processmining.plugins.flex.replayer.performance.util.OriginalFlexToILifecycleMap;
import org.processmining.plugins.flex.replayer.util.FlexCodec;
import org.processmining.plugins.replayer.converter.lifecycletransition.FullLifecycleTransition;
import org.processmining.plugins.replayer.converter.lifecycletransition.ILifecycleTransition;
import org.processmining.plugins.replayer.converter.lifecycletransition.OnlyCompleteLifecycleTransition;
import org.processmining.plugins.replayer.converter.lifecycletransition.PlaceLifecycleTransition;
import org.processmining.plugins.replayer.converter.lifecycletransition.StartCompleteLifecycleTransition;
import org.processmining.plugins.replayer.util.LifecycleTypes;

/**
 * @author aadrians
 * 
 */
@Plugin(name = "Construct Flexible model from Another Flexible model with Cancellation Region for Replay", parameterLabels = {
		"Original Flexible model", "Special nodes", "Log", "Cancelation region" }, returnLabels = {
		"Flexible model for replay purpose", "Replay c-net start task nodes set", "Replay c-net end task nodes set",
		"Replay c-net cancelation region", "Mapping c-net for replay to the original model",
		"Mapping original c-net node to lifecycle transition", "Replay c-net codec", "Flexible model special nodes" }, returnTypes = {
		Flex.class, StartTaskNodesSet.class, EndTaskNodesSet.class, CancellationRegion.class, FlexToFlexMapping.class,
		OriginalFlexToILifecycleMap.class, FlexCodec.class, FlexSpecialNodes.class }, userAccessible = true)
public class FlexFromFlexWCancellationRegion {
	@PluginVariant(variantLabel = "Construct Flexible model to another Flexible model for replay", requiredParameterLabels = {
			0, 1, 2, 3 })
	public Object[] constructReplayModelAssumingSpecialNodesAndCancelationRegion(UIPluginContext context,
			Flex originalModel, FlexSpecialNodes specialNodes, XLog log, CancellationRegion cancellationRegion) {
		// obtain startTaskNodesSet
		StartTaskNodesSet startTaskNodeSet = null;
		try {
			startTaskNodeSet = context.tryToFindOrConstructFirstObject(StartTaskNodesSet.class,
					FlexStartTaskNodeConnection.class, FlexStartTaskNodeConnection.STARTTASKNODES, originalModel);
		} catch (ConnectionCannotBeObtained e) {
			context.log("no start task node can be obtained for this model");
			return null;
		}

		// obtain end task nodes set (optionally exist)
		EndTaskNodesSet endTaskNodeSet = null;
		try {
			endTaskNodeSet = context.tryToFindOrConstructFirstObject(EndTaskNodesSet.class,
					FlexEndTaskNodeConnection.class, FlexEndTaskNodeConnection.ENDTASKNODES, originalModel);
		} catch (ConnectionCannotBeObtained e) {
			context.log("no end task node can be obtained for this model");
		}

		// obtain codec for original model (optionally exist)
		FlexCodec codec = null;
		try {
			codec = context.tryToFindOrConstructFirstObject(FlexCodec.class, FlexCodecConnection.class,
					FlexCodecConnection.FLEXCODEC, originalModel);
		} catch (ConnectionCannotBeObtained e) {
			// codec for the original model needs to be created 
			codec = new FlexCodec(originalModel);
			context.addConnection(new FlexCodecConnection("Connection to codec of " + originalModel.getLabel(),
					originalModel, codec));
		}

		// connection is not found, ask user to map each nodes in Flexible model to event classes
		XEventClassifier nameClassifier = XLogInfoImpl.NAME_CLASSIFIER;
		XLogInfo summary = XLogInfoFactory.createLogInfo(log, nameClassifier);
		XEventClasses eventClassesName = summary.getEventClasses();

		XEventClassifier stdClassifier = XLogInfoImpl.STANDARD_CLASSIFIER;
		XLogInfo summaryStd = XLogInfoFactory.createLogInfo(log, stdClassifier);
		XEventClasses eventClassesStd = summaryStd.getEventClasses();

		SimpleMappingStep mappingStep = new SimpleMappingStep(originalModel, specialNodes, eventClassesName);
		InteractionResult result = context.showConfiguration("Map nodes to activities", mappingStep);

		if (result.equals(InteractionResult.CONTINUE)) {
			// smartly create mapping
			LifecycleIdentifier li = new LifecycleIdentifier();
			li.addSupportedLifecycle(new FullLifecycleTransition());
			li.addSupportedLifecycle(new OnlyCompleteLifecycleTransition());
			li.addSupportedLifecycle(new StartCompleteLifecycleTransition());

			// identify lifecycle of each activity
			Set<XEventClass> unmappedEC = li.identifyLifecycleForEachActivity(eventClassesName, eventClassesStd);
			if (unmappedEC != null) {
				showRequirementProblem(unmappedEC, "do not have appropriate lifecycle transition "
						+ "as the ones supported. Filter out event classes with lifecycle types other "
						+ "than 'start','complete','suspend', and 'resume'.");
				cancelAll(context);
				return null;
			}

			// check mapping: all event class must be mapped to at least one node
			Map<FlexNode, XEventClass>  map = mappingStep.getConfiguration();
			Collection<XEventClass> colEvClass = eventClassesName.getClasses();

			colEvClass.removeAll(map.values());
			if (!colEvClass.isEmpty()) {
				showRequirementProblem(
						colEvClass,
						"are not mapped to any nodes. Map all event classes to transitions, or filter unmapped event classes out from the log and then try again.");
				cancelAll(context);
				return null;
			} else {
				// construct Flexible model and new mapping
				return constructProcessModel(context, log, eventClassesStd, originalModel, specialNodes,
						startTaskNodeSet, endTaskNodeSet, cancellationRegion, codec,
						li.getLifecycleTransitionForEachActivity(), li.getMappingBetweenEventClass(),
						li.getMappingToLifecycleTypes(), mappingStep.getConfiguration());
			}
		} else {
			return null;
		}
	}

	private void showRequirementProblem(Collection<XEventClass> colEvClass, String errorMessage) {
		// requirement is not satisfied
		String message = "";
		String limiter = "";
		for (XEventClass evClass : colEvClass) {
			message += limiter;
			message += evClass.toString();
			limiter = "<br />- ";
		}

		JOptionPane.showMessageDialog(null, "<html>The following event class: <br/> - " + message + "<br/> "
				+ errorMessage + "</html>", "Unsatisfied Replay Requirement", JOptionPane.ERROR_MESSAGE);
	}

	private void cancelAll(UIPluginContext context) {
		for (int i = 0; i < 8; i++) {
			context.getFutureResult(i).cancel(true);
		}
	}

	private Object[] constructProcessModel(PluginContext context, XLog log, XEventClasses detailedEventClasses,
			Flex originalModel, FlexSpecialNodes specialNodes, StartTaskNodesSet startTaskNodeSet,
			EndTaskNodesSet endTaskNodeSet, CancellationRegion cancellationRegion, FlexCodec codec,
			Map<XEventClass, ILifecycleTransition> lifecycleTransitionForEachActivity,
			Map<XEventClass, Set<XEventClass>> mapGenericToDetailedEC,
			Map<XEventClass, LifecycleTypes> mappingDetailedECToLCTypes, Map<FlexNode, XEventClass> mapOriginalNodeToActEC) {

		// needed due to possible activity instance mapping. Output means output of the original model
		Map<FlexNode, FlexNode> inputMap = new HashMap<FlexNode, FlexNode>(); // input of a node in the original model
		Map<FlexNode, FlexNode> outputMap = new HashMap<FlexNode, FlexNode>(); // output of a node in the original model

		// arcs cancelled when a particular node is marked as cancelation region
		Map<FlexNode, Set<FlexEdge<? extends FlexNode, ? extends FlexNode>>> cancelledArcs = new HashMap<FlexNode, Set<FlexEdge<? extends FlexNode, ? extends FlexNode>>>();

		// result
		StartTaskNodesSet newStartTaskNodesSet = null;
		EndTaskNodesSet newEndTaskNodesSet = new EndTaskNodesSet();
		CancellationRegion newCancellationRegion = new CancellationRegion();
		Flex newCausal = FlexFactory.newFlex("Replay Flexible model of " + originalModel.getLabel());
		OriginalFlexToILifecycleMap originalFlexToILifecycleMap = new OriginalFlexToILifecycleMap();

		// utility
		FlexToFlexMapping flexMapping = new FlexToFlexMapping(); // mapping from Flexible model for replay to Flexible model for projection 

		// utility for replay
		Collection<Pair<FlexNode, XEventClass>> mappingForReplay = new HashSet<Pair<FlexNode, XEventClass>>(); // mapping that is going to be used for replay

		/**
		 * START CONSTRUCTING MODEL FOR REPLAY
		 */
		// construct start task nodes
		FlexNode originalStartTaskNode = null;
		if (startTaskNodeSet != null) {
			originalStartTaskNode = startTaskNodeSet.iterator().next().first();

			FlexNode newStartTaskNode = newCausal.addNode(originalStartTaskNode.getLabel());
			newStartTaskNode.setInvisible(true);
			SetFlex newStartTaskSetFlex = new SetFlex();
			newStartTaskSetFlex.add(newStartTaskNode);

			newStartTaskNodesSet = new StartTaskNodesSet();
			newStartTaskNodesSet.add(newStartTaskSetFlex);

			flexMapping.put(newStartTaskNode, new Pair<FlexNode, LifecycleTypes>(originalStartTaskNode,
					LifecycleTypes.COMPLETE));

			inputMap.put(originalStartTaskNode, newStartTaskNode);
			outputMap.put(originalStartTaskNode, newStartTaskNode);
		}
		// construct end task nodes
		FlexNode originalEndTaskNode = null;
		if (endTaskNodeSet != null) {
			originalEndTaskNode = endTaskNodeSet.iterator().next().first();

			FlexNode newEndTaskNode = newCausal.addNode(originalEndTaskNode.getLabel());
			newEndTaskNode.setInvisible(true);
			SetFlex newEndTaskSetFlex = new SetFlex();
			newEndTaskSetFlex.add(newEndTaskNode);

			newEndTaskNodesSet = new EndTaskNodesSet();
			newEndTaskNodesSet.add(newEndTaskSetFlex);

			flexMapping.put(newEndTaskNode, new Pair<FlexNode, LifecycleTypes>(originalEndTaskNode,
					LifecycleTypes.COMPLETE));

			inputMap.put(originalEndTaskNode, newEndTaskNode);
			outputMap.put(originalEndTaskNode, newEndTaskNode);
		}

		// set special nodes
		Set<FlexNode> lateBindingNodes = specialNodes.getSetLateBindingNodes();
		Set<FlexNode> compositeTasks = specialNodes.getCompositeTasks();
		Set<FlexNode> multipleAtomicTasks = specialNodes.getMultipleAtomicTasks();
		Set<FlexNode> multipleCompositeTasks = specialNodes.getMultipleCompositeTasks();

		// exclude start and end nodes
		for (FlexNode originalNode : originalModel.getNodes()) {
			if (!originalNode.equals(originalStartTaskNode) && !(originalNode.equals(originalEndTaskNode))) {
				XEventClass originalActivity = mapOriginalNodeToActEC.get(originalNode);
				if (lateBindingNodes.contains(originalNode)) {
					// place-like nodes
					FlexNode openingNode = newCausal.addNode(originalNode.getLabel() + "_op");
					openingNode.setInvisible(true);
					flexMapping.put(openingNode, new Pair<FlexNode, LifecycleTypes>(originalNode,
							LifecycleTypes.PLACE_START));
					FlexNode closingNode = newCausal.addNode(originalNode.getLabel() + "_cl");
					closingNode.setInvisible(true);
					flexMapping.put(closingNode, new Pair<FlexNode, LifecycleTypes>(originalNode,
							LifecycleTypes.PLACE_END));

					// lifecycle of a 
					originalFlexToILifecycleMap.put(originalNode, new PlaceLifecycleTransition());

					// add arc in between
					FlexEdge<? extends FlexNode, ? extends FlexNode> arc = newCausal.addArc(openingNode, closingNode);

					// add set input output
					SetFlex setOut = new SetFlex();
					setOut.add(closingNode);
					openingNode.addOutputNodes(setOut);

					SetFlex setIn = new SetFlex();
					setIn.add(openingNode);
					closingNode.addInputNodes(setIn);

					// update cancellation set for the place
					insertToMap(originalNode, arc, cancelledArcs);

					// update input mapping
					inputMap.put(originalNode, openingNode);
					outputMap.put(originalNode, closingNode);
				} else if (compositeTasks.contains(originalNode)) {
					// composite task are currently not supported
					context.log("The original model contains composite tasks. Conversion to Flexible model for replay is NOT supported. ");
					return null;
				} else if (multipleCompositeTasks.contains(originalNode)) {
					// composite tasks are currently not supported 
					context.log("The original model contains multiple executable composite tasks. Conversion to Flexible model for replay is NOT supported. ");
					return null;
				} else if (originalActivity.equals(SimpleMappingStep.DUMMYEVENTCLASS)) { // unmapped original task
					// map to invisible
					FlexNode notMappedNode = newCausal.addNode(originalNode.getLabel());
					notMappedNode.setInvisible(true);
					inputMap.put(originalNode, notMappedNode);
					outputMap.put(originalNode, notMappedNode);

					flexMapping.put(notMappedNode, new Pair<FlexNode, LifecycleTypes>(originalNode,
							LifecycleTypes.COMPLETE));

					// update mapping original node
					originalFlexToILifecycleMap.put(originalNode, new OnlyCompleteLifecycleTransition());
				} else {
					// it has to be normal node single atomic task or multipleAtomicTasks, can be mapped to activity
					ILifecycleTransition lifecycle = lifecycleTransitionForEachActivity.get(originalActivity);

					// create at least one node
					FlexNode startInstanceNode = newCausal.addNode(originalNode.getLabel() + "+"
							+ lifecycle.getStartLifecycle().toString());

					// check mapping for the start instance
					for (XEventClass detailedEC : mapGenericToDetailedEC.get(originalActivity)) {
						if (mappingDetailedECToLCTypes.get(detailedEC).equals(lifecycle.getStartLifecycle())) {
							mappingForReplay.add(new Pair<FlexNode, XEventClass>(startInstanceNode, detailedEC));
							break;
						}
					}

					// cancellation region is only when start is executed
					if (cancellationRegion.containsKey(originalNode)) {
						newCancellationRegion.put(startInstanceNode, new HashSet<Pair<FlexNode, FlexNode>>());
					}

					// set in for this node 
					inputMap.put(originalNode, startInstanceNode);
					FlexNode endInstanceNode = constructTransitions(lifecycle, startInstanceNode, newCausal,
							originalNode, cancelledArcs, flexMapping, mapGenericToDetailedEC,
							mappingDetailedECToLCTypes, mappingForReplay, originalActivity);

					// set out for this node 
					outputMap.put(originalNode, endInstanceNode);

					// update mapping original node
					originalFlexToILifecycleMap.put(originalNode, lifecycle);
				}
			}
		}

		// updating arcs 
		// updating input/output sets
		// update cancellation region
		for (FlexNode originalNode : originalModel.getNodes()) {

			// create similar input setFlex
			FlexNode inputOfOriginalNode = inputMap.get(originalNode);
			for (SetFlex inputSetFlex : originalNode.getInputNodes()) {
				SetFlex newInputSetFlex = new SetFlex();
				for (FlexNode inputNode : inputSetFlex) {
					newInputSetFlex.add(outputMap.get(inputNode));
					FlexNode from = outputMap.get(inputNode);
					if (newCausal.getArc(from, inputOfOriginalNode) == null) {
						newCausal.addArc(from, inputOfOriginalNode);
					}
				}
				inputOfOriginalNode.addInputNodes(newInputSetFlex);
			}

			// create similar output setFlex
			FlexNode outputOfOriginalNode = outputMap.get(originalNode);
			for (SetFlex outputSetFlex : originalNode.getOutputNodes()) {
				SetFlex newOutputSetFlex = new SetFlex();
				for (FlexNode outputNode : outputSetFlex) {
					newOutputSetFlex.add(inputMap.get(outputNode));
					FlexNode to = inputMap.get(outputNode);
					if (newCausal.getArc(outputOfOriginalNode, to) == null) {
						newCausal.addArc(outputOfOriginalNode, to);
					}
				}
				outputOfOriginalNode.addOutputNodes(newOutputSetFlex);
			}

			// special for multiple atomic tasks (task that can be multiply executed)
			if (multipleAtomicTasks.contains(originalNode)) {
				// the node can activate itselfs
				SetFlex newInputSetFlex = new SetFlex();
				newInputSetFlex.add(inputOfOriginalNode);
				inputOfOriginalNode.addInputNodes(newInputSetFlex);

				if (newCausal.getArc(inputOfOriginalNode, inputOfOriginalNode) == null) {
					FlexEdge<FlexNode, FlexNode> arc = newCausal.addArc(inputOfOriginalNode, inputOfOriginalNode);
					insertToMap(originalNode, arc, cancelledArcs);
				}

				Set<SetFlex> setToActivateItself = new HashSet<SetFlex>();
				for (SetFlex setOut : inputOfOriginalNode.getOutputNodes()) {
					// make output set that activate itself as well as progressing
					SetFlex newOutputSetFlex = new SetFlex();
					newOutputSetFlex.addAll(setOut);
					newOutputSetFlex.add(inputOfOriginalNode);
					setToActivateItself.add(newOutputSetFlex);
				}
				for (SetFlex newSetToActivateItself : setToActivateItself) {
					inputOfOriginalNode.addOutputNodes(newSetToActivateItself);
				}

				// the outcomes also need to be sound, hence make input set that can also synchronize with itself
				SetFlex newOutputSetFlex = new SetFlex();
				newOutputSetFlex.add(outputOfOriginalNode);
				outputOfOriginalNode.addOutputNodes(newOutputSetFlex);

				if (newCausal.getArc(outputOfOriginalNode, outputOfOriginalNode) == null) {
					FlexEdge<FlexNode, FlexNode> arc = newCausal.addArc(outputOfOriginalNode, outputOfOriginalNode);
					insertToMap(originalNode, arc, cancelledArcs);
				}

				Set<SetFlex> setToSyncItself = new HashSet<SetFlex>();
				for (SetFlex setIn : outputOfOriginalNode.getInputNodes()) {
					// make input set that collects remaining tokens
					SetFlex newInSetFlex = new SetFlex();
					newInSetFlex.addAll(setIn);
					newInSetFlex.add(outputOfOriginalNode);
					setToSyncItself.add(setIn);

				}
				for (SetFlex newSetToSyncItself : setToSyncItself) {
					outputOfOriginalNode.addInputNodes(newSetToSyncItself);
				}
			}

			// update cancellation region for nodes
			Set<FlexNode> cancelSets = cancellationRegion.getNodeCancellationFor(originalNode);
			if (cancelSets != null) {
				// there is node that cancelled
				for (FlexNode cancelledNode : cancelSets) {
					Set<FlexEdge<? extends FlexNode, ? extends FlexNode>> arcs = cancelledArcs.get(cancelledNode);
					if (arcs != null) {
						for (FlexEdge<? extends FlexNode, ? extends FlexNode> arc : arcs) {
							insertToMap(inputMap.get(originalNode),
									new Pair<FlexNode, FlexNode>(arc.getSource(), arc.getTarget()),
									newCancellationRegion);
						}
					}

					if (multipleAtomicTasks.contains(cancelledNode)) { // multiple atomic tasks is also cancelled
						insertToMap(inputMap.get(originalNode),
								new Pair<FlexNode, FlexNode>(inputMap.get(cancelledNode), inputMap.get(cancelledNode)),
								newCancellationRegion);
					}
				}
			}
		}

		// update cancellation region for arcs
		for (FlexNode node : cancellationRegion.keySet()) {
			for (Pair<FlexNode, FlexNode> pair : cancellationRegion.get(node)) {
				insertToMap(inputMap.get(node),
						new Pair<FlexNode, FlexNode>(outputMap.get(pair.getFirst()), inputMap.get(pair.getSecond())),
						newCancellationRegion);
			}
		}

		// commit all updates in the new Flexible model
		for (FlexNode node : newCausal.getNodes()) {
			node.commitUpdates();
		}

		context.addConnection(new FlexLogConnection(log, detailedEventClasses, newCausal, mappingForReplay));

		// create codec
		FlexCodec newCodec = new FlexCodec(newCausal);

		// create necessary connections
		context.addConnection(new FlexStartTaskNodeConnection("Connection to start task node of "
				+ newCausal.getLabel(), newCausal, newStartTaskNodesSet));
		context.addConnection(new FlexEndTaskNodeConnection("Connection to end task node of " + newCausal.getLabel(),
				newCausal, newEndTaskNodesSet));
		context.addConnection(new FlexCancellationRegionConnection("Connection to cancellation region of "
				+ newCausal.getLabel(), newCausal, newCancellationRegion));
		context.addConnection(new FlexCodecConnection("Connection to codec of " + newCausal.getLabel(), newCausal,
				newCodec));
		context.addConnection(new ReplayFlexOfOrigFlexConnection("Connection to replay Flexible model of "
				+ originalModel.getLabel(), originalModel, specialNodes, log, flexMapping, originalFlexToILifecycleMap, newCausal));

		context.getFutureResult(0).setLabel("Replay Flexible model of " + originalModel.getLabel());
		context.getFutureResult(1).setLabel("Replay start task node set of " + newCausal.getLabel());
		context.getFutureResult(2).setLabel("Replay end task node set of " + newCausal.getLabel());
		context.getFutureResult(3).setLabel("Replay cancellation region of " + newCausal.getLabel());
		context.getFutureResult(4).setLabel(
				"Mapping from replay model to original model (" + newCausal.getLabel() + " to "
						+ originalModel.getLabel());
		context.getFutureResult(5).setLabel(
				"Mapping from nodes of " + newCausal.getLabel() + " to lifecycle transition");
		context.getFutureResult(6).setLabel("Codec of " + newCausal.getLabel());
		context.getFutureResult(7).setLabel("Special nodes of " + newCausal.getLabel());

		return new Object[] { newCausal, newStartTaskNodesSet, newEndTaskNodesSet, newCancellationRegion, flexMapping,
				originalFlexToILifecycleMap, newCodec, specialNodes };
	}

	private FlexNode constructTransitions(ILifecycleTransition lifecycle, FlexNode startInstanceNode, Flex newCausal,
			FlexNode originalNode, Map<FlexNode, Set<FlexEdge<? extends FlexNode, ? extends FlexNode>>> cancelledArcs,
			FlexToFlexMapping flexMapping, Map<XEventClass, Set<XEventClass>> mapGenericToDetailedEC,
			Map<XEventClass, LifecycleTypes> mappingDetailedECToLCTypes,
			Collection<Pair<FlexNode, XEventClass>> mappingForReplay, XEventClass originalActivity) {
		// more than two instance in the same activity lifecycle
		Map<LifecycleTypes, FlexNode> tempMap = new HashMap<LifecycleTypes, FlexNode>();
		tempMap.put(lifecycle.getStartLifecycle(), startInstanceNode);
		flexMapping.put(startInstanceNode,
				new Pair<FlexNode, LifecycleTypes>(originalNode, lifecycle.getStartLifecycle()));

		Stack<LifecycleTypes> notExpanded = new Stack<LifecycleTypes>();
		notExpanded.add(lifecycle.getStartLifecycle());

		// map event class to the start of a lifecycle
		for (XEventClass detailedEC : mapGenericToDetailedEC.get(originalActivity)) {
			if (mappingDetailedECToLCTypes.get(detailedEC).equals(lifecycle.getStartLifecycle())) {
				mappingForReplay.add(new Pair<FlexNode, XEventClass>(startInstanceNode, detailedEC));
				break;
			}
		}

		// start lifecycle is created
		LifecycleTypes notYetExpandedLifecycle;
		Set<LifecycleTypes> nextLifeCycles;

		while (!notExpanded.isEmpty()) {
			notYetExpandedLifecycle = notExpanded.pop();
			nextLifeCycles = lifecycle.getNextLifecycle(notYetExpandedLifecycle);
			if (nextLifeCycles != null) {

				// prepare input
				SetFlex setFlexInput = new SetFlex();
				setFlexInput.add(tempMap.get(notYetExpandedLifecycle));

				nextLifecycle: for (LifecycleTypes nextLC : nextLifeCycles) {
					FlexNode newNode = tempMap.get(nextLC);
					if (newNode == null) {
						// create new node
						newNode = newCausal.addNode(originalNode.getLabel() + "+" + nextLC.toString());
						tempMap.put(nextLC, newNode);

						notExpanded.add(nextLC);
					}
					// connect with previous node
					SetFlex setFlexOutput = new SetFlex();
					setFlexOutput.add(newNode);

					tempMap.get(notYetExpandedLifecycle).addOutputNodes(setFlexOutput);

					// other way around
					newNode.addInputNodes(setFlexInput);

					// add arc
					FlexEdge<? extends FlexNode, ? extends FlexNode> arc = newCausal.addArc(
							tempMap.get(notYetExpandedLifecycle), newNode);

					// add constructed arc
					insertToMap(originalNode, arc, cancelledArcs);

					// add mapping 
					flexMapping.put(newNode, new Pair<FlexNode, LifecycleTypes>(originalNode, nextLC));

					// add mapping to event class
					for (XEventClass detailedEC : mapGenericToDetailedEC.get(originalActivity)) {
						if (mappingDetailedECToLCTypes.get(detailedEC).equals(nextLC)) {
							mappingForReplay.add(new Pair<FlexNode, XEventClass>(newNode, detailedEC));
							continue nextLifecycle;
						}
					}

					// no event class is mapped to this node
					newNode.setInvisible(true);
				}
			}
		}

		// until this point, we should get our end instance
		return tempMap.get(lifecycle.getEndLifecycle());
	}

	private <S, T> void insertToMap(S key, T value, Map<S, Set<T>> map) {
		Set<T> set = map.get(key);
		if (set != null) {
			set.add(value);
		} else {
			set = new HashSet<T>();
			set.add(value);
			map.put(key, set);
		}
	}
}
