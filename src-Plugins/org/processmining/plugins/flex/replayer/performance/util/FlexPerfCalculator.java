/**
 * 
 */
package org.processmining.plugins.flex.replayer.performance.util;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.processmining.framework.util.Pair;
import org.processmining.models.flexiblemodel.CancellationRegion;
import org.processmining.models.flexiblemodel.FlexNode;
import org.processmining.plugins.flex.replayer.util.FlexBinding;
import org.processmining.plugins.flex.replayer.util.FlexCodec;
import org.processmining.plugins.flex.replayresult.performance.CaseInstance;
import org.processmining.plugins.flex.replayresult.performance.EdgeInstance;
import org.processmining.plugins.flex.replayresult.performance.FlexBindingInstance;
import org.processmining.plugins.flex.replayresult.performance.FlexPerfRepInfo;
import org.processmining.plugins.replayer.converter.lifecycletransition.ILifecycleTransition;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;
import org.processmining.plugins.replayer.util.LifecycleTypes;

/**
 * @author aadrians
 * 
 */
public class FlexPerfCalculator {
	// mapping to "original" model 
	private FlexCodec codecOriginalModel;
	private FlexCodec codecReplayModel;
	private FlexToFlexMapping mappingToOriginalModel;
	private OriginalFlexToILifecycleMap originalFlexToILifecycleMap;
	private CancellationRegion cancelationRegion;
	private FlexSpecialNodes specialNodes;
	private SortedSet<Integer> unreliableCaseIndexes = null;

	// store unfinished instance of a node
	// queue of set of input bindings and time the node instance starts
	private Map<FlexNode, Queue<PartialBindingPerformance>> startNodeInstances = new HashMap<FlexNode, Queue<PartialBindingPerformance>>();

	// store obligations
	private final Map<Pair<FlexNode, FlexNode>, Queue<Long>> obligationsReplayModel = new HashMap<Pair<FlexNode, FlexNode>, Queue<Long>>();

	// long data of obligationsFromPlace consists the first time a token comes to the place
	// after a transition take a token from the untimedObligations, move all pair in the set to obligations and update timestamp 
	private List<UntimedObligation> untimedObligationsReplayModel = new LinkedList<UntimedObligation>();

	// store result
	private Map<FlexBinding, FlexBindingInstanceAccumulator> mapNodesToAccumulator = new HashMap<FlexBinding, FlexBindingInstanceAccumulator>();
	private Map<Pair<FlexNode, FlexNode>, EdgeInstanceAccumulator> mapEdgeToAccumulator = new HashMap<Pair<FlexNode, FlexNode>, EdgeInstanceAccumulator>();
	private CaseInstanceAccumulator caseAcc = new CaseInstanceAccumulator();
	private Map<FlexNode, Integer> unfinishedFrequency = new HashMap<FlexNode, Integer>();
	private Map<FlexNode, Integer> canceledFrequency = new HashMap<FlexNode, Integer>();
	private Map<Pair<FlexNode, FlexNode>, Integer> canceledArcFrequency = new HashMap<Pair<FlexNode, FlexNode>, Integer>();

	// case involvement
	private Map<FlexNode, Integer> caseInvolvementNode = new HashMap<FlexNode, Integer>();

	// temporary variable to calculate case related metrics
	private Set<Pair<FlexNode, FlexNode>> involvedEdges = new HashSet<Pair<FlexNode, FlexNode>>();
	private Set<FlexNode> involvedNodes = new HashSet<FlexNode>();

	// starting case
	private long startCaseTime = 0;
	private long endCaseTime = 0;

	@SuppressWarnings("unused")
	private FlexPerfCalculator(){} // not used
	
	public FlexPerfCalculator(FlexToFlexMapping mappingToOriginalModel, FlexCodec codecOriginalModel,
			CancellationRegion cancelationRegion, FlexSpecialNodes originalSpecialNodes) {
		this.mappingToOriginalModel = mappingToOriginalModel;
		this.codecOriginalModel = codecOriginalModel;
		this.cancelationRegion = cancelationRegion;
		this.specialNodes = originalSpecialNodes;
		this.unreliableCaseIndexes = new TreeSet<Integer>();
	}

