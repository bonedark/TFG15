/**
 * 
 */
package org.processmining.plugins.replayer.converter.lifecycletransition;

import java.util.Set;

import org.processmining.plugins.replayer.util.LifecycleTypes;

/**
 * @author aadrians
 *
 */
public interface ILifecycleTransition {

	public LifecycleTypes getStartLifecycle();
	
	public LifecycleTypes getEndLifecycle();
	
	public Set<LifecycleTypes> getNextLifecycle(LifecycleTypes query);

	public Set<LifecycleTypes> getAllLifecycle();
	
	public Set<LifecycleTypes> getPrevLifecycle(LifecycleTypes query);
	
	public String toString();

	public Set<String> getAllLifecycleInString();
}
