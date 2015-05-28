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

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.processmining.framework.plugin.PluginContext;
import org.uncommons.watchmaker.framework.PopulationData;
import org.uncommons.watchmaker.framework.islands.IslandEvolutionObserver;

/**
 * Trivial evolution observer for displaying information at the end of each
 * generation.
 * 
 * @param <T>
 *            The type of entity being evolved.
 * 
 * @author jbuijs
 * @author Daniel Dyer (original)
 */
public class EvolutionLogger<T> implements IslandEvolutionObserver<T> {
	private final PluginContext context;

	public EvolutionLogger(final PluginContext context) {
		super();
		this.context = context;
	}

	public void populationUpdate(PopulationData<? extends T> data) {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		Node node = (Node) data.getBestCandidate();
		String bestCandadidateString = node.toString();
		System.out.println("Generation " + data.getGenerationNumber() + ": " + data.getBestCandidateFitness() + "("
				+ data.getMeanFitness() + " +/- " + data.getFitnessStandardDeviation() + ") - #"+data.getPopulationSize());
		System.out.println("  " + bestCandadidateString + ") - " + sdf.format(cal.getTime()));

		//And output to ProM
		context.getProgress().inc();
		context.getProgress().setCaption(
				"Generation " + data.getGenerationNumber() + ": " + data.getBestCandidateFitness());
	}

	public void islandPopulationUpdate(int islandNr, PopulationData<? extends T> data) {
		//Node node = (Node) data.getBestCandidate();
		//String bestCandadidateString = node.toString();
		System.out.println("  Island " + islandNr + ": " + data.getBestCandidateFitness() + "("
				+ data.getMeanFitness() + " +/- " + data.getFitnessStandardDeviation() + ")");
		//System.out.println("  " + bestCandadidateString + ") - " + sdf.format(cal.getTime()));		
	}
}