	/**
	 * Add start of node instance
	 * 
	 * @param currentVertex
	 * @param binding
	 * @param timestamp
	 */
	public void addStartNodeInstance(FlexNode currentVertex, FlexBinding binding, Long longTimestamp) {
		// update canceled nodes frequency
		Set<FlexNode> canceledNodes = null;
		Set<Pair<FlexNode, FlexNode>> canceledPairs = null;
		if (cancelationRegion != null){
			canceledNodes = cancelationRegion.getNodeCancellationFor(currentVertex);
			canceledPairs  = cancelationRegion.get(currentVertex);
		}
		if (canceledNodes != null) {
			for (FlexNode node : canceledNodes) {
				Integer freq = canceledFrequency.get(node);
				if (freq == null) {
					canceledFrequency.put(node, 1);
				} else {
					canceledFrequency.put(node, freq + 1);
				}
				
				if (specialNodes.containsMultipleAtomicTasks(node)){ // only update if it is multiple atomic
					Pair<FlexNode, FlexNode> pair = new Pair<FlexNode, FlexNode>(node, node);
					Integer arcFreq = canceledArcFrequency.get(pair);
					if (arcFreq == null) {
						canceledArcFrequency.put(pair, 1);
					} else {
						canceledArcFrequency.put(pair, arcFreq + 1);
					}
				}
			}
		}

		
		if (canceledPairs != null) {
			for (Pair<FlexNode, FlexNode> pair : canceledPairs) {
				Integer freq = canceledArcFrequency.get(pair);
				if (freq == null) {
					canceledArcFrequency.put(pair, 1);
				} else {
					canceledArcFrequency.put(pair, freq + 1);
				}
			}
		}

		// continue with replay node 
		FlexNode currentReplayNode = codecReplayModel.decode(binding.getEncodedNode());

		involvedNodes.add(mappingToOriginalModel.get(currentReplayNode).getFirst());

		// update waiting and synchronization time for this node
		// find out its predecessors that finishes first and last
		if (longTimestamp > endCaseTime) {
			endCaseTime = longTimestamp;
		}

		// variables to later calculate the partial binding
		Long timeLastFinishedObligations = startCaseTime;
		Long timeFirstFinishedObligations = longTimestamp;

		Set<Short> encodedInputBinding = new HashSet<Short>();

		// this is start, hence focus on incoming input
		if ((binding.getEncodedInputBinding() != FlexCodec.BLANK)
				&& (binding.getEncodedInputBinding() != FlexCodec.EMPTYSET)) {
			// need to update moving time and possibly waiting time for places
			// get binding if it is available
			for (Short replayNode : codecReplayModel.getIOBindingsFor(binding.getEncodedInputBinding())) {
				// unless the node is a place, the predecessors should have timestamps 
				FlexNode replayNodePredecessor = codecReplayModel.decode(replayNode);
				Pair<FlexNode, FlexNode> pairToBeTaken = new Pair<FlexNode, FlexNode>(replayNodePredecessor,
						currentReplayNode);
				Queue<Long> timeQueue = obligationsReplayModel.get(pairToBeTaken);
				if ((timeQueue == null) || (timeQueue.isEmpty())) {
					if (timeQueue == null) {
						timeQueue = new LinkedList<Long>();
					}
					obligationsReplayModel.put(pairToBeTaken, timeQueue);

					// populate content of obligationsReplayModel with info from untimedObligationsReplayModel
					// it has to be in the untimed obligations
					// in addition, we have to set waiting time for a place
					Iterator<UntimedObligation> it = untimedObligationsReplayModel.iterator();
					while (it.hasNext()) {
						UntimedObligation untimedObligation = it.next();
						Set<Pair<FlexNode, FlexNode>> setObligations = untimedObligation.getSetObligations();
						if (setObligations.contains(pairToBeTaken)) {
							// obligations are identified
							// move information to obligations
							for (Pair<FlexNode, FlexNode> newObligation : setObligations) {
								Queue<Long> longQueue = obligationsReplayModel.get(newObligation);
								if (longQueue == null) {
									longQueue = new LinkedList<Long>();
									obligationsReplayModel.put(newObligation, longQueue);
								}
								longQueue.add(longTimestamp);
							}

							// update timeQueue
							timeQueue = obligationsReplayModel.get(pairToBeTaken);

							/**
							 * update waiting time for a place in the original
							 * model
							 */
							// construct binding in original model1
							// check which is the original model
							FlexBinding bindingOriginalModel = constructFlexBindingInOrigModel(
									untimedObligation.getCurrNodeReplay(), untimedObligation.getInputNodeBindingIO(),
									untimedObligation.getOutputNodeBindingIO());

							FlexBindingInstanceAccumulator accumulator = mapNodesToAccumulator
									.get(bindingOriginalModel);
							if (accumulator == null) {
								accumulator = new FlexBindingInstanceAccumulator();
								mapNodesToAccumulator.put(bindingOriginalModel, accumulator);
							}
							accumulator.addAllPerfMeasures(longTimestamp - untimedObligation.getStartObligationTime(),
									0, 0);
							it.remove();
							break;
						}
					}
				}

				assert (!timeQueue.isEmpty());

				// update move time in original model
				FlexNode nodePredecessorInOriginal = mappingToOriginalModel.get(replayNodePredecessor).getFirst();
				Pair<FlexNode, FlexNode> edgePair = new Pair<FlexNode, FlexNode>(nodePredecessorInOriginal,
						currentVertex);
				involvedEdges.add(edgePair);

				EdgeInstanceAccumulator edgeAccumulator = mapEdgeToAccumulator.get(edgePair);
				if (edgeAccumulator == null) {
					edgeAccumulator = new EdgeInstanceAccumulator();
					mapEdgeToAccumulator.put(edgePair, edgeAccumulator);
				}
				Long timePredecessorNode = timeQueue.peek();
				edgeAccumulator.addMoveTime(longTimestamp - timePredecessorNode); //TODO: fix this bug

				// add for node performance
				if (timePredecessorNode.compareTo(timeFirstFinishedObligations) < 0) {
					timeFirstFinishedObligations = timePredecessorNode;
				}

				if (timePredecessorNode.compareTo(timeLastFinishedObligations) > 0) {
					timeLastFinishedObligations = timePredecessorNode;
				}

				encodedInputBinding.add(codecOriginalModel.encode(nodePredecessorInOriginal));
			}
			;
		}

		// after handling move time, add to halfbindperformance
		Queue<PartialBindingPerformance> partialBindPerfQueue = startNodeInstances.get(currentVertex);
		if (partialBindPerfQueue == null) {
			partialBindPerfQueue = new LinkedList<PartialBindingPerformance>();
			startNodeInstances.put(currentVertex, partialBindPerfQueue);
		}

		PartialBindingPerformance newPartialBindingPerformance = null;
		if ((binding.getEncodedInputBinding() != FlexCodec.BLANK)
				&& (binding.getEncodedInputBinding() != FlexCodec.EMPTYSET)) {
			newPartialBindingPerformance = new PartialBindingPerformance(
					codecOriginalModel.getEncIOBindingFor(encodedInputBinding), longTimestamp,
					timeLastFinishedObligations - timeFirstFinishedObligations, longTimestamp
							- timeLastFinishedObligations);
		} else {
			newPartialBindingPerformance = new PartialBindingPerformance(binding.getEncodedInputBinding(),
					longTimestamp, timeLastFinishedObligations - timeFirstFinishedObligations, longTimestamp
							- timeLastFinishedObligations);
		}
		partialBindPerfQueue.add(newPartialBindingPerformance);
	}

