/**
 * 
 */
package org.processmining.plugins.replayer.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.deckfour.xes.classification.XEventClass;

/**
 * @author aadrians
 *
 */
public class EventEncoder {
	private Map<Short, XEventClass> mapShortEventClass = new HashMap<Short, XEventClass>();
	private Map<XEventClass, Short> mapEventClassShort = new HashMap<XEventClass, Short>();
	
	public EventEncoder(Map<XEventClass, Short> mapEventClassShort){
		this.mapEventClassShort = mapEventClassShort;
		for (XEventClass evClass : mapEventClassShort.keySet()){
			mapShortEventClass.put(mapEventClassShort.get(evClass), evClass);
		}
	}
	
	public Short encode(XEventClass eventClass){
		return mapEventClassShort.get(eventClass);
	}
	
	public XEventClass decode(short value){
		return mapShortEventClass.get(value);
	}
	
	public Collection<Short> getMappedEncodedFlexNode(){
		return mapEventClassShort.values();
	}
	
	public String toString(){
		String res = "";
		res += "EventClass --> Short \n";
		for (XEventClass ev : mapEventClassShort.keySet()){
			res += ev.toString() + " --> " + mapEventClassShort.get(ev).toString() + "\n";
		}
		res += "-------------------------------------------";
		res += "Short --> EventClass \n";
		for (Short sh : mapShortEventClass.keySet()){
			res += sh + " --> " + mapShortEventClass.get(sh).toString() + "\n";
		}
		res += "-------------------------------------------";
		return res;
	}
}
