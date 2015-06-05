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
public class FullLifecycleTransition implements ILifecycleTransition{
	public LifecycleTypes getStartLifecycle() {
		return LifecycleTypes.START;
	}

	public LifecycleTypes getEndLifecycle() {
		return LifecycleTypes.COMPLETE;
	}

	public Set<LifecycleTypes> getNextLifecycle(LifecycleTypes query) {
		Set<LifecycleTypes> res = new HashSet<LifecycleTypes>();
		switch (query){
			case START : 
				res.add(LifecycleTypes.SUSPEND);
				res.add(LifecycleTypes.COMPLETE);
				return res;
			case SUSPEND : 
				res.add(LifecycleTypes.RESUME);
				return res;
			case RESUME : 
				res.add(LifecycleTypes.SUSPEND);
				res.add(LifecycleTypes.COMPLETE);
				return res;
			default:
				return null;
		}
	}

	public Set<LifecycleTypes> getAllLifecycle() {
		Set<LifecycleTypes> result = new HashSet<LifecycleTypes>();
		result.add(LifecycleTypes.START);
		result.add(LifecycleTypes.COMPLETE);
		result.add(LifecycleTypes.SUSPEND);
		result.add(LifecycleTypes.RESUME);
		return result;
	}
	
	@Override
	public String toString(){
		return "'Start-complete' lifecycle with 'suspend-resume'";
	}

	public Set<String> getAllLifecycleInString() {
		Set<String> result = new TreeSet<String>();
		result.add(LifecycleTypes.START.toString());
		result.add(LifecycleTypes.COMPLETE.toString());
		result.add(LifecycleTypes.SUSPEND.toString());
		result.add(LifecycleTypes.RESUME.toString());
		return result;
	}

	public Set<LifecycleTypes> getPrevLifecycle(LifecycleTypes query) {
		Set<LifecycleTypes> res = new HashSet<LifecycleTypes>();
		switch (query){
			case COMPLETE : 
				res.add(LifecycleTypes.START);
				res.add(LifecycleTypes.RESUME);
				break;
			case SUSPEND : 
				res.add(LifecycleTypes.START);
				res.add(LifecycleTypes.RESUME);
				break;
			case RESUME : 
				res.add(LifecycleTypes.SUSPEND);
				break;
			default: // do nothing
				break;
		}
		return res;
	}

}