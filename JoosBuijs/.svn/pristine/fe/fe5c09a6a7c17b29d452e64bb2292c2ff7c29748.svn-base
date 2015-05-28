package org.processmining.plugins.joosbuijs.eventSorter;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;

public class CompareTraceOrder {
	private XConceptExtension conceptExt = XConceptExtension.instance();
	
	@Plugin(
			name = "00JB Trace Comparison",
				parameterLabels = { "Event log 1", "Event log 2" },
				returnLabels = { },
				returnTypes = {  },
				userAccessible = true,
				help = "Compares order of events between traces (f.i. usefull after sorting)")
	@UITopiaVariant(
			affiliation = "Eindhoven University of Technology",
				author = "J.C.A.M.Buijs",
				email = "j.c.a.m.buijs@tue.nl", pack = "JoosBuijs")
	public void compareTraceOrder(final PluginContext context, XLog eventLog1, XLog eventLog2) {
		for(int i = 0; i < eventLog1.size() && i < eventLog2.size(); i++)
		{
			XTrace trace1 = eventLog1.get(i);
			XTrace trace2 = eventLog2.get(i);
			
			System.out.println("Trace: " + conceptExt.extractName(trace1));
			
			for(int j = 0; j < trace1.size() && j<trace2.size(); j++)
			{
				String event1Name = conceptExt.extractName(trace1.get(j));
				String event2Name = conceptExt.extractName(trace2.get(j));
				if(!event1Name.equalsIgnoreCase(event2Name))
				{
					System.out.println(j + " " + event1Name + "-" + event2Name);
				}
			}
		}
		
	}

}
