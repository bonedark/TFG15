/**
 * 
 */
package org.processmining.plugins.petrinet.replayer.algorithms.costbasedcomplete;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.deckfour.xes.classification.XEventClass;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.petrinet.replayer.algorithms.AbstractDefaultPNReplayParam;

/**
 * @author aadrians Oct 21, 2011
 * 
 */
public class CostBasedCompleteParam extends AbstractDefaultPNReplayParam {
	private Map<XEventClass, Integer> mapEvClass2Cost = null;
	private Integer maxNumOfStates = null;
	private Map<Transition, Integer> mapTrans2Cost = null;
	private Marking initialMarking = null;
	private Marking[] finalMarkings = null;

	@SuppressWarnings("unused")
	private CostBasedCompleteParam() {
	}

	/**
	 * Constructor with default initialization. Cost of move on model: move on
	 * log = 2 : 5. If no dummy event class exist (i.e. an event class that does
	 * not exist in log, any transitions that are NOT silent and not mapped to
	 * any event class in the log is mapped to it), just put null
	 */
	public CostBasedCompleteParam(Collection<XEventClass> evClassCol, XEventClass dummyEvClass,
			Collection<Transition> transCol) {
		mapEvClass2Cost = new HashMap<XEventClass, Integer>();
		if (evClassCol != null) {
			for (XEventClass evClass : evClassCol) {
				mapEvClass2Cost.put(evClass, 5);
			}
		}
		if (dummyEvClass != null) {
			mapEvClass2Cost.put(dummyEvClass, 5);
		}
		maxNumOfStates = 200000;

		transCol = new HashSet<Transition>();
		if (transCol != null) {
			for (Transition t : transCol) {
				if (t.isInvisible()) {
					mapTrans2Cost.put(t, 0);
				} else {
					mapTrans2Cost.put(t, 2);
				}
			}
		}

		initialMarking = new Marking();
		finalMarkings = new Marking[0];
	}
	
	/**
	 * Constructor with default initialization. Cost of move on model: move on
	 * log can be adjusted. If no dummy event class exist (i.e. an event class that does
	 * not exist in log, any transitions that are NOT silent and not mapped to
	 * any event class in the log is mapped to it), just put null
	 */
	public CostBasedCompleteParam(Collection<XEventClass> evClassCol, XEventClass dummyEvClass,
			Collection<Transition> transCol, int defMoveOnModelCost, int defMoveOnLogCost) {
		if (evClassCol != null) {
			for (XEventClass evClass : evClassCol) {
				mapEvClass2Cost.put(evClass, defMoveOnLogCost);
			}
		}
		if (dummyEvClass != null) {
			mapEvClass2Cost.put(dummyEvClass, defMoveOnLogCost);
		}
		
		this.maxNumOfStates = 200000;

		transCol = new HashSet<Transition>();
		if (transCol != null) {
			for (Transition t : transCol) {
				if (t.isInvisible()) {
					mapTrans2Cost.put(t, 0);
				} else {
					mapTrans2Cost.put(t, defMoveOnModelCost);
				}
			}
		}
		this.initialMarking = new Marking();
		this.finalMarkings = new Marking[0];
	}

	/**
	 * Constructor with given cost mapping
	 */
	public CostBasedCompleteParam(Map<XEventClass, Integer> mapEvClass2Cost, Map<Transition, Integer> mapTrans2Cost) {
		this.mapEvClass2Cost = mapEvClass2Cost;
		this.maxNumOfStates = 200000;
		this.mapTrans2Cost = mapTrans2Cost;
		this.initialMarking = new Marking();
		this.finalMarkings = new Marking[0];
	}

	/**
	 * @return the initialMarking
	 */
	public Marking getInitialMarking() {
		return initialMarking;
	}

	/**
	 * @param initialMarking
	 *            the initialMarking to set
	 */
	public void setInitialMarking(Marking initialMarking) {
		this.initialMarking = initialMarking;
	}

	/**
	 * @return the finalMarkings
	 */
	public Marking[] getFinalMarkings() {
		return finalMarkings;
	}

	/**
	 * @param finalMarkings
	 *            the finalMarkings to set
	 */
	public void setFinalMarkings(Marking[] finalMarkings) {
		this.finalMarkings = finalMarkings;
	}

	/**
	 * 
	 * @return
	 */
	public Map<XEventClass, Integer> getMapEvClass2Cost() {
		return mapEvClass2Cost;
	}

	/**
	 * 
	 * @return
	 */
	public Integer getMaxNumOfStates() {
		return maxNumOfStates;
	}

	/**
	 * 
	 * @return
	 */
	public Map<Transition, Integer> getMapTrans2Cost() {
		return mapTrans2Cost;
	}

	/**
	 * @param mapEvClass2Cost
	 *            the mapEvClass2Cost to set
	 */
	public void setMapEvClass2Cost(Map<XEventClass, Integer> mapEvClass2Cost) {
		this.mapEvClass2Cost = mapEvClass2Cost;
	}

	/**
	 * @param maxNumOfStates
	 *            the maxNumOfStates to set
	 */
	public void setMaxNumOfStates(Integer maxNumOfStates) {
		this.maxNumOfStates = maxNumOfStates;
	}

	/**
	 * @param mapTrans2Cost
	 *            the mapTrans2Cost to set
	 */
	public void setMapTrans2Cost(Map<Transition, Integer> mapTrans2Cost) {
		this.mapTrans2Cost = mapTrans2Cost;
	}
}
