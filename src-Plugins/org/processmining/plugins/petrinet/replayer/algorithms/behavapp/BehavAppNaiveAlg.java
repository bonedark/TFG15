/**
 * 
 */
package org.processmining.plugins.petrinet.replayer.algorithms.behavapp;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;

import org.deckfour.xes.classification.XEventClass;
import org.processmining.framework.plugin.Progress;
import org.processmining.plugins.petrinet.replayer.util.LogAutomatonNode;
import org.processmining.plugins.petrinet.replayer.util.codec.EncPNWSetFinalMarkings;
import org.processmining.plugins.petrinet.replayer.util.statespaces.CPNCostBasedTreeNodeEnc;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;
import org.processmining.plugins.replayer.util.StepTypes;

/**
 * @author aadrians
 * Oct 24, 2011
 *
 */
public class BehavAppNaiveAlg extends AbstractBehavAppAlg {

	public String toString() {
		return "Best-First Search Cost-based Behavioral Appropriateness";
	}
	
	public String getHTMLInfo() {
		return "<html>This is an algorithm to calculate behavioral appropriateness between a log and a Petri net. <br/><br/>"
				+ "Given a trace of a log and a Petri net (can also be reset/inhibitor net), this algorithm "
				+ "return a matching between the trace and an allowed firing sequence of the net with the "
				+ "least deviation cost using the Best-First Search technique. Cost is calculated based on "
				+ "the number of disagreement between the next allowed event class according to the model and "
				+ "the ones allowed according to the log. The firing sequence has to reach proper termination "
				+ "(possible final markings/dead markings) of the net. "
				+ "<br/> <br/> The cost of disagreement for each event class allowed according to the log, "
				+ "but not according to the model can be weighed by the event class' frequency. The other way "
				+ "around, the cost for each event class allowed according to the model, but not according to "
				+ "the log can be configured per event-class. </html>";
	}

