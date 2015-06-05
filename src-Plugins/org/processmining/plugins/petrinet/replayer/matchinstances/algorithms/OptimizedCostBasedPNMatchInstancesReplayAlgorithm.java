/**
 * 
 */
package org.processmining.plugins.petrinet.replayer.matchinstances.algorithms;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;
import org.deckfour.xes.classification.XEventClass;
import org.processmining.framework.plugin.Progress;
import org.processmining.framework.util.Pair;
import org.processmining.plugins.petrinet.replayer.util.codec.PNCodec;
import org.processmining.plugins.petrinet.replayer.util.statespaces.CPNCostBasedTreeNodeX;
import org.processmining.plugins.replayer.util.StepTypes;

/**
 * @author aadrians
 *
 */
public class OptimizedCostBasedPNMatchInstancesReplayAlgorithm {
	// accessor result
	public static int LISTSOFPAIR = 0;
	public static int ISRELIABLE = 1;

	// result of loopReplay
	private static int SOLUTIONNODES = 0;
	private static int RELIABILITY = 1;
	
	
	public static Object[] replayTraceInEncodedForm(Progress progress, List<XEventClass> listTrace,
			Map<XEventClass, List<Short>> transitionMapping, Set<Short> setInviTrans, PNCodec codec,
			Bag<Short> encInitMarking, int maxNumOfStates, int cstMoveOnLog, int cstMoveOnModelInvi,
			int cstMoveOnModelReal, boolean isAllowMoveOnLog, boolean isAllowMoveOnModelInvi,
			boolean isAllowMoveOnModelReal, PriorityQueue<CPNCostBasedTreeNodeX> pq) {
		pq.clear();

		CPNCostBasedTreeNodeX root = new CPNCostBasedTreeNodeX();
		root.setCurrMarking(encInitMarking);
		root.setCurrIndexOnTrace(0);
		root.setCost(0);
		pq.add(root);

		// only replay once, no reset
		Object[] loopResult = replayLoop(progress, listTrace, transitionMapping, setInviTrans, codec, encInitMarking,
				maxNumOfStates, cstMoveOnLog, cstMoveOnModelInvi, cstMoveOnModelReal, isAllowMoveOnLog, isAllowMoveOnModelInvi, isAllowMoveOnModelReal, 
				pq, 1);
		@SuppressWarnings("unchecked")
		Set<CPNCostBasedTreeNodeX> setSolutionNodes = (Set<CPNCostBasedTreeNodeX>) loopResult[SOLUTIONNODES];

		if (progress.isCancelled()) {
			return null;
		}
		
		List<List<Pair<StepTypes, Object>>> listsOfPair = new LinkedList<List<Pair<StepTypes,Object>>>();
		for (CPNCostBasedTreeNodeX currNode : setSolutionNodes){
			listsOfPair.add(createShortListFromTreeNode(encInitMarking, setInviTrans, currNode, codec, listTrace, transitionMapping));
		}

		return new Object[] {listsOfPair,loopResult[RELIABILITY] };
	}

