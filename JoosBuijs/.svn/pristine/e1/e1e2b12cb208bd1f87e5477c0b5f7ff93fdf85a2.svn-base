package org.processmining.plugins.joosbuijs;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XOrganizationalExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.buffered.XAttributeMapBufferedImpl;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.deckfour.xes.model.impl.XAttributeTimestampImpl;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;

public class AddArtificialStartEnd {
	private XFactory factory = XFactoryRegistry.instance().currentDefault();
	private XConceptExtension conceptExt = XConceptExtension.instance();
	private XTimeExtension timeExt = XTimeExtension.instance();
	private XOrganizationalExtension orgExt = XOrganizationalExtension.instance();
	private XLifecycleExtension lifecycleExt = XLifecycleExtension.instance();

	@Plugin(
			name = "00JB Add artificial start and end events",
				parameterLabels = { "Event log" },
				returnLabels = { "Event Log with artificial start/end events" },
				returnTypes = { XLog.class },
				userAccessible = true,
				help = "Adds events to the beginning and end of each trace")
	@UITopiaVariant(
			affiliation = "Eindhoven University of Technology",
				author = "J.C.A.M.Buijs",
				email = "j.c.a.m.buijs@tue.nl",
				pack = "JoosBuijs")
	public XLog addArtificialStartEnd(final PluginContext context, XLog eventLog) {
		XLog eLogArt = (XLog) eventLog.clone();

		for (XTrace trace : eLogArt) {
			XAttributeMap eventStartAttributes = new XAttributeMapBufferedImpl();
			eventStartAttributes.put("concept:name", new XAttributeLiteralImpl("concept:name", "Artificial Start",
					conceptExt));
			eventStartAttributes.put("org:resource", new XAttributeLiteralImpl("org:resource", "Artificial Start",
					orgExt));
			eventStartAttributes.put("lifecycle:transition", new XAttributeLiteralImpl("lifecycle:transition",
					"complete", lifecycleExt));
			eventStartAttributes.put("time:timestamp",
					new XAttributeTimestampImpl("time:timestamp", timeExt.extractTimestamp(trace.get(0)), timeExt));
			XEvent eventStart = factory.createEvent(eventStartAttributes);

			XAttributeMap eventEndAttributes = new XAttributeMapBufferedImpl();
			eventEndAttributes.put("concept:name", new XAttributeLiteralImpl("concept:name", "Artificial End",
					conceptExt));
			eventEndAttributes.put("org:resource", new XAttributeLiteralImpl("org:resource", "Artificial End", orgExt));
			eventEndAttributes.put("lifecycle:transition", new XAttributeLiteralImpl("lifecycle:transition",
					"complete", lifecycleExt));
			eventEndAttributes.put("time:timestamp",
					new XAttributeTimestampImpl("time:timestamp",
							timeExt.extractTimestamp(trace.get(trace.size() - 1)), timeExt));
			XEvent eventEnd = factory.createEvent(eventEndAttributes);

			trace.add(0, eventStart);
			trace.add(eventEnd);
		}

		return eLogArt;
	}

}
