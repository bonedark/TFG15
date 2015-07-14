/**
 * 
 */
package org.processmining.plugins.replayer.util;

import java.util.Map;

/**
 * @author aadrians
 *
 */
public class CostProvider {
	// name of parameter
	public static final int SKIPPINGDEF = 2;
	public static final int INSERTINGDEF = 5;
	public static final int UNOBSERVABLEDEF = 0;
	
	private boolean useGenericCost; // true if no distinction between activities
	
	// real value
	private int skippingEventCost = 0; 
	private int insertingEventCost = 0;
	private int unobservableEventCost = 0;
	
	// values (only if generalModel = false)
	private Map<Short, Integer> skippedEvents;
	private Map<Short, Integer> insertedEvents;
	private Map<Short, Integer> unobservableEvents;
	
	@SuppressWarnings("unused")
	private CostProvider(){};
	
	public CostProvider(boolean useGeneric){
		this.useGenericCost = useGeneric;
	}
	
	public int getCostForSkipping(short nodeID){
		if (useGenericCost){
			return skippingEventCost;
		} else {
			return skippedEvents.get(nodeID);
		}
	}
	
	public int getCostForInserting(short nodeID){
		if (useGenericCost){
			return insertingEventCost;
		} else {
			return insertedEvents.get(nodeID);
		}
	}
	
	public int getCostForUnobservable(short nodeID){
		if (useGenericCost){
			return unobservableEventCost;
		} else {
			return unobservableEvents.get(nodeID);
		}
	}

	/**
	 * @return the useGenericCost
	 */
	public boolean isUseGenericCost() {
		return useGenericCost;
	}

	/**
	 * @param useGenericCost the useGenericCost to set
	 */
	public void setUseGenericCost(boolean useGenericCost) {
		this.useGenericCost = useGenericCost;
	}

	/**
	 * @return the skippingEventCost
	 */
	public int getSkippingEventCost() {
		return skippingEventCost;
	}

	/**
	 * @param skippingEventCost the skippingEventCost to set
	 */
	public void setSkippingEventCost(int skippingEventCost) {
		this.skippingEventCost = skippingEventCost;
	}

	/**
	 * @return the insertingEventCost
	 */
	public int getInsertingEventCost() {
		return insertingEventCost;
	}

	/**
	 * @param insertingEventCost the insertingEventCost to set
	 */
	public void setInsertingEventCost(int insertingEventCost) {
		this.insertingEventCost = insertingEventCost;
	}

	/**
	 * @return the unobservableEventCost
	 */
	public int getUnobservableEventCost() {
		return unobservableEventCost;
	}

	/**
	 * @param unobservableEventCost the unobservableEventCost to set
	 */
	public void setUnobservableEventCost(int unobservableEventCost) {
		this.unobservableEventCost = unobservableEventCost;
	}

	/**
	 * @return the skippedEvents
	 */
	public Map<Short, Integer> getSkippedEvents() {
		return skippedEvents;
	}

	/**
	 * @param skippedEvents the skippedEvents to set
	 */
	public void setSkippedEvents(Map<Short, Integer> skippedEvents) {
		this.skippedEvents = skippedEvents;
	}

	/**
	 * @return the insertedEvents
	 */
	public Map<Short, Integer> getInsertedEvents() {
		return insertedEvents;
	}

	/**
	 * @param insertedEvents the insertedEvents to set
	 */
	public void setInsertedEvents(Map<Short, Integer> insertedEvents) {
		this.insertedEvents = insertedEvents;
	}

	/**
	 * @return the unobservableEvents
	 */
	public Map<Short, Integer> getUnobservableEvents() {
		return unobservableEvents;
	}

	/**
	 * @param unobservableEvents the unobservableEvents to set
	 */
	public void setUnobservableEvents(Map<Short, Integer> unobservableEvents) {
		this.unobservableEvents = unobservableEvents;
	}
	
	
	
	
}
