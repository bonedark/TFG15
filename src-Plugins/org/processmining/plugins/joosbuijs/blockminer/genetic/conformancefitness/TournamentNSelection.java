package org.processmining.plugins.joosbuijs.blockminer.genetic.conformancefitness;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.uncommons.watchmaker.framework.EvaluatedCandidate;
import org.uncommons.watchmaker.framework.SelectionStrategy;

public class TournamentNSelection implements SelectionStrategy<Object> {
	private final int N;

	private String description = "Tournament Selection where the fittest of N candidates is returned";

	/**
	 * Tournament selection that selects the fittest candidate out of N randomly
	 * selected
	 * 
	 * @param N
	 *            number of candidates to select the fittest from
	 */
	public TournamentNSelection(int N) {
		if (N < 1)
			throw new IllegalArgumentException("N must be at least 1.");
		this.N = N;
	}

	public <S> List<S> select(List<EvaluatedCandidate<S>> population, boolean naturalFitnessScores, int selectionSize,
			Random rng) {
		List<S> selection = new ArrayList<S>(selectionSize);
		for (int i = 0; i < selectionSize; i++) {
			//Initialize with the first candidate
			EvaluatedCandidate<S> fittestCandidate = population.get(rng.nextInt(population.size()));
			
			//Now select candidates 2..N and let the fittest survive
			for(int n = 1; n < N; n++)
			{
				EvaluatedCandidate<S> candidate = population.get(rng.nextInt(population.size()));
				if(candidate.getFitness() > fittestCandidate.getFitness())
					fittestCandidate = candidate;
			}
			
			selection.add(fittestCandidate.getCandidate());
		}
		return selection;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return description;
	}
}
