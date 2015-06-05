/**
 * 
 */
package org.processmining.plugins.replayer.converter.lifecycletransition;

import java.util.HashSet;
import java.util.Set;

import org.processmining.plugins.replayer.util.LifecycleTypes;

/**
 * @author aadrians
 * 
 */
public class OnlyCompleteLifecycleTransition implements ILifecycleTransition {

	public LifecycleTypes getStartLifecycle() {
		return LifecycleTypes.COMPLETE;
	}

	public LifecycleTypes getEndLifecycle() {
		return LifecycleTypes.COMPLETE;
	}

	public Set<LifecycleTypes> getNextLifecycle(LifecycleTypes query) {
		return null;
	}

	public Set<LifecycleTypes> getAllLifecycle() {
		Set<LifecycleTypes> result = new HashSet<LifecycleTypes>();
		result.add(LifecycleTypes.COMPLETE);
		return result;
	}

	@Override
	public String toString(){
		return "Only 'Complete' lifecycle";
	}

	public Set<String> getAllLifecycleInString() {
		Set<String> res = new HashSet<String>();
		res.add(LifecycleTypes.COMPLETE.toString());
		return res;
	}
	
	public Set<LifecycleTypes> getPrevLifecycle(LifecycleTypes query) {
		return new HashSet<LifecycleTypes>();
	}
}