	private Short getShortOfOriginalModelOfEncodedReplayNode(Short encodedNode) {
		return codecOriginalModel.encode(mappingToOriginalModel.get(codecReplayModel.decode(encodedNode)).getFirst());
	}

	private FlexBinding constructFlexBindingInOrigModel(FlexNode currNodeReplay, Short inputNodeBindingIO,
			Short outputNodeBindingIO) {
		FlexNode originalNode = mappingToOriginalModel.get(currNodeReplay).getFirst();

		// predecessor does not have to be encoded anymore. It is already encoded!
		Short outputEncoding = null;
		if ((outputNodeBindingIO.compareTo(FlexCodec.BLANK) == 0)
				|| (outputNodeBindingIO.compareTo(FlexCodec.EMPTYSET) == 0)) {
			outputEncoding = outputNodeBindingIO;
		} else {
			Set<Short> encodedSuccessorsOriginal = new HashSet<Short>();
			for (Short encodedSuccessor : codecReplayModel.getIOBindingsFor(outputNodeBindingIO)) {
				encodedSuccessorsOriginal.add(getShortOfOriginalModelOfEncodedReplayNode(encodedSuccessor));
			}
			outputEncoding = codecOriginalModel.getEncIOBindingFor(encodedSuccessorsOriginal);
		}

		FlexBinding binding = new FlexBinding(inputNodeBindingIO, codecOriginalModel.encode(originalNode),
				outputEncoding);
		assert (codecOriginalModel.getEncodedFlexBindingFor(binding) != null);

		return binding;
	}

