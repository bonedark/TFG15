/**
 * 
 */
package org.processmining.plugins.replayer.converter.lifecycletransition;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.processmining.plugins.replayer.util.LifecycleTypes;

/**
 * @author aadrians
 *
 */
public class PlaceLifecycleTransition implements ILifecycleTransition{
	public LifecycleTypes getStartLifecycle() {
		return LifecycleTypes.PLACE_START;
	}

	public LifecycleTypes getEndLifecycle() {
		return LifecycleTypes.PLACE_END;
	}

	public Set<LifecycleTypes> getNextLifecycle(LifecycleTypes query) {
		Set<LifecycleTypes> res = new HashSet<LifecycleTypes>();
		switch (query){
			case PLACE_START: 
				res.add(LifecycleTypes.PLACE_END);
				return res;
			default:
				return null;
		}
	}

	public Set<LifecycleTypes> getAllLifecycle() {
		Set<LifecycleTypes> result = new HashSet<LifecycleTypes>();
		result.add(LifecycleTypes.PLACE_START);
		result.add(LifecycleTypes.PLACE_END);
		return result;
	}
	
	@Override
	public String toString(){
		return "'Place' lifecycle";
	}

	public Set<String> getAllLifecycleInString() {
		Set<String> result = new TreeSet<String>();
		result.add(LifecycleTypes.PLACE_START.toString());
		result.add(LifecycleTypes.PLACE_END.toString());
		return result;
	}

	public Set<LifecycleTypes> getPrevLifecycle(LifecycleTypes query) {
		Set<LifecycleTypes> res = new HashSet<LifecycleTypes>();
		switch (query){
			case PLACE_END : 
				res.add(LifecycleTypes.PLACE_START);
				break;
			default: // do nothing
				break;
		}
		return res;
	}

}
