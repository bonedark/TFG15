package org.processmining.plugins.joosbuijs.blockminer.clustering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.util.Pair;

public class FuzzyCMedoidsClustering {

	//We keep track of event classes and their relationships
	private XEventClasses classes;
	private Map<Pair<XEventClass, XEventClass>, Integer> directFollowsDependencies;
	private Map<Pair<XEventClass, XEventClass>, Long> directFollowsTotalTimes;
	private Map<XEventClass, Long> totalTimeSinceTraceStart;

	//And some hard coded defaults
	//TODO create setters/getters?
	private int maximumDirectSuccession;
	private final static int MAXITER = 1000;
	private final static int STEPS = 100;
	//significance: how many decimals are taken into account for comparing if 2 values are eq.
	private final static int SIGNIFICANCE = 100;

	//Map<XEventClass, Set<XEventClass>> foundClusters = getFuzzyCMedoidClusters(context, clusters, FUZZIFIER);

	public FuzzyCMedoidsClustering(XLog log, XLogInfo summary) {
		classes = summary.getEventClasses();

		directFollowsDependencies = new HashMap<Pair<XEventClass, XEventClass>, Integer>();
		directFollowsTotalTimes = new HashMap<Pair<XEventClass, XEventClass>, Long>();
		totalTimeSinceTraceStart = new HashMap<XEventClass, Long>();

		int iter = 0;
		int max = 0;
		for (XTrace trace : log) {
			/*
			 * if (progress.isCancelled()) { return; }
			 */
			Date start = XTimeExtension.instance().extractTimestamp(trace.get(0));

			for (int i = 0; i < trace.size() - 1; i++) {
				iter++;
				if (iter == (summary.getNumberOfEvents() / STEPS)) {
					iter = 0;
					//					progress.inc();
				}
				XEventClass fromEvent = classes.getClassOf(trace.get(i));
				Date fromEventTime = XTimeExtension.instance().extractTimestamp(trace.get(i));

				XEventClass toEvent = classes.getClassOf(trace.get(i + 1));
				Date toEventTime = XTimeExtension.instance().extractTimestamp(trace.get(i + 1));

				Pair<XEventClass, XEventClass> pair = new Pair<XEventClass, XEventClass>(fromEvent, toEvent);

				// update direct successions dependencies
				int n = directFollowsDependencies.containsKey(pair) ? directFollowsDependencies.get(pair) : 0;
				directFollowsDependencies.put(pair, n + 1);
				max = Math.max(max, n + 1);

				// update direct successions dependencies
				long l = directFollowsTotalTimes.containsKey(pair) ? directFollowsTotalTimes.get(pair) : 0;
				long dif = (toEventTime != null ? toEventTime.getTime() : 0)
						- (fromEventTime != null ? fromEventTime.getTime() : 0);
				directFollowsTotalTimes.put(pair, l + dif);

				dif = (toEventTime != null ? toEventTime.getTime() : 0) - (start != null ? start.getTime() : 0);
				long old = (totalTimeSinceTraceStart.containsKey(toEvent) ? totalTimeSinceTraceStart.get(toEvent) : 0);
				totalTimeSinceTraceStart.put(toEvent, old + dif);
			}
		}
		maximumDirectSuccession = max;
	}