	/**
	 * currentVertex is the original node
	 */
	public void addEndNodeInstance(FlexNode currentVertex, FlexBinding binding, Long currTime,
			FlexNode decodedReplayNode) {
		// construct flexBinding for this node instance
		PartialBindingPerformance partBinding = startNodeInstances.get(currentVertex).poll();
		FlexBinding bindingOrigModel = constructFlexBindingInOrigModel(decodedReplayNode,
				partBinding.getEncodedInputBinding(), binding.getEncodedOutputBinding());

		addToAccumulator(bindingOrigModel, currTime - partBinding.getStartTime(), partBinding.getWaitingTime(),
				partBinding.getSyncTime());
	}

	private void addToAccumulator(FlexBinding binding, long throughputTime, long waitingTime, long syncTime) {
		FlexBindingInstanceAccumulator accumulator = mapNodesToAccumulator.get(binding);
		if (accumulator == null) {
			accumulator = new FlexBindingInstanceAccumulator();
			mapNodesToAccumulator.put(binding, accumulator);
		}
		accumulator.addAllPerfMeasures(throughputTime, waitingTime, syncTime);
	}

	public FlexPerfRepInfo finalizePerfRepInfo() {
		Map<Short, Set<FlexBindingInstance>> nodeInstancesMap = new HashMap<Short, Set<FlexBindingInstance>>();

		// node instances and activity instances
		for (FlexBinding binding : mapNodesToAccumulator.keySet()) {
			FlexBindingInstanceAccumulator acc = mapNodesToAccumulator.get(binding);

			// special case for start node
			FlexBindingInstance bindingInstance = new FlexBindingInstance(
					codecOriginalModel.getEncodedFlexBindingFor(binding));

			bindingInstance.setFrequency(acc.getFrequency()); // set frequency

			// set sync time
			bindingInstance.setMaxSyncTime(acc.getMaxSyncTimeSpan());
			bindingInstance.setMinSyncTime(acc.getMinSyncTimeSpan());
			bindingInstance.setAvgSyncTime((double) acc.getSumSyncTimeSpan() / (double) acc.getFrequency());
			bindingInstance.setStdDevSyncTime(acc.getStdDevSyncTime());

			// set waiting time
			bindingInstance.setMaxWaitTime(acc.getMaxWaitTimeSpan());
			bindingInstance.setMinWaitTime(acc.getMinWaitTimeSpan());
			bindingInstance.setAvgWaitTime((double) acc.getSumWaitTimeSpan() / (double) acc.getFrequency());
			bindingInstance.setStdDevWaitTime(acc.getStdDevWaitTime());

			// set working time
			bindingInstance.setMaxWorkTime(acc.getMaxWorkTimeSpan());
			bindingInstance.setMinWorkTime(acc.getMinWorkTimeSpan());
			bindingInstance.setAvgWorkTime((double) acc.getSumWorkTimeSpan() / (double) acc.getFrequency());
			bindingInstance.setStdDevWorkTime(acc.getStdDevWorkTime());

			// set number of case this binding is involved in
			bindingInstance.incNumCaseInvolved();

			// put calculation on set of binding instance
			Set<FlexBindingInstance> setBindingInstance = null;
			if ((setBindingInstance = nodeInstancesMap.get(binding.getEncodedNode())) != null) {
				setBindingInstance.add(bindingInstance);
			} else {
				setBindingInstance = new HashSet<FlexBindingInstance>();
				setBindingInstance.add(bindingInstance);
				nodeInstancesMap.put(binding.getEncodedNode(), setBindingInstance);
			}

		}

		// edge instances
		Map<Pair<Short, Short>, EdgeInstance> edgeInstancesMap = new HashMap<Pair<Short, Short>, EdgeInstance>();
		for (Pair<FlexNode, FlexNode> pair : mapEdgeToAccumulator.keySet()) {
			EdgeInstanceAccumulator acc = mapEdgeToAccumulator.get(pair);

			EdgeInstance edgeInstance = new EdgeInstance();
			edgeInstance.setFrequency(acc.getFrequency());
			edgeInstance.setMaxMoveTime(acc.getMaxMoveTimeSpan());
			edgeInstance.setMinMoveTime(acc.getMinMoveTimeSpan());
			edgeInstance.setAvgMoveTime((double) acc.getSumMoveTimeSpan() / (double) acc.getFrequency());
			edgeInstance.setStdDevMoveTime(acc.getStdDevMoveTime());

			Pair<Short, Short> flexEdgeEncoded = new Pair<Short, Short>(codecOriginalModel.encode(pair.getFirst()),
					codecOriginalModel.encode(pair.getSecond()));
			edgeInstancesMap.put(flexEdgeEncoded, edgeInstance);
		}

		FlexPerfRepInfo flexPerfRepInfo = new FlexPerfRepInfo();
		flexPerfRepInfo.setNodeInstancesMap(nodeInstancesMap);
		flexPerfRepInfo.setEdgeInstancesMap(edgeInstancesMap);
		flexPerfRepInfo.setNodeCaseInvolvement(convertToShortMapping(caseInvolvementNode));
		flexPerfRepInfo.setCaseInstance(new CaseInstance(caseAcc));
		flexPerfRepInfo.setUnfinishedFrequency(convertToShortMapping(unfinishedFrequency));
		flexPerfRepInfo.setCancelationFrequency(convertToShortMapping(canceledFrequency));
		flexPerfRepInfo.setArcCancelationFrequency(canceledArcFrequency);
		flexPerfRepInfo.setUnreliableTraceIndexes(unreliableCaseIndexes);

		return flexPerfRepInfo;
	}

