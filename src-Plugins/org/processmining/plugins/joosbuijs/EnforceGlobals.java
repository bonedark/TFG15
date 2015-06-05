package org.processmining.plugins.joosbuijs;

import java.util.List;

import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;

public class EnforceGlobals {
	@Plugin(
			name = "00JB Enforce Globals",
				parameterLabels = { "Event log" },
				returnLabels = { "Event Log with all global vars set" },
				returnTypes = { XLog.class },
				userAccessible = true,
				help = "Adds attributes to traces/events that are specified by the globals if they don't exist")
	@UITopiaVariant(
			affiliation = "Eindhoven University of Technology",
				author = "J.C.A.M.Buijs",
				email = "j.c.a.m.buijs@tue.nl",
				pack = "JoosBuijs")
	public XLog enforceGlobals(final PluginContext context, XLog eventLog) {
		//Show a progress bar!
		context.getProgress().setMinimum(0);
		context.getProgress().setMaximum(eventLog.size());

		XLog returnLog = (XLog) eventLog.clone();
		
		List<XAttribute> traceGlobals = returnLog.getGlobalTraceAttributes();
		List<XAttribute> eventGlobals = returnLog.getGlobalEventAttributes();

		//Now loop through all the traces
		for (XTrace trace : returnLog) {
			//Check each trace global
			for (XAttribute att : traceGlobals) {
				if (!trace.getAttributes().containsKey(att.getKey())) {
					/*
					XAttributeMapBufferedImpl attMap = new XAttributeMapBufferedImpl();
					attMap.putAll(trace.getAttributes());
					attMap.put(att.getKey(), (XAttribute) att.clone());
					trace.setAttributes(attMap);/**/
					trace.getAttributes().put(att.getKey(), (XAttribute) att.clone());
				}
			}

			//And check all events
			for (XEvent event : trace) {
				//Check each event global
				for (XAttribute att : eventGlobals) {
					if (!event.getAttributes().containsKey(att.getKey())) {
						/* * /
						XAttributeMapBufferedImpl attMap = new XAttributeMapBufferedImpl();
						attMap.putAll(event.getAttributes());
						attMap.put(att.getKey(), (XAttribute) att.clone());
						event.setAttributes(attMap);/**/
						event.getAttributes().put(att.getKey(), (XAttribute) att.clone());
					}
				}
				//TODO We probably need to add the event again to the trace...
				int i = trace.indexOf(event);
				trace.remove(i);
				trace.add(i, event);
				
			}
		}

		return returnLog;
	}
}