	private static List<Pair<StepTypes, Object>> createShortListFromTreeNode(Bag<Short> encInitMarking,
			Set<Short> setInviTrans, CPNCostBasedTreeNodeX currNode, PNCodec codec, List<XEventClass> listTrace,
			Map<XEventClass, List<Short>> transitionMapping) {
		ListIterator<Pair<Integer, Short>> itDup = currNode.getDuplicatesOnlyStep().listIterator();
		ListIterator<Pair<Integer, Short>> itModOnly = currNode.getModelOnlyStep().listIterator();
		ListIterator<Integer> itTraceOnly = currNode.getMoveTraceOnlyStep().listIterator();

		List<Pair<StepTypes, Object>> res = new LinkedList<Pair<StepTypes, Object>>();

		int currIdx = 0;
		Pair<Integer, Short> currDup = itDup.hasNext() ? itDup.next() : null;
		Pair<Integer, Short> currModOnly = itModOnly.hasNext() ? itModOnly.next() : null;
		int currTraceOnly = itTraceOnly.hasNext() ? itTraceOnly.next() : Integer.MAX_VALUE;

		// replay token games
		Bag<Short> currMarking = new HashBag<Short>(encInitMarking);
		int currTraceIndex = 0;

		int resTraceSize = listTrace.size() + currNode.getModelOnlyStep().size();
		boolean loopFinish = false;

		while (currIdx < resTraceSize) {
			loopFinish = false;
			if (currDup != null) {
				if (currDup.getFirst() == currIdx) { // currently executing
					// duplicate
					Short executedTransition = currDup.getSecond();

					boolean isAppropriate = currMarking.containsAll(codec.getPredecessors(executedTransition));
					for (Short inh : codec.getInhibitors(executedTransition)) {
						if (currMarking.contains(inh)) {
							isAppropriate = false;
							break;
						}
					}

					if (isAppropriate) {
						// valid execution
						res.add(new Pair<StepTypes, Object>(StepTypes.LMGOOD, codec.decode(executedTransition)));
					} else { // invalid execution
						res.add(new Pair<StepTypes, Object>(StepTypes.LMNOGOOD, codec.decode(executedTransition)));
					}
					currMarking.removeAll(codec.getPredecessors(executedTransition));
					for (Short reset : codec.getResets(executedTransition)) {
						currMarking.remove(reset, currMarking.getCount(reset));
					}
					currMarking.addAll(codec.getSuccessors(executedTransition));
					loopFinish = true;
					currTraceIndex++;
					currDup = itDup.hasNext() ? itDup.next() : null;
				}
			}

			if ((currModOnly != null) && (!loopFinish)) {
				if (currModOnly.getFirst() == currIdx) {
					Short executedTransition = currModOnly.getSecond();
					if (setInviTrans.contains(executedTransition)) {
						res.add(new Pair<StepTypes, Object>(StepTypes.MINVI, codec.decode(executedTransition)));
					} else {
						res.add(new Pair<StepTypes, Object>(StepTypes.MREAL, codec.decode(executedTransition)));
					}
					currMarking.removeAll(codec.getPredecessors(executedTransition));
					for (Short reset : codec.getResets(executedTransition)) {
						currMarking.remove(reset, currMarking.getCount(reset));
					}
					currMarking.addAll(codec.getSuccessors(executedTransition));
					loopFinish = true;
					currModOnly = itModOnly.hasNext() ? itModOnly.next() : null;
				}
			}

			if (currTraceOnly == currIdx) {
				res.add(new Pair<StepTypes, Object>(StepTypes.L, listTrace.get(currTraceIndex)));
				loopFinish = true;
				currTraceIndex++;
				currTraceOnly = itTraceOnly.hasNext() ? itTraceOnly.next() : Integer.MAX_VALUE;
			}

			if (!loopFinish) {
				Short executedTransition = transitionMapping.get(listTrace.get(currTraceIndex)).get(0);

				boolean isAppropriate = currMarking.containsAll(codec.getPredecessors(executedTransition));
				for (Short inh : codec.getInhibitors(executedTransition)) {
					if (currMarking.contains(inh)) {
						isAppropriate = false;
						break;
					}
				}

				if (isAppropriate) {
					// valid execution
					res.add(new Pair<StepTypes, Object>(StepTypes.LMGOOD, codec.decode(executedTransition)));
				} else { // invalid execution
					res.add(new Pair<StepTypes, Object>(StepTypes.LMNOGOOD, codec.decode(executedTransition)));
				}
				currMarking.removeAll(codec.getPredecessors(executedTransition));
				for (Short reset : codec.getResets(executedTransition)) {
					currMarking.remove(reset, currMarking.getCount(reset));
				}
				currMarking.addAll(codec.getSuccessors(executedTransition));
				currTraceIndex++;
			}

			currIdx++;
		}
		return res;
	}

	private static Object[] replayLoop(Progress progress, List<XEventClass> listTrace,
			Map<XEventClass, List<Short>> transitionMapping, Set<Short> setInviTrans, PNCodec codec,
			Bag<Short> encInitMarking, int maxNumOfStates, int cstMoveOnLog, int cstMoveOnModelInvi,
			int cstMoveOnModelReal, boolean isAllowMoveOnLog, boolean isAllowMoveOnModelInvi,
			boolean isAllowMoveOnModelReal, PriorityQueue<CPNCostBasedTreeNodeX> pq, int id) {
		CPNCostBasedTreeNodeX currNode = pq.poll();
		int traceSize = listTrace.size();

		Integer solutionCost = null;
		Set<CPNCostBasedTreeNodeX> solutionNodes = new HashSet<CPNCostBasedTreeNodeX>();
		boolean isReliable = true;
		
		while ((currNode != null)&&((solutionCost == null) || (currNode.getCost() <= solutionCost)) && (id < maxNumOfStates) && (!progress.isCancelled())) {
			if (isAllowMoveOnLog) {
				// skip current event
				CPNCostBasedTreeNodeX nodeT = createNodeByMoveOnLog(currNode, cstMoveOnLog);
				id++;
				pq.add(nodeT);
			}

			// execute next event
			List<Short> candidateTrans = transitionMapping.get(listTrace.get(currNode.getCurrIndexOnTrace()));
			if (candidateTrans.size() > 1) {
				for (Short candidate : candidateTrans) {
					if (progress.isCancelled()) {
						return null;
					}
					// create new pnCostBasedTreeNode
					CPNCostBasedTreeNodeX nodeE = createNodeByMoveSynchronously(currNode, candidate, codec, traceSize, true);

					if (nodeE != null) {
						id++;
						pq.add(nodeE);
					}
				}
			} else {
				Short candidate = candidateTrans.get(0);
				CPNCostBasedTreeNodeX nodeE = createNodeByMoveSynchronously(currNode, candidate, codec, traceSize, false);
				if (nodeE != null) {
					id++;
					pq.add(nodeE);
				}
			}

			if (isAllowMoveOnModelInvi || isAllowMoveOnModelReal) {
				// execute any possible transitions (including both invisible and
				// real transitions)
				Set<CPNCostBasedTreeNodeX> additional = createNodeByMoveOnModel(currNode, codec,
						setInviTrans, cstMoveOnModelInvi, cstMoveOnModelReal, isAllowMoveOnModelInvi, isAllowMoveOnModelReal);
				for (CPNCostBasedTreeNodeX nodeM : additional) {
					id++;
					pq.add(nodeM);
				}
			}

			// continue to the next node
			currNode = pq.poll();
			while ((currNode.getCurrIndexOnTrace() == traceSize)&&(currNode != null)){
				if (solutionCost == null){ // check if solution had been found before
					solutionCost = currNode.getCost();
					solutionNodes.add(currNode);
					currNode = pq.poll();
				} else {
					if (solutionCost.compareTo(currNode.getCost()) >= 0){
						// another solution is found
						solutionNodes.add(currNode);
					}
					currNode = pq.poll();
				}
			}
		}
		
		if (id >= maxNumOfStates){
			isReliable = false;
		}
		
		return new Object[] { solutionNodes, isReliable};
	}

