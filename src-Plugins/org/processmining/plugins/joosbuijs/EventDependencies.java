package org.processmining.plugins.joosbuijs;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;

public class EventDependencies {
	private XConceptExtension conceptExtension = XConceptExtension.instance();

	@Plugin(
			name = "00JB Event Dependencies",
				parameterLabels = { "Event log" },
				returnLabels = { "Table" },
				returnTypes = { String.class },
				userAccessible = true,
				help = "Goes through the log and finds all direct A->B, B->A, etc. relations")
	@UITopiaVariant(
			affiliation = "Eindhoven University of Technology",
				author = "J.C.A.M.Buijs",
				email = "j.c.a.m.buijs@tue.nl",
				pack = "JoosBuijs")
	public String eventDependenciesPlugin(final PluginContext context, XLog eventLog) {
		//Show a progress bar!
		context.getProgress().setMinimum(0);
		context.getProgress().setMaximum(eventLog.size());

		Map<String, Integer> dep = buildAllDirectRelations(context, eventLog);

		Map<Integer, HashSet<String>> sortedDep = new TreeMap<Integer, HashSet<String>>();
		sortedDep = sortDep(dep);

		String str = buildHTML(sortedDep);

		System.out.println("HTML:");
		System.out.println(str);

		return str;
	}

	/**
	 * Builds all dependency relations A->B, B->A, etc. with counts
	 * 
	 * @param eventLog
	 * @return
	 */
	private Map<String, Integer> buildAllDirectRelations(final PluginContext context, XLog eventLog) {
		Map<String, Integer> dep = new HashMap<String, Integer>();
		//Loop through traces
		for (XTrace trace : eventLog) {
			//Per trace add all A->B, B->C, ... dependencies
			//First check trace size
			if (trace.size() > 1) {
				//Cycle through the events from beginning to second-last
				int i = 0;
				while (i < trace.size() - 1) {
					XEvent firstEvent = trace.get(i);
					XEvent secondEvent = trace.get(i + 1);
					//Construct A->B string (dirty but works)
					String dependency = conceptExtension.extractName(firstEvent) + "->"
							+ conceptExtension.extractName(secondEvent);
					//count
					int n = dep.containsKey(dependency) ? dep.get(dependency) : 0;
					dep.put(dependency, n + 1);
					i++;
				}
			}
			context.getProgress().inc();
		}

		return dep;
	}

	/**
	 * Uses the direct relations and sorts them on count, ASCENDING
	 * 
	 * @return
	 */
	private Map<Integer, HashSet<String>> sortDep(Map<String, Integer> dep) {
		Map<Integer, HashSet<String>> sortedDep = new TreeMap<Integer, HashSet<String>>(Collections.reverseOrder());
		for (Map.Entry<String, Integer> entry : dep.entrySet()) {
			//If this nr. of occur. is already there, add the pair to the set
			if (sortedDep.containsKey(entry.getValue())) {
				sortedDep.get(entry.getValue()).add(entry.getKey());
			} else {
				//Add the key and set
				HashSet<String> set = new HashSet<String>();
				set.add(entry.getKey());
				sortedDep.put(entry.getValue(), set);
			}
		}

		return sortedDep;
	}

	/**
	 * Converts a Map<Integer,Set<String>> to an HTML table
	 * 
	 */
	private String buildHTML(Map<Integer, HashSet<String>> sortedDep) {
		StringBuilder str = new StringBuilder();

		str.append("<html><body><table><tr><th>Dependency</th><th>Count</th></tr>");

		for (Map.Entry<Integer, HashSet<String>> entry : sortedDep.entrySet()) {
			HashSet<String> set = entry.getValue();
			Iterator<String> it = set.iterator();
			while (it.hasNext()) {
				str.append("<tr><td>" + it.next() + "</td><td>" + entry.getKey() + "</td></tr>");
			}

		}
		str.append("</table></html>");

		return str.toString();
	}
}
