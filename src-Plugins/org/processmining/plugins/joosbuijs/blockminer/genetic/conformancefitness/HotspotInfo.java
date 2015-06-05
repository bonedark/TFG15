package org.processmining.plugins.joosbuijs.blockminer.genetic.conformancefitness;

import java.util.HashMap;

import org.processmining.plugins.replayer.util.StepTypes;

/**
	 * Class that records information about a hotspot node
	 * 
	 * @author jbuijs
	 * 
	 */
	public class HotspotInfo {
//		private Pair<HashMap<StepTypes, Integer>, HashMap<Pair<Node, Node>, Integer>> info;
		//Occurrences of each step type
		private HashMap<StepTypes, Integer> moveCounts;

		/**
		 * Constructor for constructing empty hotspotinfo
		 */
		public HotspotInfo() {
			moveCounts = new HashMap<StepTypes, Integer>();
			moveCounts.put(StepTypes.LMGOOD, 0);//LMGOOD
			moveCounts.put(StepTypes.MREAL,0);//MREAL
			moveCounts.put(StepTypes.L,0);//L
		}
		
		/**
		 * Increase the steptype count with the provided value
		 * 
		 * @param steptype steptype to increase
		 * @param value amount to increase with
		 */
		public void addStepTypeCount(StepTypes steptype, int value){
			moveCounts.put(steptype, moveCounts.get(steptype)+value);
		}
		
		/**
		 * Returns a 'severity' value which can be used to select a hotspot to improve
		 * 
		 * @return
		 */
		public int getSeverity(){
			//FIXME implement
			return 0;
		}
	}