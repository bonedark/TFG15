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
package org.processmining.plugins.joosbuijs.blockminer.genetic.conformancefitness;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
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
 */
public class EvolutionLogger<T> implements IslandEvolutionObserver<T> {
	private boolean showOnConsole = false;
	private boolean fileLoggingEnabled = true;

	private final PluginContext context;
	private DecimalFormat df = new DecimalFormat("#.######");
	private Calendar cal;
	private int nrGenerations = 0;

	//Writing to a file
	private FileOutputStream fos;
	private PrintWriter out;

	public EvolutionLogger(final PluginContext context) {
		this(context, true);
	}

	public EvolutionLogger(final PluginContext context, boolean fileLoggingEnabled) {
		super();

		this.context = context;
		this.fileLoggingEnabled = fileLoggingEnabled;

		if (this.fileLoggingEnabled) {
			//File writing stuff
			try {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss");
				cal = Calendar.getInstance();
				String timestamp = sdf.format(cal.getTime());

				//Try FilePermission permission = new FilePermission("C:\\Users\\FSSD\\Desktop\\My Test", FilePermission.READ);

				String filename = "./stats/stats_" + timestamp + ".csv";
				File statsFile = new File(filename);

				//			statsFile.createNewFile();
				statsFile.setWritable(true);
				statsFile.setReadable(true);

				fos = new FileOutputStream(statsFile);
				out = new PrintWriter(fos);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

			//Write the header line
			out.println("Timestamp; Generation; Fittest; Average; Deviation; "
					+ "replayFitness; BehApp; Coverage; bestCandidate");
		}
	}

	public void disableFileLogging() {
		fileLoggingEnabled = false;
	}

	public void populationUpdate(PopulationData<? extends T> data) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		cal = Calendar.getInstance();

		Tree tree = (Tree) data.getBestCandidate();
		String bestCandadidateString = tree.toString();

		//Prepare values
		int generation = data.getGenerationNumber();
		double bestOverallFitness = data.getBestCandidateFitness();
		double meanFitness = data.getMeanFitness();
		double stddev = data.getFitnessStandardDeviation();
		Fitness fitness = tree.getFitness();

		double replayFitness = -1;
		double behavioralAppropriateness = -1;
		double coverage = -1;

		if (fitness != null) {
			replayFitness = fitness.getReplayFitness();
			behavioralAppropriateness = fitness.getBehavioralAppropriateness();
			coverage = fitness.getCoverage();
		}

		/*-*/
		if (showOnConsole) {
			System.out.println("Generation " + generation + ": " + df.format(bestOverallFitness) + "("
					+ df.format(meanFitness) + " +/- " + df.format(stddev) + ") - #" + data.getPopulationSize());
			System.out.println("  " + bestCandadidateString + ") - " + sdf.format(cal.getTime()));
		}
		/**/

		if (fileLoggingEnabled) {
			//And to a file
			out.println(sdf.format(cal.getTime()) + " ; " + generation + " ; " + df.format(bestOverallFitness) + " ; "
					+ df.format(meanFitness) + " ; " + df.format(stddev) + " ; " + df.format(replayFitness) + " ; "
					+ df.format(behavioralAppropriateness) + " ; " + df.format(coverage) + " ; "
					+ bestCandadidateString);
			out.flush();
		}

		//And output to ProM
		context.getProgress().inc();
		//context.log("Generation " + data.getGenerationNumber() + ": " + data.getBestCandidateFitness());

		//And update the nr generations needed
		setNrGenerations(generation);
	}

	public void islandPopulationUpdate(int islandNr, PopulationData<? extends T> data) {
		//Node node = (Node) data.getBestCandidate();
		//String bestCandadidateString = node.toString();
		if (showOnConsole){
			System.out.println("  Island " + islandNr + ": " + df.format(data.getBestCandidateFitness()) + "("
					+ df.format(data.getMeanFitness()) + " +/- " + df.format(data.getFitnessStandardDeviation())
					+ ") -#" + data.getPopulationSize());
		//System.out.println("  " + bestCandadidateString + ") - " + sdf.format(cal.getTime()));
		}
	}

	public void closeFile() {
		out.close();
	}

	private void setNrGenerations(int nrGenerations) {
		this.nrGenerations = nrGenerations;
	}

	public int getNrGenerations() {
		return nrGenerations;
	}
}
