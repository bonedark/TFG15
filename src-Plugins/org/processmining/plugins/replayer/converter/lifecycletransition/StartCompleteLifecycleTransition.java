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
public class StartCompleteLifecycleTransition implements ILifecycleTransition {

	public LifecycleTypes getStartLifecycle() {
		return LifecycleTypes.START;
	}

	public LifecycleTypes getEndLifecycle() {
		return LifecycleTypes.COMPLETE;
	}

	public Set<LifecycleTypes> getNextLifecycle(LifecycleTypes query) {
		switch (query) {
			case START :
				Set<LifecycleTypes> res = new HashSet<LifecycleTypes>();
				res.add(LifecycleTypes.COMPLETE);
				return res;
			default :
				return null;
		}
	}

	public Set<LifecycleTypes> getAllLifecycle() {
		Set<LifecycleTypes> result = new HashSet<LifecycleTypes>();
		result.add(LifecycleTypes.START);
		result.add(LifecycleTypes.COMPLETE);
		return result;
	}

	@Override
	public String toString(){
		return "'Start-complete' lifecycle";
	}

	public Set<String> getAllLifecycleInString() {
		Set<String> res = new HashSet<String>();
		res.add(LifecycleTypes.START.toString());
		res.add(LifecycleTypes.COMPLETE.toString());
		return res;
	}
	
	public Set<LifecycleTypes> getPrevLifecycle(LifecycleTypes query) {
		Set<LifecycleTypes> res = new HashSet<LifecycleTypes>();
		switch (query){
			case COMPLETE : 
				res.add(LifecycleTypes.START);
				break;
			default: // do nothing
				break;
		}
		return res;
	}
}
