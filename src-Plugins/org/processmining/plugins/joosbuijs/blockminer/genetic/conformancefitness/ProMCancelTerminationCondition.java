package org.processmining.plugins.joosbuijs.blockminer.genetic.conformancefitness;

import org.processmining.plugins.astar.algorithm.AStarThread.Canceller;
import org.uncommons.watchmaker.framework.PopulationData;
import org.uncommons.watchmaker.framework.TerminationCondition;

public class ProMCancelTerminationCondition implements TerminationCondition {
	private final Canceller canceller;

	public ProMCancelTerminationCondition(final Canceller canceller) {
		this.canceller = canceller;
	}

	public boolean shouldTerminate(PopulationData<?> populationData) {
		return canceller.isCancelled();
	}

}
