package org.processmining.plugins.joosbuijs;

import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;

public class UnpackGroupedLog {

	@Plugin(
			name = "00JB Unpack EL",
				parameterLabels = { "Event log" },
				returnLabels = { "Unpacked Event Log" },
				returnTypes = { XLog.class },
				userAccessible = true,
				help = "...")
	@UITopiaVariant(
			uiLabel = "00JB Unpack EL",
				affiliation = "Eindhoven University of Technology",
				author = "J.C.A.M.Buijs",
				email = "j.c.a.m.buijs@tue.nl",
				pack = "JoosBuijs")
	public XLog unpack(final PluginContext context, XLog eventLog) {
		XLog ungroupedEventLog = XFactoryRegistry.instance().currentDefault().createLog();
		//(XLog) eventLog.clone();

		//TODO copy log attributes...
		
		//Now go through each trace and ungroup
		for (XTrace trace : eventLog) {
			//Get the count
			XAttribute countAtt = trace.getAttributes().get("numSimilarInstances");
			int c = Integer.parseInt(countAtt.toString());
			
			for(int i = 0; i < c; i++){
				ungroupedEventLog.add((XTrace) trace.clone());
			}
		}

		return ungroupedEventLog;
	}

}
