// =============================================================================
// Copyright 2006-2010 Daniel W. Dyer
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// =============================================================================
package org.processmining.plugins.joosbuijs.blockminer.genetic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.PluginContextID;
import org.processmining.framework.plugin.PluginExecutionResult;
import org.processmining.framework.plugin.PluginParameterBinding;
import org.processmining.framework.plugin.events.Logger;
import org.processmining.framework.util.Pair;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.petrinet.replayer.algorithms.BehavioralReplayAlgorithm;
import org.processmining.plugins.petrinet.replayer.algorithms.CostBasedPNReplayAlgorithm;
import org.processmining.plugins.petrinet.replayer.algorithms.IPNLogReplayAlgorithm;
import org.processmining.plugins.petrinet.replayer.ui.ParamSettingPNCostBasedReplay;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;
import org.uncommons.watchmaker.framework.FitnessEvaluator;

/**
 * Fitness function for the genetic discovery of block structured process
 * models.
 * 
 * @author jbuijs
 * @author Daniel Dyer (original)
 */
public class TreeEvaluator implements FitnessEvaluator<Node> {
	private final XLog log;
	private final PluginContext context;

	/**
	 * @param data
	 *            Each row is consists of a set of inputs and an expected output
	 *            (the last item in the row is the output).
	 */
	public TreeEvaluator(PluginContext context, XLog eventLog) {
		this.log = eventLog;
		this.context = context;
	}