	private static Set<CPNCostBasedTreeNodeX> createNodeByMoveOnModel(CPNCostBasedTreeNodeX currNode, PNCodec codec,
			Set<Short> setInviTrans, int cstMoveOnModelInvi, int cstMoveOnModelReal, boolean isAllowMoveOnModelInvi,
			boolean isAllowMoveOnModelReal) {
		Set<Short> possiblyExecuted = getPossiblyExecutedTrans(currNode, codec);
		Set<CPNCostBasedTreeNodeX> res = new HashSet<CPNCostBasedTreeNodeX>(possiblyExecuted.size());
		for (Short possblyExec : possiblyExecuted) {
			if (setInviTrans.contains(possblyExec)) {
				if (!isAllowMoveOnModelInvi) {
					continue;
				}
			} else {
				if (!isAllowMoveOnModelReal) {
					continue;
				}
			}

			CPNCostBasedTreeNodeX newNode = new CPNCostBasedTreeNodeX(currNode);

			// marking
			Bag<Short> newMarking = newNode.getCurrMarking();
			newMarking.removeAll(codec.getPredecessors(possblyExec));
			for (Short reset : codec.getResets(possblyExec)) {
				newMarking.remove(reset, newMarking.getCount(reset));
			}
			newMarking.addAll(codec.getSuccessors(possblyExec));
			newNode.setCurrMarking(newMarking);

			// cost
			int cost = newNode.getCost();
			if (setInviTrans.contains(possblyExec)) {
				cost += cstMoveOnModelInvi;
			} else {
				cost += cstMoveOnModelReal;
			}

			newNode.setCost(cost);

			// move only
			List<Pair<Integer, Short>> listPair = newNode.getModelOnlyStep();
			listPair.add(new Pair<Integer, Short>(listPair.size() + currNode.getCurrIndexOnTrace(), possblyExec));
			newNode.setModelOnlyStep(listPair);

			res.add(newNode);
		}
		return res;
	}

	private static Set<Short> getPossiblyExecutedTrans(CPNCostBasedTreeNodeX currNode, PNCodec codec) {
		Set<Short> res = new HashSet<Short>();
		Bag<Short> currMarking = currNode.getCurrMarking();
		for (Short codedTrans : codec.getMapShortTrans().keySet()) {
			if (currMarking.containsAll(codec.getPredecessors(codedTrans))){
				res.add(codedTrans);
			};
		}
		return res;
	}
	
	private static CPNCostBasedTreeNodeX createNodeByMoveSynchronously(CPNCostBasedTreeNodeX currNode, Short candidate,
			PNCodec codec, int traceSize, boolean isDuplicate) {
		Set<Short> predecessors = codec.getPredecessors(candidate);
		
		if (currNode.getCurrMarking().containsAll(predecessors)){
			CPNCostBasedTreeNodeX res = new CPNCostBasedTreeNodeX(currNode);
			
			Bag<Short> newMarking = new HashBag<Short>(currNode.getCurrMarking());
			newMarking.removeAll(predecessors);
			newMarking.addAll(codec.getSuccessors(candidate));
			res.setCurrMarking(newMarking);				
			res.setCurrIndexOnTrace(currNode.getCurrIndexOnTrace() + 1);

			if (isDuplicate) {
				res.getDuplicatesOnlyStep().add(
						new Pair<Integer, Short>(currNode.getCurrIndexOnTrace() + currNode.getModelOnlyStep().size(),
								candidate));
			}
			res.setCost(currNode.getCost());

			return res;
		} else {
			return null;
		}
		
		
	}

	private static CPNCostBasedTreeNodeX createNodeByMoveOnLog(CPNCostBasedTreeNodeX currNode,
			int cstMoveOnLog) {
		CPNCostBasedTreeNodeX res = new CPNCostBasedTreeNodeX(currNode);
		res.setCurrIndexOnTrace(currNode.getCurrIndexOnTrace() + 1);
		res.getMoveTraceOnlyStep().add(currNode.getCurrIndexOnTrace() + currNode.getModelOnlyStep().size());

		// cost
		res.setCost((currNode.getCost() + cstMoveOnLog));
		
		return res;
	}

}
