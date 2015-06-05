/**
 * 
 */
package org.processmining.plugins.replayer.util;

/**
 * @author aadrians
 * 
 */
public enum LifecycleTypes {
	SCHEDULED, START, COMPLETE, SUSPEND, RESUME, NONE, PLACE_START, PLACE_END;

	@Override
	public String toString() {
		switch(this){
			case SCHEDULED: return "scheduled"; 
			case START: return "start"; 
			case COMPLETE: return "complete"; 
			case SUSPEND: return "suspend"; 
			case RESUME: return "resume"; 
			case NONE: return "none"; 
			case PLACE_START: return "place start"; 
			case PLACE_END: return "place end"; 
			default: return name();
		}
	}
}