	/**
	 * The fitness value of the candidate is calculated combining different
	 * notions of fitness: First there is the replay fitness of Arya. Second, we
	 * punish for leaving out event classes (which might be cheating to get a
	 * fitness of 1 according to the replay fitness), or duplication of event
	 * classes (this last one could be covered by precision in the future).
	 * Third (NOT IMPLEMENTED YET) we should take precision into account to
	 * prevent the flower equivalent block structured net.
	 * 
	 * @param candidate
	 *            The program tree to evaluate.
	 * @param population
	 *            Ignored by this implementation.
	 * @return The fitness score for the specified candidate.
	 */
	public double getFitness(Node candidate, List<? extends Node> population) {
		//We need a Petri Net to calculate replay fitness
		TreeToPNConvertor convertor = new TreeToPNConvertor();
		Petrinet net = convertor.buildPetrinet(candidate);

		//We don't have an initial marking but we can create one
		Marking initMarking = new Marking();
		for (Place place : net.getPlaces()) {
			//TODO improve by looking at the number of tokens?
			if (net.getInEdges(place).isEmpty()) {
				initMarking.add(place);
				//break;?
			}
		}

		//Build a LogPetrinetConnection
		XEventClassifier classifier = XLogInfoImpl.STANDARD_CLASSIFIER;
		XLogInfo logInfo = XLogInfoFactory.createLogInfo(log, classifier);
		XEventClasses classes = logInfo.getEventClasses(classifier);

		Collection<Pair<Transition, XEventClass>> mapping = new HashSet<Pair<Transition, XEventClass>>();
		for (Transition trans : net.getTransitions()) {
			//For each transition, find the event class with the exact same name
			mapping.add(new Pair<Transition, XEventClass>(trans, classes.getByIdentity(trans.getLabel())));
		}

		//Collection<Pair<Transition, XEventClass>> mapping 
		IPNLogReplayAlgorithm selectedAlg = new CostBasedPNReplayAlgorithm();

		//And the parameters
		// replay parameters
		Object[] parameters = new Object[15];

		parameters[ParamSettingPNCostBasedReplay.INAPPROPRIATETRANSFIRECOST] = 0;
		parameters[ParamSettingPNCostBasedReplay.ALLOWINAPPROPRIATEFIRING] = false;
		parameters[ParamSettingPNCostBasedReplay.REPLAYEDEVENTCOST] = 1;
		parameters[ParamSettingPNCostBasedReplay.HEURISTICDISTANCECOST] = 1;

		parameters[ParamSettingPNCostBasedReplay.SKIPPEDEVENTCOST] = 5;

		parameters[ParamSettingPNCostBasedReplay.ALLOWEVENTSKIP] = true;

		parameters[ParamSettingPNCostBasedReplay.INITIATIVEINVISTASKCOST] = 0;

		parameters[ParamSettingPNCostBasedReplay.ALLOWINVITASK] = true;

		parameters[ParamSettingPNCostBasedReplay.INITIATIVEREALTASKCOST] = 2;

		parameters[ParamSettingPNCostBasedReplay.ALLOWREALTASK] = true;

		parameters[ParamSettingPNCostBasedReplay.MAXEXPLOREDINSTANCESINTVAL] = 50000;

		parameters[ParamSettingPNCostBasedReplay.ALLOWTASKEXECWOTOKENS] = false;

		parameters[ParamSettingPNCostBasedReplay.ISTESTINGMODEINTVAL] = false;

		parameters[ParamSettingPNCostBasedReplay.FILELOCATIONSTRVAL] = "";
		parameters[ParamSettingPNCostBasedReplay.ANALYSISTYPEINTVAL] = -1;

		double replayFitness = callReplayPlugin(net, mapping, initMarking, new Marking(), selectedAlg, parameters,
				SyncReplayResult.FITNESS);

		//We can ask the replayer plugin using the `behavioral replay algorithm' 
		//and ask for 'BehavioralAppropriateness', see the 4 parameters in that class

		/*
		 * We will now ask for behavioral appropriateness
		 */

		//Collection<Pair<Transition, XEventClass>> mapping 
		IPNLogReplayAlgorithm behRepAlg = new BehavioralReplayAlgorithm();

		//And the parameters
		// replay parameters
		Object[] parametersBehRepAlg = new Object[4];

		parametersBehRepAlg[BehavioralReplayAlgorithm.MAXNUMSTATESINT] = 50000;
		parametersBehRepAlg[BehavioralReplayAlgorithm.USELOGWEIGHTBOOL] = true;
		parametersBehRepAlg[BehavioralReplayAlgorithm.MODELWEIGHTMAP] = null;
		parametersBehRepAlg[BehavioralReplayAlgorithm.LOG] = 3;

		double behavioralFitness = callReplayPlugin(net, mapping, initMarking, new Marking(), behRepAlg,
				parametersBehRepAlg, SyncReplayResult.BEHAVIORAPPROPRIATENESS);

		/*
		 * Now we need to calculate a precision metric since fitness and
		 * behavioral appropriateness are unrealistically high for low coverages
		 */

		HashSet<XEventClass> classesInTree = getEventClassesUnderNodeSet(candidate);

		//The number of different classes in the tree
		int nrClassesCovered = classesInTree.size();
		//The number of different classes in the event log
		int nrClasses = logInfo.getEventClasses().size();

		double punishment = (nrClassesCovered / (((candidate.countNodes() + 1) / 2) + (double) (nrClasses - nrClassesCovered)));

		//System.out.println(" " + replayFitness + "*" + behavioralFitness + "*" + punishment + " "
		//+ candidate.toString());

		/*
		 * Return the replay fitness times the behavioral appropriateness times
		 * the punishment for leaving out event classes
		 */
		return punishment * replayFitness * behavioralFitness;

	}