	public SyncReplayResult replayLoop(List<XEventClass> lstEvtClass, EncPNWSetFinalMarkings encodedPN,
			Map<Integer, Map<Integer, Integer>> mapArc2Weight, Map<Integer, Map<Integer, Integer>> mapInt2Marking,
			Map<Map<Integer, Integer>, Integer> mapMarking2Int, Map<Integer, LogAutomatonNode> mapInt2Automaton,
			Map<Integer, XEventClass> mapEncTrans2EvClass, boolean useLogWeight,
			Map<XEventClass, Integer> encActivityWeight, Random numGenerator, Integer maxNumStates,
			Map<List<XEventClass>, Set<Integer>> traceClass, Set<Integer> encInvisTransition,
			Set<Integer> encFinalMarkings, Map<Integer, Integer> mapMarking2EqMarkingClass,
			Map<Integer, Set<XEventClass>> mapMarking2EnabledEvClass,
			Map<Integer, SortedSet<Integer>> mapMarking2Enabled,
			Map<Integer, Map<Integer, Integer>> mapFiringTransitions, Progress progress) {
		// control variables
		int lstLength = lstEvtClass.size();

		// create tree 
		CPNCostBasedTreeNodeEnc stateSpaceRoot = new CPNCostBasedTreeNodeEnc(0, mapMarking2Int.get(encodedPN
				.getEncInitialMarking()), 0, null, null, 0, null);

		// explore state space
		int stateCounter = 1;
		CPNCostBasedTreeNodeEnc currStateSpaceNode = stateSpaceRoot;

		PriorityQueue<CPNCostBasedTreeNodeEnc> costBasedPNPQ = new PriorityQueue<CPNCostBasedTreeNodeEnc>();
		do {
			Integer nextAutomatonIndex = null;
			if (currStateSpaceNode.getCurrIndexOnTrace() < lstLength) {
				nextAutomatonIndex = getNextAutomatonIndex(
						mapInt2Automaton.get(currStateSpaceNode.getCurrIndexOnAutomaton()),
						lstEvtClass.get(currStateSpaceNode.getCurrIndexOnTrace()));

				// do move on log
				CPNCostBasedTreeNodeEnc mvOnLogStateSpace;

				if ((currStateSpaceNode.getCurrIndexOnTrace() + 1 == lstLength)
						&& (encFinalMarkings.contains(currStateSpaceNode.getCurrEncMarking()))) {
					mvOnLogStateSpace = new CPNCostBasedTreeNodeEnc(currStateSpaceNode.getCurrIndexOnTrace() + 1,
							currStateSpaceNode.getCurrEncMarking(), nextAutomatonIndex, StepTypes.L, null,
							currStateSpaceNode.getCurrCost(), currStateSpaceNode);
				} else {
					mvOnLogStateSpace = new CPNCostBasedTreeNodeEnc(currStateSpaceNode.getCurrIndexOnTrace() + 1,
							currStateSpaceNode.getCurrEncMarking(), nextAutomatonIndex, StepTypes.L, null,
							currStateSpaceNode.getCurrCost()
									+ getDeviationRatio(mapInt2Automaton.get(nextAutomatonIndex),
											mapMarking2EnabledEvClass.get(mapMarking2EqMarkingClass
													.get(currStateSpaceNode.getCurrEncMarking())), useLogWeight,
											encActivityWeight), currStateSpaceNode);
				}

				costBasedPNPQ.add(mvOnLogStateSpace);
				stateCounter++;
			}

			// do move on model
			SortedSet<Integer> enabledTransitions = identifyEnabledTransitions(currStateSpaceNode.getCurrEncMarking(),
					mapInt2Marking, mapMarking2Enabled, encodedPN, mapArc2Weight);

			for (Integer trans : enabledTransitions) {
				// execute the enabled transitions and change current marking
				Integer newMarkingIndex = fireTransition(currStateSpaceNode.getCurrEncMarking(), trans, mapInt2Marking,
						mapMarking2Int, numGenerator, mapFiringTransitions, encodedPN, mapArc2Weight);

				CPNCostBasedTreeNodeEnc mvOnModelStateSpace = null;
				if (!encInvisTransition.contains(trans)) {
					// real execution, hence, the new marking is considered as a new class of marking
					Set<XEventClass> enabledActivities = getEnabledActAndUpdate(newMarkingIndex, mapInt2Marking,
							encodedPN, encInvisTransition, mapEncTrans2EvClass, mapMarking2EnabledEvClass,
							mapMarking2EqMarkingClass);

					// not invisible
					if ((currStateSpaceNode.getCurrIndexOnTrace() == lstLength)
							&& (encFinalMarkings.contains(newMarkingIndex))) {
						mvOnModelStateSpace = new CPNCostBasedTreeNodeEnc(currStateSpaceNode.getCurrIndexOnTrace(),
								newMarkingIndex, currStateSpaceNode.getCurrIndexOnAutomaton(), StepTypes.MREAL, trans,
								currStateSpaceNode.getCurrCost(), currStateSpaceNode);
					} else {
						mvOnModelStateSpace = new CPNCostBasedTreeNodeEnc(currStateSpaceNode.getCurrIndexOnTrace(),
								newMarkingIndex, currStateSpaceNode.getCurrIndexOnAutomaton(), StepTypes.MREAL, trans,
								currStateSpaceNode.getCurrCost()
										+ getDeviationRatio(
												mapInt2Automaton.get(currStateSpaceNode.getCurrIndexOnAutomaton()),
												enabledActivities, useLogWeight, encActivityWeight), currStateSpaceNode);
					}

					costBasedPNPQ.add(mvOnModelStateSpace);
					stateCounter++;

					// check for move synchronously 
					if (nextAutomatonIndex != null) {
						XEventClass evClass4Trans = mapEncTrans2EvClass.get(trans);
						if (evClass4Trans != null) {
							if (evClass4Trans.equals(mapInt2Automaton.get(nextAutomatonIndex).getEventClass())) {
								CPNCostBasedTreeNodeEnc mvSync;
								if ((currStateSpaceNode.getCurrIndexOnTrace() == lstLength)
										&& (encFinalMarkings.contains(newMarkingIndex))) {
									mvSync = new CPNCostBasedTreeNodeEnc(currStateSpaceNode.getCurrIndexOnTrace() + 1,
											newMarkingIndex, nextAutomatonIndex, StepTypes.LMGOOD, trans,
											currStateSpaceNode.getCurrCost(), currStateSpaceNode);
								} else {
									mvSync = new CPNCostBasedTreeNodeEnc(currStateSpaceNode.getCurrIndexOnTrace() + 1,
											newMarkingIndex, nextAutomatonIndex, StepTypes.LMGOOD, trans,
											currStateSpaceNode.getCurrCost()
													+ getDeviationRatio(mapInt2Automaton.get(nextAutomatonIndex),
															enabledActivities, useLogWeight, encActivityWeight),
											currStateSpaceNode);
								}
								costBasedPNPQ.add(mvSync);
								stateCounter++;
							}
						}
					}

				} else {
					// the class of marking is the same as the previous marking (because only move on model invisible) 
					mapMarking2EqMarkingClass.put(newMarkingIndex,
							mapMarking2EqMarkingClass.get(currStateSpaceNode.getCurrEncMarking()));

					// invisible
					mvOnModelStateSpace = new CPNCostBasedTreeNodeEnc(currStateSpaceNode.getCurrIndexOnTrace(),
							newMarkingIndex, currStateSpaceNode.getCurrIndexOnAutomaton(), StepTypes.MINVI, trans,
							currStateSpaceNode.getCurrCost(), currStateSpaceNode);

					costBasedPNPQ.add(mvOnModelStateSpace);
					stateCounter++;
				}
			}

			// continue to the next node
			currStateSpaceNode = costBasedPNPQ.poll();
		} while ((currStateSpaceNode != null)
				&& ((!isEndOfModel(currStateSpaceNode.getCurrEncMarking(), encFinalMarkings, encodedPN, mapInt2Marking,
						mapMarking2Enabled, mapArc2Weight)) || (currStateSpaceNode
						.getCurrIndexOnTrace() < lstLength)) && (maxNumStates > stateCounter));

		// calculate behavioral appropriateness
		double behavAppropriateness = 1.0000;

		List<Object> nodeInstanceLst = new LinkedList<Object>();
		List<StepTypes> stepTypesLst = new LinkedList<StepTypes>();

		if (currStateSpaceNode == null) {
			// no termination is reached
			behavAppropriateness = 0.0000;

		} else {
			// add the precision measurement of the initial state
			// note that the precision of the last step must be 0 (because both log and model do not allow further move
			double precisionValue = getDeviationRatio(mapInt2Automaton.get(0),
					mapMarking2EnabledEvClass.get(mapMarking2EqMarkingClass.get(mapMarking2Int.get(encodedPN
							.getEncInitialMarking()))), useLogWeight, encActivityWeight)
					+ currStateSpaceNode.getCost();

			// print solution
			int numMatch = 0;
			while (currStateSpaceNode.getLatestStepType() != null) {
				numMatch++;
				switch (currStateSpaceNode.getLatestStepType()) {
					case LMGOOD :
						stepTypesLst.add(0, StepTypes.LMGOOD);
						nodeInstanceLst.add(0, encodedPN.getPetrinetNodeOf(currStateSpaceNode.getRelatedStepTypeObj()));
						break;

					case MINVI :
						stepTypesLst.add(0, StepTypes.MINVI);
						nodeInstanceLst.add(0, encodedPN.getPetrinetNodeOf(currStateSpaceNode.getRelatedStepTypeObj()));
						numMatch--;
						break;

					case MREAL :
						stepTypesLst.add(0, StepTypes.MREAL);
						nodeInstanceLst.add(0, encodedPN.getPetrinetNodeOf(currStateSpaceNode.getRelatedStepTypeObj()));
						break;

					case L :
						stepTypesLst.add(0, StepTypes.L);
						nodeInstanceLst.add(0, mapInt2Automaton.get(currStateSpaceNode.getCurrIndexOnAutomaton())
								.getEventClass().toString());
						break;

					default :
						break;
				}
				currStateSpaceNode = currStateSpaceNode.getParent();
			}

			// calculate behavioral appropriateness
			if (numMatch > 0) {
				behavAppropriateness = behavAppropriateness - (precisionValue / numMatch);
			}
		}

		// update average precision value
		sumAvgPrecisionValue += traceClass.get(lstEvtClass).size() * behavAppropriateness;

		// create SyncReplayResult
		Set<Integer> allEvtClass = traceClass.get(lstEvtClass);
		Iterator<Integer> it = allEvtClass.iterator();
		int tracePointer = it.next();
		SyncReplayResult repResult = new SyncReplayResult(nodeInstanceLst, stepTypesLst, tracePointer);
		while (it.hasNext()) {
			tracePointer = it.next();
			repResult.addNewCase(tracePointer);
		}
		repResult.addInfo(PNRepResult.BEHAVIORAPPROPRIATENESS, behavAppropriateness);
		repResult.addInfo(PNRepResult.NUMSTATEGENERATED, (double) stateCounter);

		// set reliability
		if (maxNumStates <= stateCounter) {
			repResult.setReliable(false);
		} else if (currStateSpaceNode == null) {
			repResult.setReliable(false);
		} else {
			repResult.setReliable(true);
		}

		return repResult;
	}

}