	// assume that the node in caseInvolvement is the original node
	private Map<Short, Integer> convertToShortMapping(Map<FlexNode, Integer> caseInvolvement) {
		Map<Short, Integer> shortMap = new HashMap<Short, Integer>();
		for (FlexNode vertex : caseInvolvement.keySet()) {
			shortMap.put(codecOriginalModel.encode(vertex), caseInvolvement.get(vertex));
		}
		return shortMap;
	}

	public void finishOneCase() {

		// clear all obligations
		obligationsReplayModel.clear(); // ignore all unfinished obligations		

		// clear caseInvolvementEdge
		for (Pair<FlexNode, FlexNode> key : involvedEdges) {
			EdgeInstanceAccumulator edgeAcc = mapEdgeToAccumulator.get(key);
			if (edgeAcc != null) {
				edgeAcc.incCaseInvolved();
			} else {
				edgeAcc = new EdgeInstanceAccumulator();
				edgeAcc.incCaseInvolved();
				mapEdgeToAccumulator.put(key, edgeAcc);
			}
		}
		involvedEdges.clear();

		// clear involved Node
		for (FlexNode node : involvedNodes) {
			if (caseInvolvementNode.get(node) != null) {
				caseInvolvementNode.put(node, caseInvolvementNode.get(node) + 1);
			} else {
				caseInvolvementNode.put(node, 1);
			}

		}
		involvedNodes.clear();

		// node instances that are unfinished
		for (FlexNode node : startNodeInstances.keySet()) {
			Integer value = unfinishedFrequency.get(node);
			if (value == null) {
				unfinishedFrequency.put(node, startNodeInstances.get(node).size());
			} else {
				unfinishedFrequency.put(node, value + startNodeInstances.get(node).size());
			}
			startNodeInstances.get(node).clear(); // clear node instances
		}

		// update time
		caseAcc.addCaseTime(endCaseTime - startCaseTime);
		startCaseTime = -1;
		endCaseTime = -1;
	}

