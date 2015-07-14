package org.processmining.plugins.joosbuijs.blockminer.genetic.conformancefitness;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.model.XLog;

/**
 * A class that calculates a (pessimistic) fitness value of an ~RPST on an event
 * log. See meeting notes 15-9-2011
 * 
 * @author jbuijs
 * 
 */
public class TreeFitness {

	//Internal recording of trace clusters including #traces that are in this cluster
	private HashMap<LinkedList<XEventClass>, Integer> traceClusters;

	public TreeFitness(XLog log) {
		traceClusters = buildTraceClusters(log);

	}

	private HashMap<LinkedList<XEventClass>, Integer> buildTraceClusters(XLog log) {
		//CALL some other existing function to get the trace clusters
		return null;
	}

	/**
	 * Calculates a pessimistic cost value of the fitness of the tree
	 * 
	 * @param tree
	 * @return
	 */
	public int calculateFitness(Tree tree) {
		//Detect duplicate activities in the tree (for escalation)
		HashSet<XEventClass> escalationSet = getEscalationSet(tree);

		//Loop through the trace clusters
		for (LinkedList<XEventClass> cluster : traceClusters.keySet()) {
			//Per cluster, walk through the tree, lowest level nodes first
			for (Node node : tree.getPostorder()) {
				if (node instanceof FunctionNode) {

					//Per block, try to parse the log projection and: (DEPENDING on control flow operator)
					//1) count the number of initializations of the block needed
					//2) count the number of missing activities in the trace (e.g. needed to reach correct marking in model)
					//3) escalate activities if you can BUT be sure to use at least one
					// Cost now is (#init-1) (for XOR) or (#init-1 + #init*#skippedAct-length) (for SEQ and AND)
				}
			}
		}

		return 0;
	}

	/**
	 * Returns those activities that appear twice in the tree for escalation
	 * during fitness calculation
	 * 
	 * @param tree
	 * @return
	 */
	private HashSet<XEventClass> getEscalationSet(Tree tree) {
		//Loop through the tree

		//Count occurrence of each node

		//Return those nodes that occur>1

		return null;
	}

	/**
	 * Returns a projection on a set of activities for the given trace
	 */
	private LinkedList<XEventClass> project(LinkedList<XEventClass> trace, HashSet<XEventClass> projectOn) {
		return trace;

	}
}
