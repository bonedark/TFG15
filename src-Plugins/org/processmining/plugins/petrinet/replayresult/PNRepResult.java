/**
 * 
 */
package org.processmining.plugins.petrinet.replayresult;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.processmining.plugins.replayer.replayresult.SyncReplayResult;

/**
 * @author aadrians
 * 
 */
public class PNRepResult extends TreeSet<SyncReplayResult> {
	private static final long serialVersionUID = -7708700759236806915L;
	
	private Map<String, Object> info = new HashMap<String, Object>(1);

	/**
	 * reference to information in SyncReplayResult
	 */
//	public static final String FITNESS = "Prefix Fitness"; // refer to TRACEFITNESS instead
	public static final String TRACEFITNESS = "Trace Fitness"; 
	// NOTE: TRACEFITNESS can be based on prefix, complete trace, depends on the algorithm
	
	
	public static final String BEHAVIORAPPROPRIATENESS = "Behavioral Appropriateness";
	public static final String MOVELOGFITNESS = "Move-Log Fitness";
	public static final String MOVEMODELFITNESS = "Move-Model Fitness";
	public static final String RAWFITNESSCOST = "Raw Fitness Cost";
	public static final String NUMSTATEGENERATED = "Num. States";
//	public static final String COMPLETEFITNESS = "Fitness"; // refer to TRACEFITNESS instead
	public static final String TIME = "Calculation Time (ms)";
	
	// additional
	public static final String ORIGTRACELENGTH = "Trace Length";
	
	/**
	 * Add information
	 * @param property
	 * @param valString
	 */
	public void addInfo(String property, String valString){
		info.put(property, valString);
	}
	
	/**
	 * @return the info
	 */
	public Map<String, Object> getInfo() {
		return info;
	}



	/**
	 * @param info the info to set
	 */
	public void setInfo(Map<String, Object> info) {
		this.info = info;
	}



	public PNRepResult(Collection<SyncReplayResult> col) {
		super(new Comparator<SyncReplayResult>() {
			public int compare(SyncReplayResult o1, SyncReplayResult o2) {
				SortedSet<Integer> s1 = o1.getTraceIndex();
				SortedSet<Integer> s2 = o2.getTraceIndex();
				if (o1.isReliable() && !o2.isReliable()) {
					return -1;
				}
				if (!o1.isReliable() && o2.isReliable()) {
					return 1;
				}
				if (s1.size() != s2.size()) {
					return s2.size() - s1.size();
				}
				if (o1.equals(o2)) {
					return 0;
				}
				if (o1.getStepTypes().size() != o2.getStepTypes().size()) {
					return o2.getStepTypes().size() - o1.getStepTypes().size();
				}
				Iterator<Integer> it1 = s1.iterator();
				Iterator<Integer> it2 = s2.iterator();
				while (it1.hasNext()) {
					Integer ss1 = it1.next();
					Integer ss2 = it2.next();
					if (!ss1.equals(ss2)) {
						return ss1.compareTo(ss2);
					}
				}
				return 0;
			}

		});
		addAll(col);
	}
}