	public void setFirstTimestampOfCase(Date extractTimestamp) {
		startCaseTime = extractTimestamp.getTime();
	}

	public void setCodecReplayModel(FlexCodec codecReplayModel) {
		this.codecReplayModel = codecReplayModel;
	}

	public void setMappingToOriginalModel(FlexToFlexMapping mappingToOriginalModel) {
		this.mappingToOriginalModel = mappingToOriginalModel;

	}

	public void setOriginalFlexToLifecycleMap(OriginalFlexToILifecycleMap originalFlexToILifecycleMap) {
		this.originalFlexToILifecycleMap = originalFlexToILifecycleMap;
	}

	public void addBinding(FlexBinding binding, Long longTimestamp) {
		assert (binding != null);

		// be aware of the lifecycle
		FlexNode decodedReplayNode = codecReplayModel.decode(binding.getEncodedNode());
		Pair<FlexNode, LifecycleTypes> pairOrigNodeLifecycle = mappingToOriginalModel.get(decodedReplayNode);

		ILifecycleTransition lifecycle = originalFlexToILifecycleMap.get(pairOrigNodeLifecycle.getFirst());
		if ((lifecycle == null) || (lifecycle.getStartLifecycle().equals(pairOrigNodeLifecycle.getSecond()))) {
			addStartNodeInstance(pairOrigNodeLifecycle.getFirst(), binding, longTimestamp);
		}

		if ((lifecycle == null) || (lifecycle.getEndLifecycle().equals(pairOrigNodeLifecycle.getSecond()))) {
			addEndNodeInstance(pairOrigNodeLifecycle.getFirst(), binding, longTimestamp, decodedReplayNode);
		}

		// only update obligations
		updateObligations(binding, longTimestamp);
	}

	private void updateObligations(FlexBinding binding, Long timestamp) {
		// also tell the calculator to which tasks it is synchronized with
		short inputBinding = binding.getEncodedInputBinding();
		short outputBinding = binding.getEncodedOutputBinding();

		// output binding is not important, because this is indicating the start of activity instance
		if ((inputBinding != FlexCodec.BLANK) && (inputBinding != FlexCodec.EMPTYSET)) {
			// remove necessary obligations
			removeObligations(codecReplayModel.decode(binding.getEncodedNode()),
					codecReplayModel.getIOBindingsFor(inputBinding));
		}

		if ((outputBinding != FlexCodec.BLANK) && (outputBinding != FlexCodec.EMPTYSET)) {
			addObligations(codecReplayModel.decode(binding.getEncodedNode()),
					codecReplayModel.getIOBindingsFor(outputBinding), timestamp);
		}
	}