	/**
	 * Creates fuzzy clusters using a distance metric. <br>
	 * Follows the FCTMdd algorithm as presented in 'A Fuzzy Relative of the
	 * k-Medoids Algorith with Application to Web Document and Snippet
	 * Clustering' paper with some improvements/fixes and adaptations for
	 * process models.
	 * 
	 * @param nrClusters
	 *            Number of clusters to search for
	 * @param fuzzifier
	 * @return
	 */
	public Map<XEventClass, Set<XEventClass>> getFuzzyCMedoidClusters(int nrClusters, double fuzzifier) {
		Map<XEventClass, Set<XEventClass>> result;
		//		Progress progress = context.getProgress();

		Set<XEventClass> nonMedoids = new HashSet<XEventClass>(classes.size());
		nonMedoids.addAll(classes.getClasses());

		// If the number of clusters is greater or equal to the number
		// of event classes, then return each event class in its own cluster
		if (nrClusters >= nonMedoids.size()) {
			result = new HashMap<XEventClass, Set<XEventClass>>(nonMedoids.size());
			for (XEventClass eventClass : nonMedoids) {
				Set<XEventClass> cluster = new HashSet<XEventClass>(1);
				cluster.add(eventClass);
				result.put(eventClass, cluster);
			}
			return result;
		}
		result = new HashMap<XEventClass, Set<XEventClass>>(nrClusters);

		// Select c random medoids (the first k as returned by the iterator over
		// nonMedoids (in java 6 this is random, in java 5 it is not))
		Set<XEventClass> medoids;
		Set<XEventClass> newMedoids = new HashSet<XEventClass>(nrClusters);

		//Randomly select nonMedoids and put them in the medoids class
		int i = 0;
		do {
			Iterator<XEventClass> it = nonMedoids.iterator();
			XEventClass medoid = it.next();
			if (Math.random() < .75) {
				continue;
			}
			newMedoids.add(medoid);
			it.remove();
			i++;
		} while (i < nrClusters); //stop when we have medoids for each cluster

		/*
		 * In this part we assign event classes to medoids based on distances,
		 * compute new medoids and repeat (until no change or limit reached)
		 */

		Map<Pair<XEventClass, XEventClass>, Double> U;
		int iter = 0;
		int prog_inc = 0;
		do {
			iter++; //count the number of iterations so we can stop

			//shows progress every so many iterations
			/*-
			prog_inc++; 
			if (prog_inc == (MAXITER / STEPS)) {
				prog_inc = 0;
				//progress.inc();
			}/**/
			medoids = newMedoids;

			// Compute the U_{ij} values for the medoids
			U = computeUValues(medoids, fuzzifier);
			printUMatrix(nrClusters, medoids, U);

			// Compute the new medoids
			newMedoids = new HashSet<XEventClass>(nrClusters);
			//For each of the current medoids
			for (XEventClass medoid : medoids) {
				double min = Double.MAX_VALUE;
				XEventClass minMedoid = null;
				//For each combination of classes in the event log
				for (XEventClass eventClass_k : classes.getClasses()) {
					double sum = 0;
					for (XEventClass eventClass_j : classes.getClasses()) {
						//calculate similarity between 2 event classes
						double r_comp = getSimilarity(eventClass_k, eventClass_j);
						//invert for distance
						double r = 1.0 / r_comp;

						//add (U^m)*r to the sum for medoid candidate k
						sum += Math.pow(U.get(new Pair<XEventClass, XEventClass>(medoid, eventClass_j)), fuzzifier) * r;
					}
					//If the sum for medoid candidate k is lower than the minimum so far update the new minMedoid
					if ((sum < min) && !newMedoids.contains(eventClass_k)) {
						min = sum;
						minMedoid = eventClass_k;
					}
				}
				newMedoids.add(minMedoid); //add the new minimal medoid
			}
			//Stop as soon as we did not find any other medoids than before XOR if we tried enough times
		} while (!newMedoids.equals(medoids) && (iter < MAXITER));

		medoids = newMedoids; //use new medoids
		U = computeUValues(medoids, fuzzifier); //compute U values for these medoids

		// Initialize the clusters
		for (XEventClass medoid : medoids) {
			result.put(medoid, new TreeSet<XEventClass>());
		}

		/*
		 * Put all eventClasses in the cluster belonging to the medoid for which
		 * the probability of that eventClass belonging there is highest (there
		 * may be more!!!)
		 */
		for (XEventClass eventClass : classes.getClasses()) {

			// First, find the highest probability recorded for any of the medoids
			double max = Double.MIN_VALUE;
			for (XEventClass medoid : medoids) {
				double val = U.get(new Pair<XEventClass, XEventClass>(medoid, eventClass));
				if (val > max) {
					max = val;
				}
			}

			// Check all medoids and verify whether the probability is the same as 
			// the maximum probability (precision depends on significance)
			for (XEventClass medoid : medoids) {
				//Calculate distance between medoid and current event class
				double val = U.get(new Pair<XEventClass, XEventClass>(medoid, eventClass));
				if (Math.round(SIGNIFICANCE * val) == Math.round(SIGNIFICANCE * max)) {
					result.get(medoid).add(eventClass);
				}
			}
		}

		return result;
	}

	/**
	 * Computes the U values between the medoids and all event classes in the
	 * log
	 * 
	 * @param medoids
	 *            the central event classes for their clusters
	 * @param fuzzifier
	 *            Fuzzifier
	 * @return
	 */
	private Map<Pair<XEventClass, XEventClass>, Double> computeUValues(Collection<XEventClass> medoids, double fuzzifier) {
		HashMap<Pair<XEventClass, XEventClass>, Double> U = new HashMap<Pair<XEventClass, XEventClass>, Double>();
		//For each event class in the log
		for (XEventClass eventClass : classes.getClasses()) {
			double sum = 0;
			//for each medoid (1st run)
			for (XEventClass medoid_k : medoids) {
				//calculate the similarity
				double r_comp = getSimilarity(medoid_k, eventClass);
				//And then add this to the sum using (
				sum += Math.pow(r_comp, 1 / (fuzzifier - 1));
			}
			//sum is now the normalizing value to optain U values between 0 and 1
			
			//So, now we loop again through all medoids
			for (XEventClass medoid_i : medoids) {
				double r_comp = getSimilarity(medoid_i, eventClass);
				U.put(new Pair<XEventClass, XEventClass>(medoid_i, eventClass), Math.pow(r_comp, 1 / (fuzzifier - 1)) / sum);
			}
		}
		return U;
	}

	private void printUMatrix(int c, Collection<XEventClass> medoids, Map<Pair<XEventClass, XEventClass>, Double> U) {
		ArrayList<XEventClass> cl = new ArrayList<XEventClass>(classes.getClasses());
		String[][] report = new String[c + 1][cl.size() + 1];
		int i = 1;
		for (XEventClass medoid : medoids) {
			report[i][0] = medoid.toString();
			int j = 1;
			for (XEventClass eventClass : cl) {
				report[i][j] = U.get(new Pair<XEventClass, XEventClass>(medoid, eventClass)).toString();
				j++;
			}
			i++;
		}
		int j = 1;
		for (XEventClass eventClass : cl) {
			report[0][j] = eventClass.toString();
			j++;
		}

		System.out.println("Matrix:");
		for (i = 0; i < report.length; i++) {
			System.out.println(Arrays.toString(report[i]));
		}
	}
	
	/**
	 * Calculates similarity between 2 event classes based on directly following relationships
	 * 
	 * @param first
	 * @param second
	 * @return
	 */
	private double getSimilarity(XEventClass first, XEventClass second) {
		if (first.equals(second)) {
			return 1;
		}
		Pair<XEventClass, XEventClass> p1 = new Pair<XEventClass, XEventClass>(first, second);
		Pair<XEventClass, XEventClass> p2 = new Pair<XEventClass, XEventClass>(second, first);
		int r1 = directFollowsDependencies.containsKey(p1) ? directFollowsDependencies.get(p1) : 0;
		int r2 = directFollowsDependencies.containsKey(p2) ? directFollowsDependencies.get(p2) : 0;
		return (double) (r1 + r2 + 1) / (double) (2 * maximumDirectSuccession + 1);
	}

}