	/**
	 * Calls the provided selected algorithm in the Replayer package and
	 * calculates the requested fitness value
	 * 
	 * @param net
	 * @param mapping
	 * @param initMarking
	 * @param marking
	 * @param selectedAlg
	 * @param parameters
	 * @param fitness
	 * @return
	 */
	private double callReplayPlugin(Petrinet net, Collection<Pair<Transition, XEventClass>> mapping,
			Marking initMarking, Marking marking, IPNLogReplayAlgorithm selectedAlg, Object[] parameters, String fitness) {
		PNRepResult replayResult = null;
		//logReplayResult = context.tryToFindOrConstructFirstObject(PNRepResult.class, null, "", net, log,  mapping, initMarking, selectedAlg);

		//FIXME keep a (clean) list of currently running child contexts
		//FIXME for the progress listener cancellation: cycle through this list and send cancellation messages
		final PluginContext child = context.createChildContext("replay");
		context.getPluginLifeCycleEventListeners().firePluginCreated(child);
		child.getLoggingListeners().add(new Logger() {

			public void log(String message, PluginContextID contextID, MessageLevel messageLevel) {
				// ignore
			}

			public void log(Throwable t, PluginContextID contextID) {
				//ignore
				//context.log(t);
			}

		});

		//FIXME don't assume that there is a plugin (and what if there are more)
		//We don't use an init marking!
		Set<PluginParameterBinding> plugins = context
				.getPluginManager()
				.getPluginsAcceptingOrdered(PluginContext.class, false, net.getClass(), log.getClass(),
						mapping.getClass(), initMarking.getClass(), Marking.class, selectedAlg.getClass(),
						parameters.getClass());
		
		//We need the second... (could be better but its a quick fix anywho)
		Iterator<PluginParameterBinding> pIt = plugins.iterator();
		pIt.next();
		PluginParameterBinding plugin = pIt.next();
		

		PluginExecutionResult result = plugin.invoke(child, net, log, mapping, initMarking, new Marking(), selectedAlg,
				parameters);

		//FIXME correctly handle exceptions
		try {
			result.synchronize();
		} catch (CancellationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			//Lets see this candidate!
			return 1000;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		replayResult = result.getResult(0);

		//logReplayResult = replayer.replayLogPrivate(child, net, log, mapping, initMarking, selectedAlg, parameters);

		double returnValue = 0;

		if (replayResult != null) {
			double totalFitness = 0.0000;
			int numCases = 0;
			for (SyncReplayResult repRes : replayResult) {
				int size = repRes.getTraceIndex().size();
				totalFitness += (size * repRes.getInfo().get(fitness));
				numCases += size;
			}
			//We can now set the replay fitness value
			returnValue = totalFitness / numCases;
		} else {
			//There was an error during replay which might indicate something bad for the PN, just return a low fitness value...
			//FIXME what could we do here? returning a 'random' fitness value seems bad
			returnValue = 0.25d;
		}

		//make sure the return value is positive (we encountered a negative behavioral appropriateness value)
		if (returnValue < 0) {
			//What should the new value be??? Small I guess
			returnValue = 0.25;
		}

		return returnValue;
	}

	/**
	 * Method that returns a list of all event classes under a certain node
	 * 
	 * @param candidate
	 * @return
	 */
	private List<XEventClass> getEventClassesUnderNode(Node node) {
		ArrayList<XEventClass> list = new ArrayList<XEventClass>();

		if (node instanceof EventClassNode) {
			EventClassNode eventNode = (EventClassNode) node;
			list.add(eventNode.getEventClass());
		} else {
			list.addAll(getEventClassesUnderNode(node.getChild(0)));
			list.addAll(getEventClassesUnderNode(node.getChild(1)));
		}
		return list;
	}

	/**
	 * Method that returns a set of all event classes under a certain node
	 * 
	 * @param candidate
	 * @return
	 */
	private HashSet<XEventClass> getEventClassesUnderNodeSet(Node node) {
		HashSet<XEventClass> set = new HashSet<XEventClass>();

		if (node instanceof EventClassNode) {
			EventClassNode eventNode = (EventClassNode) node;
			set.add(eventNode.getEventClass());
		} else {
			set.addAll(getEventClassesUnderNode(node.getChild(0)));
			set.addAll(getEventClassesUnderNode(node.getChild(1)));
		}
		return set;
	}

	/**
	 * This fitness evaluator is a natural function. A fitness of one indicates
	 * a perfect solution.
	 * 
	 * @return true
	 */
	public boolean isNatural() {
		return true;
	}
}