	private void addObligations(FlexNode replayNode, Set<Short> enabledEncodedNodes, Long timestamp) {
		for (Short encodedNode : enabledEncodedNodes) {
			Pair<FlexNode, FlexNode> pair = new Pair<FlexNode, FlexNode>(replayNode,
					codecReplayModel.decode(encodedNode));
			Queue<Long> obligationsQueue = obligationsReplayModel.get(pair);
			if (obligationsQueue == null) {
				obligationsQueue = new LinkedList<Long>();
				obligationsReplayModel.put(pair, obligationsQueue);
			}
			obligationsQueue.add(timestamp);
		}
	}

	private Pair<Long, Long> removeObligations(FlexNode replayNode, Set<Short> synchronizedEnabledNodes) {
		Long earliestFinishPredecessor = Long.MAX_VALUE;
		Long latestFinishPredecessor = Long.MIN_VALUE;
		for (Short encodedNode : synchronizedEnabledNodes) {
			Pair<FlexNode, FlexNode> pair = new Pair<FlexNode, FlexNode>(codecReplayModel.decode(encodedNode),
					replayNode);

			Long value = obligationsReplayModel.get(pair).poll();
			if (earliestFinishPredecessor.compareTo(value) > 0) {
				earliestFinishPredecessor = value;
			}
			if (latestFinishPredecessor.compareTo(value) < 0) {
				latestFinishPredecessor = value;
			}
		}
		return new Pair<Long, Long>(earliestFinishPredecessor, latestFinishPredecessor);
	}

	public void addBindingInvi(FlexBinding binding) {
		assert (binding != null);

		// be aware of the lifecycle
		FlexNode currVertexReplay = codecReplayModel.decode(binding.getEncodedNode());
		Pair<FlexNode, LifecycleTypes> pairOrigNodeLifecycle = mappingToOriginalModel.get(currVertexReplay);

		// derive timestamp from latest predecessors
		switch (pairOrigNodeLifecycle.getSecond()) {
			case PLACE_END :
				// only need to update untimedObligations
				Set<Short> placeEncoded = codecReplayModel.getIOBindingsFor(binding.getEncodedInputBinding());

				assert (placeEncoded.size() == 1); // because can only be preceded by single place_start

				// create set of untimed obligations
				Set<Pair<FlexNode, FlexNode>> newUntimedObligations = new HashSet<Pair<FlexNode, FlexNode>>();
				for (Short successor : codecReplayModel.getIOBindingsFor(binding.getEncodedOutputBinding())) {
					newUntimedObligations.add(new Pair<FlexNode, FlexNode>(currVertexReplay, codecReplayModel
							.decode(successor)));
				}

				// for the only predecessor, check the partial binding performance
				FlexNode origNode = mappingToOriginalModel.get(currVertexReplay).getFirst();
				PartialBindingPerformance partialBinding = startNodeInstances.get(origNode).poll();

				UntimedObligation untimedObligation = new UntimedObligation(newUntimedObligations,
						partialBinding.getStartTime(), currVertexReplay, partialBinding.getEncodedInputBinding(),
						binding.getEncodedOutputBinding());

				this.untimedObligationsReplayModel.add(untimedObligation);
				break;
			default :
				// derive timestamps from the latest predecessor
				Short inputBinding = binding.getEncodedInputBinding();
				if (inputBinding.equals(FlexCodec.BLANK) || inputBinding.equals(FlexCodec.EMPTYSET)) {
					addBinding(binding, startCaseTime);
					break;
				}

				// find the latest predecessor
				Long latestPredecessorTime = null;
				Set<Pair<FlexNode, FlexNode>> untimestamppedPredReplay = new HashSet<Pair<FlexNode, FlexNode>>();
				for (Short encodedPredecessorReplay : codecReplayModel.getIOBindingsFor(binding
						.getEncodedInputBinding())) {
					Pair<FlexNode, FlexNode> predReplayObligation = new Pair<FlexNode, FlexNode>(
							codecReplayModel.decode(encodedPredecessorReplay), currVertexReplay);
					Queue<Long> longQueue = obligationsReplayModel.get(predReplayObligation);
					if ((longQueue == null) || (longQueue.isEmpty())) {
						untimestamppedPredReplay.add(predReplayObligation);
						continue;
					} else {
						if (latestPredecessorTime == null) {
							latestPredecessorTime = longQueue.peek();
						} else {
							if (latestPredecessorTime.compareTo(longQueue.peek()) < 0) {
								latestPredecessorTime = longQueue.peek();
							}
						}
					}
				}

				// in cases where the predecessor is only end place (which of course, have no timestamp yet), 
				// set the timestamp the same time as the start of the place
				assert (latestPredecessorTime != null);

				// this is the case where latest predecessor time is found
				// the predecessor has to be a place
				// set waiting time of all other predecessor places at least the same as the last predecessor of the node
				deriveForPredecessor: while (!untimestamppedPredReplay.isEmpty()) {
					Pair<FlexNode, FlexNode> pair = untimestamppedPredReplay.iterator().next();

					// find the predecessor place
					Iterator<UntimedObligation> it = untimedObligationsReplayModel.iterator();
					while (it.hasNext()) {
						UntimedObligation untimedObligation2 = it.next();
						if (untimedObligation2.getSetObligations().contains(pair)) {
							// give time information for the obligation. check the others
							for (Pair<FlexNode, FlexNode> sucDerivedTimePair : untimedObligation2.getSetObligations()) {
								Queue<Long> queueLong = obligationsReplayModel.get(sucDerivedTimePair);
								if (queueLong == null) {
									queueLong = new LinkedList<Long>();
									obligationsReplayModel.put(sucDerivedTimePair, queueLong);
								}
								queueLong.add(latestPredecessorTime);

								/**
								 * update waiting time for a place in the
								 * original model
								 */
								// construct binding in original model
								// check which is the original model
								FlexBinding bindingOriginalModel = constructFlexBindingInOrigModel(
										untimedObligation2.getCurrNodeReplay(),
										untimedObligation2.getInputNodeBindingIO(),
										untimedObligation2.getOutputNodeBindingIO());

								FlexBindingInstanceAccumulator accumulator = mapNodesToAccumulator
										.get(bindingOriginalModel);
								if (accumulator == null) {
									accumulator = new FlexBindingInstanceAccumulator();
									mapNodesToAccumulator.put(bindingOriginalModel, accumulator);
								}
								accumulator.addAllPerfMeasures(
										latestPredecessorTime - untimedObligation2.getStartObligationTime(), 0, 0);
								it.remove();

								// remove the one in untimestamppedPredReplay
								untimestamppedPredReplay.remove(pair);
							}
							it.remove();
							continue deriveForPredecessor;
						}
					}
				}
				addBinding(binding, latestPredecessorTime);
				break;
		}
	}

