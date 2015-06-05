/**
 * 
 */
package org.processmining.plugins.flex.replayer.performance.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.processmining.plugins.replayer.converter.lifecycletransition.FullLifecycleTransition;
import org.processmining.plugins.replayer.converter.lifecycletransition.ILifecycleTransition;
import org.processmining.plugins.replayer.converter.lifecycletransition.OnlyCompleteLifecycleTransition;
import org.processmining.plugins.replayer.util.LifecycleTypes;

/**
 * Assumption: the selected mostCompleteLT contains all possible lifecycle
 * transition exist in the log
 * 
 * @author aadrians
 * 
 */
public class LifecycleIdentifier {
	private ILifecycleTransition mostCompleteLT = new FullLifecycleTransition();
	private Set<ILifecycleTransition> supportedLifecycle;

	// storing data
	private Map<XEventClass, ILifecycleTransition> mapEvClassLifeTrans = new HashMap<XEventClass, ILifecycleTransition>();
	private Map<XEventClass, LifecycleTypes> mapToLifecycle;
	private Map<XEventClass, Set<XEventClass>> mapGeneralECToSetDetailedEC;

	public LifecycleIdentifier() {
		supportedLifecycle = new HashSet<ILifecycleTransition>();
	}

	public Set<XEventClass> identifyLifecycleForEachActivity(XEventClasses activityNames, XEventClasses actLifecycleNames) {
		// map name of activity to event class
		Map<String, XEventClass> mapNameStrToEC = new HashMap<String, XEventClass>();
		for (XEventClass ec : activityNames.getClasses()){
			mapNameStrToEC.put(ec.toString().trim(), ec);
		}
		
		// map event class A to set of lifecycle types
		Map<XEventClass, Set<LifecycleTypes>> mapECToSetLifecycle = new HashMap<XEventClass, Set<LifecycleTypes>>();
		
		// map event class A-complete to lifecycletype start/complete/suspend/etc.
		mapToLifecycle = new HashMap<XEventClass, LifecycleTypes>();

		// map event class event class A to set consists of A-complete/A-start
		mapGeneralECToSetDetailedEC = new HashMap<XEventClass, Set<XEventClass>>();
			
		for (XEventClass evClassWLifecycle : actLifecycleNames.getClasses()) {
			String[] label = evClassWLifecycle.toString().split("\\+");
			assert(label != null); // otherwise, it means the class does not have any activity
			
			if ((label.length == 2)&&(!label[1].trim().equals(""))) {
				LifecycleTypes type = getLifecycleTypeFor(label[1].trim()); 
				insertToMap(mapNameStrToEC.get(label[0].trim()), type, mapECToSetLifecycle);
				mapToLifecycle.put(evClassWLifecycle, type);				
				insertToMap(mapNameStrToEC.get(label[0].trim()), evClassWLifecycle, mapGeneralECToSetDetailedEC);
			} else {
				// non standard. Map to complete
				insertToMap(mapNameStrToEC.get(label[0].trim()), LifecycleTypes.COMPLETE, mapECToSetLifecycle);
				mapToLifecycle.put(evClassWLifecycle, LifecycleTypes.COMPLETE);
				insertToMap(mapNameStrToEC.get(label[0].trim()), evClassWLifecycle, mapGeneralECToSetDetailedEC);
			}
		}

		// identify lifecycle transition for each node event class
		Set<XEventClass> unidentifiedLT = null;
		for (XEventClass activityNameEC : activityNames.getClasses()) {
			Set<LifecycleTypes> actLifecycleNamesStr = mapECToSetLifecycle.get(activityNameEC);
			assert (actLifecycleNamesStr != null); // because activity+lifecycle is derived from activity name

			ILifecycleTransition mostProperLC = getMostAppropriateLifecycle(actLifecycleNamesStr);
			if (mostProperLC != null){
				mapEvClassLifeTrans.put(activityNameEC, mostProperLC);
			} else {
				if (unidentifiedLT == null){
					unidentifiedLT = new HashSet<XEventClass>();
				}
				unidentifiedLT.add(activityNameEC);
			}
		}
		return unidentifiedLT;
	}

	private LifecycleTypes getLifecycleTypeFor(String lifecycleStr) {
		if (lifecycleStr.equals("complete")){
			return LifecycleTypes.COMPLETE;
		} else 
		if (lifecycleStr.equals("start")){
			return LifecycleTypes.START;
		} else 
		if (lifecycleStr.equals("suspend")){
			return LifecycleTypes.SUSPEND;
		} else if (lifecycleStr.equals("resume")){
			return LifecycleTypes.RESUME;
		} 
		return null;
	}

	private ILifecycleTransition getMostAppropriateLifecycle(Set<LifecycleTypes> lifecycleType) {
		if (lifecycleType.size() == 1) {
			return new OnlyCompleteLifecycleTransition();
		} else {
			ILifecycleTransition mostAppropriateLT = mostCompleteLT;

			// if a lifecycle is not included in full lifecycle, it is not valid
			if (!mostAppropriateLT.getAllLifecycle().containsAll(lifecycleType)){
				return null;
			};

			int smallestContain = mostAppropriateLT.getAllLifecycle().size();

			for (ILifecycleTransition lt : supportedLifecycle) {
				Set<LifecycleTypes> allLT = lt.getAllLifecycle();
				if (allLT.containsAll(lifecycleType)) {
					if (allLT.size() < smallestContain) {
						smallestContain = allLT.size();
						mostAppropriateLT = lt;
					}
				}
			}
			return mostAppropriateLT;
		}
	}

	private <S, T> void insertToMap(S key, T value, Map<S, Set<T>> map) {
		Set<T> set = map.get(key);
		if (set != null) {
			set.add(value);
		} else {
			set = new HashSet<T>();
			set.add(value);
			map.put(key, set);
		}
	}

	public void addSupportedLifecycle(ILifecycleTransition lifecycleTransition) {
		supportedLifecycle.add(lifecycleTransition);
	}

	public Map<XEventClass, ILifecycleTransition> getLifecycleTransitionForEachActivity() {
		return mapEvClassLifeTrans;
	}

	public Map<XEventClass, Set<XEventClass>> getMappingBetweenEventClass() {
		return mapGeneralECToSetDetailedEC;
	}

	public Map<XEventClass, LifecycleTypes>  getMappingToLifecycleTypes() {
		return mapToLifecycle;
	}
	
	public Set<LifecycleTypes> getPossibleLifecycleTypes(){
		return mostCompleteLT.getAllLifecycle();
	}
	
}
