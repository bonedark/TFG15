package org.processmining.plugins.astar.petrinet;

import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.plugins.astar.algorithm.MemoryEfficientAStarAlgorithm;
import org.processmining.plugins.astar.petrinet.impl.DijkstraTail;
import org.processmining.plugins.astar.petrinet.impl.PDelegate;
import org.processmining.plugins.astar.petrinet.impl.PHead;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayer.annotations.PNReplayAlgorithm;

@PNReplayAlgorithm
public class PrefixBasedPetrinetReplayer extends AbstractPetrinetReplayer<DijkstraTail, PDelegate> {

	public String toString() {
		return "Prefix based A* Cost-based Fitness, assuming at most " + Short.MAX_VALUE + " tokens in each place.";
	}

	protected PDelegate getDelegate(Petrinet net, XLog log, XEventClasses classes, TransEvClassMapping mapping,
			double delta, int threads) {
		return new PDelegate(net, log, classes, mapping, mapTrans2Cost, mapEvClass2Cost, delta, true);
	}

	protected double getMinBoundMoveModel(final PluginContext context, PetrinetGraph net, TransEvClassMapping mapping,
			final XEventClasses classes, final double delta, final int threads,
			final MemoryEfficientAStarAlgorithm<PHead, DijkstraTail> aStar) {
		// in prefix calculation, there should not be any lower bound
		return 0;
	}
}
