package org.processmining.plugins.astar.petrinet;

import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.astar.petrinet.impl.PILPDelegate;
import org.processmining.plugins.astar.petrinet.impl.PILPTail;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayer.algorithms.IPNReplayParameter;
import org.processmining.plugins.petrinet.replayer.algorithms.costbasedcomplete.CostBasedCompleteParam;
import org.processmining.plugins.petrinet.replayer.annotations.PNReplayAlgorithm;

@PNReplayAlgorithm
public class PetrinetReplayerWithILP extends AbstractPetrinetReplayer<PILPTail, PILPDelegate> {

	/**
	 * Return true if all replay inputs are correct
	 */
	public boolean isAllReqSatisfied(PluginContext context, PetrinetGraph net, XLog log, TransEvClassMapping mapping,
			IPNReplayParameter parameter) {
		if (super.isAllReqSatisfied(context, net, log, mapping, parameter)) {
			Marking[] finalMarking = ((CostBasedCompleteParam) parameter).getFinalMarkings();
			return ((finalMarking != null) && (finalMarking.length > 0));
		}
		return false;
	}

	public String toString() {
		return "A* Cost-based Fitness Express with MIPS, assuming at most " + Short.MAX_VALUE
				+ " tokens in each place.";
	}

	protected PILPDelegate getDelegate(Petrinet net, XLog log, XEventClasses classes, TransEvClassMapping mapping,
			double delta, int threads) {
		return new PILPDelegate(net, log, classes, mapping, //
				mapTrans2Cost, mapEvClass2Cost, delta, threads, finalMarkings);
	}

}
