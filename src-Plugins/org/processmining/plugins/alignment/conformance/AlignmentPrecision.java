/**
 * 
 */
package org.processmining.plugins.alignment.conformance;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.connections.ConnectionManager;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.framework.util.Pair;
import org.processmining.models.connections.petrinets.EvClassLogPetrinetConnection;
import org.processmining.models.connections.petrinets.behavioral.InitialMarkingConnection;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Arc;
import org.processmining.models.graphbased.directed.petrinet.elements.InhibitorArc;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.ResetArc;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayer.util.LogAutomatonNodeWOID;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;

/**
 * @author aadrians Oct 13, 2011
 * 
 */
@Plugin(name = "Measure Conformance using Log-Model Alignment", returnLabels = { "Alignment-based Conformance Value" }, returnTypes = { AlignmentConformanceResult.class }, parameterLabels = {
		"Petri net (without Reset Arcs)", "Event Log", "Alignment" }, help = "Measure conformance using log-model alignments.", userAccessible = true)
public class AlignmentPrecision {
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "Arya Adriansyah", email = "a.adriansyah@tue.nl", pack = "Replayer")
	@PluginVariant(variantLabel = "From alignment", requiredParameterLabels = { 0, 1, 2 })
	public AlignmentConformanceResult measurePrecision(final UIPluginContext context, PetrinetGraph net, XLog log,
			PNRepResult alignment) {
		// retrieve mapping between process model to log
		try {
			ConnectionManager connManager = context.getConnectionManager();
			EvClassLogPetrinetConnection conn = connManager.getFirstConnection(EvClassLogPetrinetConnection.class,
					context, net, log);
			TransEvClassMapping mapping = (TransEvClassMapping) conn
					.getObjectWithRole(EvClassLogPetrinetConnection.TRANS2EVCLASSMAPPING);

			// get marking
			InitialMarkingConnection initMarkingConn = connManager.getFirstConnection(InitialMarkingConnection.class,
					context, net);
			Marking initMarking = initMarkingConn.getObjectWithRole(InitialMarkingConnection.MARKING);
			AlignmentConformanceResult result = measureConformanceAssumingCorrectAlignment(context, mapping, alignment,
					net, initMarking);
			return result;
		} catch (ConnectionCannotBeObtained noConnection) {
			context.log("No connection between the given net and log");
		}
		context.getFutureResult(0).cancel(true);
		return null;
	}

	/**
	 * Main method to measure precision, assuming valid alignment
	 * 
	 * @param context
	 * @param mapping
	 * @param alignment
	 * @param net
	 * @param initMarking
	 * @return
	 */
	public AlignmentConformanceResult measureConformanceAssumingCorrectAlignment(PluginContext context,
			TransEvClassMapping mapping, PNRepResult alignment, PetrinetGraph net, Marking initMarking) {
		// build automaton for alignment
		LogAutomatonNodeWOID root = new LogAutomatonNodeWOID(null);
		LogAutomatonNodeWOID pointer = root;
		int traceFreq = 0;

		// initiate map from marking to automaton of log 
		Map<Marking, Set<LogAutomatonNodeWOID>> mapMarking2Automaton = new HashMap<Marking, Set<LogAutomatonNodeWOID>>();
		Set<LogAutomatonNodeWOID> newSet = new HashSet<LogAutomatonNodeWOID>();
		newSet.add(root);
		mapMarking2Automaton.put(initMarking, newSet);

		for (SyncReplayResult repResult : alignment) {
			traceFreq = repResult.getTraceIndex().size();

			pointer = root;
			root.addFrequency(traceFreq);
			Marking latestMarking = new Marking(initMarking);
			for (Object nodeInstance : repResult.getNodeInstance()) {
				if (nodeInstance instanceof Transition) {
					// check if the transition is a part of successors
					pointer = pointer.getOrCreateChild((Transition) nodeInstance);
					pointer.addFrequency(traceFreq);

					// add mapping from marking to automaton with similar marking
					Marking newMarking = new Marking(latestMarking); // copy current marking
					updateMarkingByFiringTransition(net, newMarking, (Transition) nodeInstance);
					Set<LogAutomatonNodeWOID> addSet = mapMarking2Automaton.get(newMarking);
					if (addSet == null) {
						addSet = new HashSet<LogAutomatonNodeWOID>();
						mapMarking2Automaton.put(newMarking, addSet);
					}
					addSet.add(pointer); // map from new marking to new pointer

					// update latest marking
					latestMarking = newMarking;
				}
			}
		}

		// preprocess net
		Set<Transition> setInviTrans = new HashSet<Transition>();
		for (Transition t : net.getTransitions()) {
			if (t.isInvisible()) {
				setInviTrans.add(t);
			}
		}

		// calculate generalization based on automata (normalized with the number of automatons)
//		double generalizationBasedAutomata = 0.0000;
//		int consideredAutomaton = 0;
//		
//		Queue<Pair<Marking, LogAutomatonNodeWOID>> unexploredAutomaton = new LinkedList<Pair<Marking, LogAutomatonNodeWOID>>();
//		unexploredAutomaton.add(new Pair<Marking, LogAutomatonNodeWOID>(initMarking, root));
//		whileLoop : while (!unexploredAutomaton.isEmpty()) {
//			Pair<Marking, LogAutomatonNodeWOID> pair = unexploredAutomaton.poll();
//			
//			// if the transition is reached due to firing invisible transitions, no need to explore
//			if (pair.getSecond().getTransition() != null){
//				if (pair.getSecond().getTransition().isInvisible()){
//					continue whileLoop;
//				}
//			}
//
//			// increase the number of considered automaton
//			consideredAutomaton++;
//			
//			// reset result
//			enabledLogGen.clear();
//
//			// calculate enabled log for precision measurement
//			int[] totalFreqEnabledLogGen = calcEnabledLogTrans(mapping, enabledLogGen, pair.getSecond(), null);
//
//			// for generalization, also calculate similar states with the pointer
//			Set<LogAutomatonNodeWOID> automatonNodeSet = mapMarking2Automaton.get(pair.getFirst());
//			for (LogAutomatonNodeWOID automaton : automatonNodeSet) {
//				if (!automaton.equals(pointer)) {
//					int[] res = calcEnabledLogTrans(mapping, enabledLogGen, automaton, null);
//					totalFreqEnabledLogGen[0] += res[0];
//					totalFreqEnabledLogGen[1] += res[1];
//				}
//			}
//
//			generalizationBasedAutomata += totalFreqEnabledLogGen[0];
//
//		}

		// iterate through all syncrep result again
		double precisionRes = 0.0000;
		double generalizationRes = 0.0000;
		double generalizationResWOFreq = 0.0000;

		int totalEventNum = 0;

		// to calculate precision
		Set<XEventClass> enabledLogPrec = new HashSet<XEventClass>(1);
		Set<XEventClass> enabledModel = new HashSet<XEventClass>(1);

		// to calculate generalization
		Set<XEventClass> enabledLogGen = new HashSet<XEventClass>(1);

		for (SyncReplayResult repResult : alignment) {
			if (repResult.isReliable()) {
				pointer = root;
				Marking pointerMarking = new Marking(initMarking);

				traceFreq = repResult.getTraceIndex().size(); // generalization where frequency of a trace does matter
//				traceFreq = 1; // uncomment this line and comment the upper line to consider one trace only once

				for (Object nodeInstance : repResult.getNodeInstance()) {
					if (nodeInstance instanceof Transition) {
						Transition trans = (Transition) nodeInstance;
						if (trans.isInvisible()) {
							// ignore the transition, progress
							updateMarkingByFiringTransition(net, pointerMarking, trans);
							pointer = pointer.getSuccReferTo(trans);
						} else {
							// activity is found and was enabled in a state. Get the same activities 
							// that are also enabled by the same state (in model)
							int[] freqEnabledLogGen = calcEnabledLog4PrecGen(mapping, pointer, enabledLogPrec, net,
									pointerMarking, enabledLogGen, mapMarking2Automaton);
							calcEnabledModel(net, pointerMarking, mapping, setInviTrans, enabledModel);

							int sizeEnabledLogPrec = enabledLogPrec.size();
							int sizeEnabledLogGen = enabledLogGen.size();

							precisionRes += traceFreq * ((double) sizeEnabledLogPrec / (double) enabledModel.size());
							if (freqEnabledLogGen[0] >= (sizeEnabledLogGen + 2)) {
								generalizationRes += traceFreq
										* (((double) (sizeEnabledLogGen * (sizeEnabledLogGen + 1))) / ((double) (freqEnabledLogGen[0] * (freqEnabledLogGen[0] - 1))));
							} else {
								generalizationRes += traceFreq;
							}

							if (freqEnabledLogGen[1] >= (sizeEnabledLogGen + 2)) {
								generalizationResWOFreq += traceFreq
										* (((double) (sizeEnabledLogGen * (sizeEnabledLogGen + 1))) / ((double) (freqEnabledLogGen[1] * (freqEnabledLogGen[1] - 1))));
							} else {
								generalizationResWOFreq += traceFreq;
							}

							totalEventNum += traceFreq;

							updateMarkingByFiringTransition(net, pointerMarking, trans);
							pointer = pointer.getSuccReferTo(trans);

						}
					}
				}
			}
		}

		if (totalEventNum > 0) {
			AlignmentConformanceResult res = new AlignmentConformanceResult();
			res.setPrecision(precisionRes / totalEventNum);
			res.setGeneralization(1 - (generalizationRes / totalEventNum));
			res.setGeneralizationWOFreq(1 - (generalizationResWOFreq / totalEventNum));

			return res;
		} else {
			return new AlignmentConformanceResult();
		}
	}

	/**
	 * Get enabled activities that becomes the (indirect) successors
	 * 
	 * @param mapping
	 * @param pointer
	 * @param enabledLogPrec
	 * @param net
	 * @param markBeforeTF
	 * @param enabledLogGen
	 * @param totalFreqEnabledLogGen
	 * @param mapMarking2Automaton
	 */
	private int[] calcEnabledLog4PrecGen(TransEvClassMapping mapping, LogAutomatonNodeWOID pointer,
			Set<XEventClass> enabledLogPrec, PetrinetGraph net, Marking markBeforeTF, Set<XEventClass> enabledLogGen,
			Map<Marking, Set<LogAutomatonNodeWOID>> mapMarking2Automaton) {
		// reset result
		enabledLogPrec.clear();
		enabledLogGen.clear();

		// calculate enabled log for precision measurement
		int[] totalFreqEnabledLogGen = calcEnabledLogTrans(mapping, enabledLogPrec, pointer, null);

		// for generalization, also calculate similar states with the pointer
		Set<LogAutomatonNodeWOID> automatonNodeSet = mapMarking2Automaton.get(markBeforeTF);
		for (LogAutomatonNodeWOID automaton : automatonNodeSet) {
			if (!automaton.equals(pointer)) {
				int[] res = calcEnabledLogTrans(mapping, enabledLogGen, automaton, null);
				totalFreqEnabledLogGen[0] += res[0];
				totalFreqEnabledLogGen[1] += res[1];
			}
		}
		enabledLogGen.addAll(enabledLogPrec);
		return totalFreqEnabledLogGen;
	}

	private int[] calcEnabledLogTrans(TransEvClassMapping mapping, Set<XEventClass> enabledLog,
			LogAutomatonNodeWOID pointer, LogAutomatonNodeWOID object) {
		int[] totalFreqEnabledLog = new int[2];
		int res[] = moveForwardFindEnabledEvents(mapping, enabledLog, pointer, null);
		totalFreqEnabledLog[0] += res[0];
		totalFreqEnabledLog[1] += res[1];

		LogAutomatonNodeWOID parentPointer = pointer.getParent();
		LogAutomatonNodeWOID currPointer = pointer;
		if (parentPointer != null) {
			while ((parentPointer.getTransition() != null) && (currPointer.getTransition().isInvisible())) {
				res = moveForwardFindEnabledEvents(mapping, enabledLog, parentPointer, currPointer);
				totalFreqEnabledLog[0] += res[0];
				totalFreqEnabledLog[1] += res[1];

				currPointer = parentPointer;
				parentPointer = parentPointer.getParent();
			}
		}
		return totalFreqEnabledLog;
	}

	/**
	 * Move forward from the current state of log automaton. Finding enabled
	 * events and accumulate their frequencies
	 * 
	 * @param mapping
	 * @param res
	 * @param startNode
	 * @param exceptionNode
	 * @param totalFreqEnabledLog
	 */
	private int[] moveForwardFindEnabledEvents(TransEvClassMapping mapping, Set<XEventClass> res,
			LogAutomatonNodeWOID startNode, LogAutomatonNodeWOID exceptionNode) {
		int[] totalFreqEnabledLog = new int[2];
		Queue<LogAutomatonNodeWOID> queue = new LinkedList<LogAutomatonNodeWOID>();
		queue.add(startNode);
		while (!queue.isEmpty()) {
			LogAutomatonNodeWOID currNode = queue.poll();

			if (currNode.getSuccessors() != null) {
				for (LogAutomatonNodeWOID node : currNode.getSuccessors()) {
					if (!node.equals(exceptionNode)) {
						if (node.getTransition().isInvisible()) {
							queue.add(node);
						} else {
							XEventClass act = mapping.get(node.getTransition());
							if (act != null) {
								res.add(act);
								totalFreqEnabledLog[0] += node.getFrequency();
								totalFreqEnabledLog[1]++;
							}
						}
					}
				}
			}
		}
		return totalFreqEnabledLog;
	}

	/**
	 * Get all enabled activities from current marking
	 * 
	 * @param net
	 * @param calcMarking
	 * @param mapping
	 * @param setInviTrans
	 * @param res
	 * @return
	 */
	private void calcEnabledModel(PetrinetGraph net, Marking marking, TransEvClassMapping mapping,
			Set<Transition> setInviTrans, Set<XEventClass> res) {
		// reset result
		res.clear();

		// search all previous markings in which firing invisible transitions can reach this marking
		Queue<Pair<Marking, Marking>> mark2Check = new LinkedList<Pair<Marking, Marking>>();
		mark2Check.add(new Pair<Marking, Marking>(marking, null));
		while (mark2Check.peek() != null) {
			Pair<Marking, Marking> markToBeExpanded = mark2Check.poll();
			moveForwardFindEnableEventClass(net, markToBeExpanded.getFirst(), mapping, res,
					markToBeExpanded.getSecond());

			Set<Marking> mrkReverse = getMarkingReverseByInviTrans(net, markToBeExpanded.getFirst(), setInviTrans);
			if (mrkReverse != null) {
				for (Marking m : mrkReverse) {
					mark2Check.add(new Pair<Marking, Marking>(m, markToBeExpanded.getFirst()));
				}
			}
		}
	}

	/**
	 * get the set of markings that can be obtained by reverse-firing invisible
	 * transitions
	 * 
	 * @param net
	 * @param marking
	 * @param setInviTrans
	 * @return
	 */
	private Set<Marking> getMarkingReverseByInviTrans(PetrinetGraph net, Marking marking, Set<Transition> setInviTrans) {
		Set<Marking> res = null;

		// get reverse-enabled invi trans from marking
		inviIteration: for (Transition t : setInviTrans) {
			Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> colOutEdges = net.getOutEdges(t);
			if (colOutEdges != null) {
				for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : colOutEdges) {
					// no reset arcs. Hence, no need to check for one
					assert (!(edge instanceof ResetArc));
					if (marking.occurrences(edge.getTarget()) < net.getArc(edge.getSource(), edge.getTarget())
							.getWeight()) {
						continue inviIteration;
					}
				}

			}

			Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> colInEdges = net.getInEdges(t);
			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : colInEdges) {
				if (edge instanceof InhibitorArc) {
					if (marking.occurrences(edge.getSource()) > 0) {
						// can only be right if the transition return some tokens back
						Arc arc = net.getArc(t, edge.getSource());
						if (arc == null) {
							continue inviIteration;
						} else {
							if (marking.occurrences(edge.getSource()) != arc.getWeight()) {
								continue inviIteration;
							}
						}
					}
				}
			}

			// transition pass all edge requirement
			if (res == null) {
				res = new HashSet<Marking>(1);
			}
			res.add(getMarkingByUnFiringTransition(net, marking, t));
		}
		return res;
	}

	/**
	 * unfire a transition
	 * 
	 * @param net
	 * @param marking
	 * @param transition
	 * @return
	 */
	private Marking getMarkingByUnFiringTransition(PetrinetGraph net, Marking marking, Transition transition) {
		Marking newMarking = new Marking(marking);
		Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> colOutEdges = net
				.getOutEdges(transition);
		if (colOutEdges != null) {
			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : colOutEdges) {
				assert (!(edge instanceof ResetArc));
				newMarking.add((Place) edge.getTarget(), -1
						* net.getArc(edge.getSource(), edge.getTarget()).getWeight());
			}
		}

		Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> colInEdges = net
				.getInEdges(transition);
		if (colInEdges != null) {
			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : colInEdges) {
				if (!(edge instanceof InhibitorArc)) {
					newMarking
							.add((Place) edge.getSource(), net.getArc(edge.getSource(), edge.getTarget()).getWeight());
				}
			}
		}
		return newMarking;
	}

	/**
	 * find enable event class and stop compute if stopMarking is obtained
	 * 
	 * @param net
	 * @param marking
	 * @param mapping
	 * @param res
	 * @param stopMarking
	 */
	private void moveForwardFindEnableEventClass(PetrinetGraph net, Marking marking, TransEvClassMapping mapping,
			Set<XEventClass> res, Marking stopMarking) {
		Queue<Marking> queue = new LinkedList<Marking>();
		queue.add(marking);
		while (!queue.isEmpty()) {
			Marking currMarking = queue.poll();
			if (!currMarking.equals(stopMarking)) {
				Set<Transition> enabledTransitions = getEnabledTransitions(net, currMarking);
				if (enabledTransitions != null) {
					for (Transition trans : enabledTransitions) {
						if (trans.isInvisible()) {
							Marking newMarking = new Marking(currMarking);
							updateMarkingByFiringTransition(net, newMarking, trans);
							queue.add(newMarking);
						} else {
							XEventClass act = mapping.get(trans);
							if (act != null) {
								res.add(act);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * fire a transition, return its marking
	 * 
	 * @param net
	 * @param updatedMarking
	 * @param transition
	 * @return
	 */
	private void updateMarkingByFiringTransition(PetrinetGraph net, Marking updatedMarking, Transition transition) {
		Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> colInEdges = net
				.getInEdges(transition);
		if (colInEdges != null) {
			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : colInEdges) {
				if (!(edge instanceof InhibitorArc)) {
					updatedMarking.add((Place) edge.getSource(), -1
							* net.getArc(edge.getSource(), edge.getTarget()).getWeight());
				}
			}
		}

		Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> colOutEdges = net
				.getOutEdges(transition);
		if (colOutEdges != null) {
			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : colOutEdges) {
				assert (!(edge instanceof ResetArc));
				updatedMarking
						.add((Place) edge.getTarget(), net.getArc(edge.getSource(), edge.getTarget()).getWeight());
			}
		}
	}

	/**
	 * Return enabled transitions
	 * 
	 * @param net
	 * @param currMarking
	 * @return
	 */
	private Set<Transition> getEnabledTransitions(PetrinetGraph net, Marking marking) {
		// get continuation from marking
		Set<Transition> enabledTransitions = new HashSet<Transition>(3);
		for (Place place : marking.baseSet()) {
			Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> colOutEdges = net
					.getOutEdges(place);
			if (colOutEdges != null) {
				for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : colOutEdges) {
					assert (!(edge instanceof ResetArc));
					enabledTransitions.add((Transition) edge.getTarget());
				}
			}
		}
		// filter out enabledTransitions
		Iterator<Transition> it = enabledTransitions.iterator();
		iterateTransition: while (it.hasNext()) {
			Transition transition = it.next();

			Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> colInEdges = net
					.getInEdges(transition);
			if (colInEdges != null) {
				for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : colInEdges) {
					// consider inhibitor
					if (edge instanceof InhibitorArc) {
						if (marking.occurrences(edge.getSource()) > 0) {
							it.remove();
							continue iterateTransition;
						}
					} else {
						// must be ordinary edge
						Arc arc = net.getArc(edge.getSource(), edge.getTarget());
						if (marking.occurrences(edge.getSource()) < arc.getWeight()) {
							it.remove();
							continue iterateTransition;
						}
					}
				}
			}
		}
		return enabledTransitions;
	}

}
