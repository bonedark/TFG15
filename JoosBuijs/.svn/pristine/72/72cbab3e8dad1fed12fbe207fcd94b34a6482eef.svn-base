package org.processmining.plugins.joosbuijs.blockminer.clustering;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.model.XLog;
import org.processmining.framework.util.Pair;
import org.processmining.plugins.log.logabstraction.BasicLogRelations;

public class ClusterRelationshipDiscovery {
	private Map<Pair<XEventClass, XEventClass>, Double> causalDependencies;
	private Map<Pair<XEventClass, XEventClass>, Double> parallelDependencies;
	private Map<Pair<XEventClass, XEventClass>, Double> longtermCausalDependencies;

	//If the causal relationship is above this threshold then it is sequential
	private final double SeqThreshold = 0.25;

	public enum RELATIONSHIPTYPE {
		XOR, AND, SEQFWD, SEQBWD
	};

	public ClusterRelationshipDiscovery(XLog eventLog) {
		//Create and remember causal and parallel dependencies
		BasicLogRelations logRelations = new BasicLogRelations(eventLog);
		causalDependencies = logRelations.getCausalDependencies();
		parallelDependencies = logRelations.getParallelRelations();
		longtermCausalDependencies = buildLongtermCausalDependencies(causalDependencies);
	}

	/**
	 * Returns the strongest relationship between the two given clusters
	 * 
	 * @param cluster1
	 * @param cluster2
	 * @return map of relationshiptypes to the UNNORMALIZED occurences
	 */
	public HashMap<RELATIONSHIPTYPE, Double> calculateRelationships(Set<XEventClass> cluster1, Set<XEventClass> cluster2) {
		HashMap<RELATIONSHIPTYPE, Double> relationships = new HashMap<RELATIONSHIPTYPE, Double>();
		//initiate
		relationships.put(RELATIONSHIPTYPE.XOR, 0.0);
		relationships.put(RELATIONSHIPTYPE.AND, 0.0);
		relationships.put(RELATIONSHIPTYPE.SEQFWD, 0.0);
		relationships.put(RELATIONSHIPTYPE.SEQBWD, 0.0);

		//Loop through all combinations of event classes in the two clusters
		Iterator<XEventClass> it1 = cluster1.iterator();
		while (it1.hasNext()) {
			XEventClass class1 = it1.next();
			Iterator<XEventClass> it2 = cluster2.iterator();
			while (it2.hasNext()) {
				XEventClass class2 = it2.next();

				Pair<XEventClass, XEventClass> pair = new Pair<XEventClass, XEventClass>(class1, class2);

				//Determine the relationship between these two classes
				//TODO take the weight into account!

				//Parallel and causal relationships are exclusive
				//First test for parallelism, its easier
				if (parallelDependencies.containsKey(pair)) {
					//If the pair is in a parallel relationship, add it to the cluster's parallel relationship
					relationships.put(RELATIONSHIPTYPE.AND, relationships.get(RELATIONSHIPTYPE.AND)
							+ parallelDependencies.get(pair));
				} else {
					/*
					 * If the pair is not in a parallel relationship, we must
					 * check for causal dependencies, in both directions
					 */
					Pair<XEventClass, XEventClass> opposed = new Pair<XEventClass, XEventClass>(class2, class1);

					/*
					 * If there is a strong causal relationship one way but not
					 * the other, then its sequential. Otherwise, its exclusive.
					 */
					//Set dependencies (the pair might not be present
					double fwdCausalDep = 0.0;
					if (longtermCausalDependencies.containsKey(pair))
						fwdCausalDep = longtermCausalDependencies.get(pair);
					double bwdCausalDep = 0.0;
					if (longtermCausalDependencies.containsKey(opposed))
						bwdCausalDep = longtermCausalDependencies.get(opposed);

					//Forward
					if (fwdCausalDep > SeqThreshold && bwdCausalDep < SeqThreshold) {
						relationships.put(RELATIONSHIPTYPE.SEQFWD, relationships.get(RELATIONSHIPTYPE.SEQFWD)
								+ fwdCausalDep);
					}
					//Backward
					else if (bwdCausalDep > SeqThreshold && fwdCausalDep < SeqThreshold) {
						relationships.put(RELATIONSHIPTYPE.SEQBWD, relationships.get(RELATIONSHIPTYPE.SEQBWD)
								+ bwdCausalDep);
					}
					//Exclusive!
					else {
						/*
						 * Note that we take the 'inverse' (e.g. 1 - causal)
						 * value!
						 */
						relationships.put(RELATIONSHIPTYPE.XOR, relationships.get(RELATIONSHIPTYPE.XOR)
								+ (1 - fwdCausalDep));
					}
				}

			}
		}
		return relationships;
	}

	/**
	 * Builds long term (e.g. AXB) causal relationships between activities
	 * 
	 * @param causalDependencies2
	 * 
	 * @param causalDependencies2
	 * @return
	 */
	private Map<Pair<XEventClass, XEventClass>, Double> buildLongtermCausalDependencies(
			Map<Pair<XEventClass, XEventClass>, Double> causalDependencies2) {
		Map<Pair<XEventClass, XEventClass>, Double> longterm = new HashMap<Pair<XEventClass, XEventClass>, Double>();

		longterm.putAll(causalDependencies2);

		boolean foundNew = true;
		while (foundNew) {
			foundNew = false;
			Map<Pair<XEventClass, XEventClass>, Double> newFound = new HashMap<Pair<XEventClass, XEventClass>, Double>();
			for (Map.Entry<Pair<XEventClass, XEventClass>, Double> entry1 : longterm.entrySet()) {
				for (Map.Entry<Pair<XEventClass, XEventClass>, Double> entry2 : longterm.entrySet()) {
					XEventClass entry1First = entry1.getKey().getFirst();
					XEventClass entry1Second = entry1.getKey().getSecond();
					XEventClass entry2First = entry2.getKey().getFirst();
					XEventClass entry2Second = entry2.getKey().getSecond();
					if (entry1Second == entry2First
							&& !longterm.containsKey(new Pair<XEventClass, XEventClass>(entry1First, entry2Second))) {
						//FIXME is average ok or do we need something else...?
						//Double value = (entry1.getValue() + entry2.getValue()) / 2; //avg
						Double value = (entry1.getValue() * entry2.getValue()); //miltiplication (to take distance into account)
						newFound.put(new Pair<XEventClass, XEventClass>(entry1First, entry2Second), value);
						foundNew = true;
					}
				}
			}
			//Now add the newly found relations to the longterm map
			longterm.putAll(newFound);
		}

		return longterm;
	}
}