	/**
	 * @return the specialNodes
	 */
	public FlexSpecialNodes getSpecialNodes() {
		return specialNodes;
	}

	/**
	 * @param specialNodes the specialNodes to set
	 */
	public void setSpecialNodes(FlexSpecialNodes specialNodes) {
		this.specialNodes = specialNodes;
	}

	/**
	 * @return the cancelationRegion
	 */
	public CancellationRegion getCancelationRegion() {
		return cancelationRegion;
	}

	/**
	 * @param cancelationRegion the cancelationRegion to set
	 */
	public void setCancelationRegion(CancellationRegion cancelationRegion) {
		this.cancelationRegion = cancelationRegion;
	}

	/**
	 * @return the startNodeInstances
	 */
	public Map<FlexNode, Queue<PartialBindingPerformance>> getStartNodeInstances() {
		return startNodeInstances;
	}

	/**
	 * @param startNodeInstances the startNodeInstances to set
	 */
	public void setStartNodeInstances(Map<FlexNode, Queue<PartialBindingPerformance>> startNodeInstances) {
		this.startNodeInstances = startNodeInstances;
	}

	public void addUnreliableCaseIndexes(SyncReplayResult caseResult) {
		unreliableCaseIndexes.addAll(caseResult.getTraceIndex());
	}
	
	public SortedSet<Integer> getUnreliableCases(){
		return unreliableCaseIndexes;
	}
	
}
