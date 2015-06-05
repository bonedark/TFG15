package org.processmining.plugins.astar.petrinet;

import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.model.XLog;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.plugins.astar.petrinet.impl.PNaiveDelegate;
import org.processmining.plugins.astar.petrinet.impl.PNaiveTail;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayer.annotations.PNReplayAlgorithm;

@PNReplayAlgorithm
public class PetrinetReplayerWithoutILP extends AbstractPetrinetReplayer<PNaiveTail, PNaiveDelegate> {

	public String toString() {
		return "A* Cost-based Fitness Express, assuming at most " + Short.MAX_VALUE + " tokens in each place.";
	}
	
	protected PNaiveDelegate getDelegate(Petrinet net, XLog log, XEventClasses classes, TransEvClassMapping mapping,
			double delta, int threads) {
		return new PNaiveDelegate(net, log, classes, mapping, mapTrans2Cost, mapEvClass2Cost, delta, false);
	}

}
