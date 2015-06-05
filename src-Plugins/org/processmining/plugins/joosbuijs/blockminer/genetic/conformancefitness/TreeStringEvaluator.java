package org.processmining.plugins.joosbuijs.blockminer.genetic.conformancefitness;
import java.util.List;

import org.uncommons.watchmaker.framework.FitnessEvaluator;

public class TreeStringEvaluator implements FitnessEvaluator<Tree> {
	private final String targetString = "SEQ(A+start,SEQ(A+complete,"
			+ "XOR(SEQ(XOR(SEQ(B2+start,B2+complete),SEQ(SEQ(B1+start,B1+complete)))," + "SEQ(C+start,C+complete)))))";

	/**
	 * Assigns one "fitness point" for every character in the candidate String
	 * that matches the corresponding position in the target string.
	 */
	public double getFitness(Tree candidate, List<? extends Tree> population) {
		String string = candidate.toString();
		
		int matches = 0;
		for (int i = 0; i < string.length() && i < targetString.length(); i++) {
			if (string.charAt(i) == targetString.charAt(i)) {
				++matches;
			}
		}
		
		candidate.setFitness(new Fitness(matches, matches, matches, matches));
		
		return matches;
	}

	public boolean isNatural() {
		return true;
	}
}