package org.processmining.plugins.joosbuijs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.TreeSet;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

/*-
 * PRIVATE
 * 
 * ToDo's before public:
 *  - clean up code
 *  	- move code from plugin function to delegate functions
 *  	- remove CoSeloG specific stuffs
 *  - Improve name matching (f.i. ALL lower case)
 *  
 */

@Plugin(
		name = "00JB FScore XxX",
			parameterLabels = { "Event log 1", "Event log 2", "Event log 3", "Event log 4", "Event log 5",
					"Event log 6", "Event log 7", "Event log 8", "Event log 9", "Event log 10", "Petri Net 1",
					"Petri Net 2", "Petri Net 3", "Petri Net 4", "Petri Net 5", "Petri Net 6", "Petri Net 7",
					"Petri Net 8", "Petri Net 9", "Petri Net 10" },
			returnLabels = { "F-Score" },
			returnTypes = { String.class },
			userAccessible = true,
			help = "Calculates the F-Score between X models and X event logs (Based on event name!)")
public class FScore {
	XConceptExtension conceptExtension = XConceptExtension.instance();
	//We look at event names, without lifecycle status etc.
	private XEventNameClassifier eventNameClassifier = new XEventNameClassifier();

	private enum SCORES {
		PRECISION, RECALL, FSCORE
	};

	private enum SETS {
		TP, FP, FN
	};

	/*-
	@Plugin(
			name = "00JB FScore",
				parameterLabels = { "Event log", "Petri Net" },
				returnLabels = { "F-Score" },
				returnTypes = { String.class },
				userAccessible = true,
				help = "Calculates the F-Score between the model and the event log (Based on event name!)")
	@UITopiaVariant(
			affiliation = "Eindhoven University of Technology",
				author = "J.C.A.M.Buijs",
				email = "j.c.a.m.buijs@tue.nl")
	public String fScorePlugin(final PluginContext context, XLog eventLog, Petrinet net) {

		XLogInfo logInfo = XLogInfoImpl.create(eventLog, eventNameClassifier);

		//We call 2 calculating functions (we need the intermediate result)
		HashMap<SETS, HashSet<String>> sets = calculateSets(logInfo, net);
		HashMap<SCORES, Float> scores = calculateScores(sets);

		//We can now build our output!!!
		StringBuilder html = new StringBuilder();

		html.append("<html><body>");

		html.append(infoToHtmlTable(sets, scores));

		html.append("</body></html>");

		return html.toString();
	}/**/

	@PluginVariant(variantLabel = "00JB FScore 1x1", requiredParameterLabels = { 0, 10 })
	@UITopiaVariant(
			uiLabel = "00JB FScore 1x1",
				affiliation = "Eindhoven University of Technology",
				author = "J.C.A.M.Buijs",
				email = "j.c.a.m.buijs@tue.nl",
				pack = "JoosBuijs")
	public String fScorePlugin(final PluginContext context, XLog eventLog, Petrinet net) {

		//Lets put our log in a list...
		LinkedList<XLog> logs = new LinkedList<XLog>();
		logs.add(eventLog);

		//And also our model in a list
		LinkedList<Petrinet> nets = new LinkedList<Petrinet>();
		nets.add(net);

		return process(logs, nets);
	}

	@PluginVariant(variantLabel = "00JB FScore 3x3", requiredParameterLabels = { 0, 1, 2, 10, 11, 12 })
	@UITopiaVariant(
			uiLabel = "00JB FScore 3x3",
				affiliation = "Eindhoven University of Technology",
				author = "J.C.A.M.Buijs",
				email = "j.c.a.m.buijs@tue.nl",
				pack = "JoosBuijs")
	public String fScorePlugin(final PluginContext context, XLog eventLog1, XLog eventLog2, XLog eventLog3,
			Petrinet net1, Petrinet net2, Petrinet net3) {

		//Lets put our 3 logs in a list...
		LinkedList<XLog> logs = new LinkedList<XLog>();
		logs.add(eventLog1);
		logs.add(eventLog2);
		logs.add(eventLog3);

		//And also our 3 models in a list
		LinkedList<Petrinet> nets = new LinkedList<Petrinet>();
		nets.add(net1);
		nets.add(net2);
		nets.add(net3);

		return process(logs, nets);
	}

	@PluginVariant(variantLabel = "00JB FScore 4x4", requiredParameterLabels = { 0, 1, 2, 3, 10, 11, 12, 13 })
	@UITopiaVariant(
			uiLabel = "00JB FScore 4x4",
				affiliation = "Eindhoven University of Technology",
				author = "J.C.A.M.Buijs",
				email = "j.c.a.m.buijs@tue.nl",
				pack = "JoosBuijs")
	public String fScorePlugin(final PluginContext context, XLog eventLog1, XLog eventLog2, XLog eventLog3,
			XLog eventLog4, Petrinet net1, Petrinet net2, Petrinet net3, Petrinet net4) {

		//Lets put our 4 logs in a list...
		LinkedList<XLog> logs = new LinkedList<XLog>();
		logs.add(eventLog1);
		logs.add(eventLog2);
		logs.add(eventLog3);
		logs.add(eventLog4);

		//And also our 4 models in a list
		LinkedList<Petrinet> nets = new LinkedList<Petrinet>();
		nets.add(net1);
		nets.add(net2);
		nets.add(net3);
		nets.add(net4);

		return process(logs, nets);
	}

	@PluginVariant(variantLabel = "00JB FScore 5x3", requiredParameterLabels = { 0, 1, 2, 3, 4, 10, 11, 12 })
	@UITopiaVariant(
			uiLabel = "00JB FScore 5x3",
				affiliation = "Eindhoven University of Technology",
				author = "J.C.A.M.Buijs",
				email = "j.c.a.m.buijs@tue.nl",
				pack = "JoosBuijs")
	public String fScorePlugin(final PluginContext context, XLog eventLog1, XLog eventLog2, XLog eventLog3,
			XLog eventLog4, XLog eventLog5, Petrinet net1, Petrinet net2, Petrinet net3) {

		//Lets put our 4 logs in a list...
		LinkedList<XLog> logs = new LinkedList<XLog>();
		logs.add(eventLog1);
		logs.add(eventLog2);
		logs.add(eventLog3);
		logs.add(eventLog4);
		logs.add(eventLog5);

		//And also our 4 models in a list
		LinkedList<Petrinet> nets = new LinkedList<Petrinet>();
		nets.add(net1);
		nets.add(net2);
		nets.add(net3);

		return process(logs, nets);
	}

	/**
	 * The main function that calls calculating and presenting functions (and
	 * does some of that itself)
	 */
	private String process(LinkedList<XLog> logs, LinkedList<Petrinet> nets) {
		//The fscore table
		StringBuilder fscoreTable = new StringBuilder();
		//The precision table
		StringBuilder precisionTable = new StringBuilder();
		//The recall table
		StringBuilder recallTable = new StringBuilder();
		//And some detailed 'cell' info
		StringBuilder detailedInfo = new StringBuilder();

		//Start the tables with the header row
		fscoreTable.append("<table border=\"1\"><tr><th></th>");
		for (int i = 1; i <= nets.size(); i++) {
			fscoreTable.append("<th>Net" + i + "</th>");
		}
		fscoreTable.append("</tr>");

		//Copy
		precisionTable.append(fscoreTable.toString());
		recallTable.append(fscoreTable.toString());

		//Loop through all combi's
		int l = 1;
		for (XLog eventLog : logs) {
			fscoreTable.append("<tr><th>Log " + l + "</th>");
			precisionTable.append("<tr><th>Log " + l + "</th>");
			recallTable.append("<tr><th>Log " + l + "</th>");

			XLogInfo logInfo = XLogInfoImpl.create(eventLog, eventNameClassifier);
			String logName = conceptExtension.extractName(eventLog);

			for (Petrinet net : nets) {
				//We call 2 calculating functions (we need the intermediate result)
				HashMap<SETS, HashSet<String>> sets = calculateSets(logInfo, net);
				HashMap<SCORES, Float> scores = calculateScores(sets);

				detailedInfo.append("<h2>Details '" + logName + "' ON '" + net.getLabel() + "' </h2>");

				detailedInfo.append(sets.get(SETS.TP).size() + " ; " + sets.get(SETS.FP).size() + " ; "
						+ sets.get(SETS.FN).size() + "<br>");
				detailedInfo.append("TP:<br>");
				detailedInfo.append(setToHtmlString(sets.get(SETS.TP)));
				detailedInfo.append("FP:<br>");
				detailedInfo.append(setToHtmlString(sets.get(SETS.FP)));
				detailedInfo.append("FN:<br>");
				detailedInfo.append(setToHtmlString(sets.get(SETS.FN)));

				//Add info to the cells of the correct tables
				fscoreTable.append("<td>" + scores.get(SCORES.FSCORE) + "</td>");
				precisionTable.append("<td>" + scores.get(SCORES.PRECISION) + "</td>");
				recallTable.append("<td>" + scores.get(SCORES.RECALL) + "</td>");
			}
			fscoreTable.append("</tr>");
			precisionTable.append("</tr>");
			recallTable.append("</tr>");

			l++;
		}
		fscoreTable.append("</table>");
		precisionTable.append("</table>");
		recallTable.append("</table>");

		//Now build a small legend to be able to map event logs and nets to names
		StringBuilder legend = new StringBuilder();
		legend.append("<h3>Legend</h3>");
		legend.append("<table>");
		int j = 1;
		for (XLog eventLog : logs) {
			legend.append("<tr><td>Log " + j + "</td><td>" + conceptExtension.extractName(eventLog) + "</td></tr>");
			j++;
		}
		legend.append("</table><table>");
		int k = 1;
		for (Petrinet net : nets) {
			legend.append("<tr><td>Net " + k + "</td><td>" + net.getLabel() + "</td></tr>");
			k++;
		}
		legend.append("</table>");

		//Now build the final HTML
		//We will calculate the sets and scores for each log/net combi
		// And output it immediately
		StringBuilder html = new StringBuilder();

		html.append("<html><body>");

		//Add tables
		html.append("<h1>Overall tables</h1>");
		html.append("<h2>FScore</h2>");
		html.append(fscoreTable.toString());
		html.append("<h2>Precision</h2>");
		html.append(precisionTable.toString());
		html.append("<h2>Recall</h2>");
		html.append(recallTable.toString());

		html.append(legend.toString());

		html.append(detailedInfo.toString());

		html.append("</body></html>");

		String htmlStr = html.toString();

		System.out.println("HTML:");
		System.out.println(htmlStr);

		return htmlStr;
	}

	/**
	 * Calculates F-Scores for the given log/net combination
	 * 
	 * @param logInfo
	 * @param net
	 * @return HashMap with the keys 'precision', 'recall' and 'F1'
	 */
	private HashMap<SCORES, Float> calculateScores(HashMap<SETS, HashSet<String>> sets) {
		HashMap<SCORES, Float> scores = new HashMap<SCORES, Float>();

		//Extract set sizes (and remain 0 if nullPointer)
		float tpSize = 0;
		try {
			tpSize = sets.get(SETS.TP).size();
		} catch (Exception e) {
		}
		float fpSize = 0;
		try {
			fpSize = sets.get(SETS.FP).size();
		} catch (Exception e) {
		}
		float fnSize = 0;
		try {
			fnSize = sets.get(SETS.FN).size();
		} catch (Exception e) {
		}

		//Calculate metrics (and ignore div by 0 errors...)
		//Precision = tp / (tp+fp)
		float precision = 0f;
		try {
			precision = (tpSize / (tpSize + fpSize));
		} catch (Exception e) {
		}

		//Recall = tp / (tp+fn)
		float recall = 0f;
		try {
			recall = (tpSize / (tpSize + fnSize));
		} catch (Exception e) {
		}

		//F1-Score
		float fscore = 0f;
		try {
			fscore = 2 * ((precision * recall) / (precision + recall));
		} catch (Exception e) {
			fscore = 0;
		}

		//Store in return object
		scores.put(SCORES.PRECISION, precision);
		scores.put(SCORES.RECALL, recall);
		scores.put(SCORES.FSCORE, fscore);

		return scores;
	}

	/**
	 * Calculates the tp, fp and fn sets between log and model
	 * 
	 * @param logInfo
	 * @param net
	 * @return HashSet of Hashset of String in
	 */
	private HashMap<SETS, HashSet<String>> calculateSets(XLogInfo logInfo, Petrinet net) {
		//We keep track of the event/transition names names that are:
		//True Positive (both in log and model)
		HashSet<String> tp = new HashSet<String>();
		//False Positive (in the model but not in the log)
		HashSet<String> fp = new HashSet<String>();
		//False Negative (in the log but not in the model)
		HashSet<String> fn = new HashSet<String>();

		/*-
		 * There are, of course, 4 options: true/false positive/negative
		 * We have however always 0 true negatives 
		 *    (those not in the model and not in the net)
		 * 1. We start with all Petri net transitions in fp
		 * 2. We move those in the eventlog and the net to tp
		 *     Please note that we apply some renamings here since we might
		 *     want to map certain events to certain tasks
		 *     (depending on the log and model source!!!)
		 * 3. If we don't find a match, we add the event name
		 *      to the fn list
		 *
		 * This way we loop once to get all transitions in the fp set
		 *  and then we loop once over all combi's to move them to the
		 *  correct set (instead of 2 times iterating over all combi's)
		 */
		//1. Fill fp with all net transitions
		for (Transition transition : net.getTransitions()) {
			//Some Petri Net transitions should NOT be included
			//e.g. artificial start/end and SILENT skips
			String label = transition.getLabel();
			if (!label.contains("SILENT") && !label.contains("Artificial")) {
				//Add the renamed label!
				fp.add(applyRenamings(label, logInfo, net));
			}
		}

		//1b. Apply the renamings from model to log
		//fp = applyWMORenamings(fp, logInfo, net);

		//2. Now, we check all events in the log against the model
		for (XEventClass event : logInfo.getNameClasses().getClasses()) {
			String eventStr = applyRenamings(event.getId(), logInfo, net);
			for (Transition transition : net.getTransitions()) {
				String label = applyRenamings(transition.getLabel(), logInfo, net);
				//If the event from the log exists in the net
				if (eventStr.equalsIgnoreCase(label)) {
					//We found a True Positive!!!
					if (!tp.contains(eventStr)) {
						tp.add(eventStr);
					}
					//and remove it from the fp list
					fp.remove(eventStr);
				}
			}

			//3. If we didn't find a match in the model
			if (!tp.contains(eventStr)) {
				//We have a false positive
				fn.add(eventStr);
			}
		}

		//Build the return object
		HashMap<SETS, HashSet<String>> ret = new HashMap<SETS, HashSet<String>>();
		ret.put(SETS.TP, tp);
		ret.put(SETS.FP, fp);
		ret.put(SETS.FN, fn);

		return ret;
	}

	/*-
	private StringBuffer infoToHtmlTable(HashMap<SETS, HashSet<String>> sets, HashMap<SCORES, Float> scores) {
		StringBuffer info = new StringBuffer();

		info.append("<h1>True Positives:</h1>");
		info.append(setToHtmlString(sets.get(SETS.TP)));

		info.append("<h1>False Positives:</h1>");
		info.append(setToHtmlString(sets.get(SETS.FP)));

		info.append("<h1>False Negatives:</h1>");
		info.append(setToHtmlString(sets.get(SETS.FN)));

		info.append("<h1>Scores:</h1>");
		info.append(scoresToHtmlTable(scores));

		return info;
	}/**/

	/**
	 * Converts a hashSet to an HTML table
	 * 
	 * @param set
	 * @return
	 */
	private StringBuffer setToHtmlString(HashSet<String> set) {
		StringBuffer html = new StringBuffer();

		//We want it sorted (we're humans, reads easier)
		TreeSet<String> sortedSet = new TreeSet<String>(set);

		for (String word : sortedSet) {
			html.append(word + "<br>");
		}

		return html;
	}

	/**
	 * Converts a hashMap of scores to an HTML table
	 * 
	 * @param scores
	 * @return
	 */
	/*-
	private StringBuffer scoresToHtmlTable(HashMap<SCORES, Float> scores) {
		StringBuffer table = new StringBuffer();

		table.append("<table><tr><th>Metric</th><th>Score</th></tr>");

		for (SCORES value : SCORES.values()) {
			table.append("<tr><td>" + value + "</td><td>" + scores.get(value) + "</td></tr>");
		}

		table.append("</table>");

		return table;
	}/**/

	/**
	 * Applies some CoSeLoG WMO specific renamings for better/special matching
	 * Needs logInfo and Petrinet since some mappings depend on this.
	 * 
	 * @param set
	 * @param logInfo
	 * @param net
	 * @return renamed hashset<String>
	 */
	private String applyRenamings(String label, XLogInfo logInfo, Petrinet net) {
		//System.out.println("Set BEFORE: " + setToHtmlTable(set));

		//First we need to determine from which municipality the log and PN are from
		//The municipalities we should recognize
		HashSet<String> municipalities = new HashSet<String>();
		//WMO municipalities
		municipalities.add("ISD");
		municipalities.add("Hld");
		municipalities.add("Zwl");
		municipalities.add("GBl");
		//WOZ municipalities
		municipalities.add("Ers");
		municipalities.add("Ors");

		//Log:
		String logm = "";
		String logName = conceptExtension.extractName(logInfo.getLog());
		String netm = "";
		String modelName = net.getLabel();

		for (String m : municipalities) {
			if (logName.contains(m))
				logm = m;
			if (modelName.contains(m))
				netm = m;
		}

		//Stop if we couldn't match one of the municipalities
		if (logm.isEmpty() || netm.isEmpty())
			return label;

		//Now, statically build our map table from m1 to m2 we map event X to Y
		HashMap<String, String> mapping = new HashMap<String, String>();
		//Keys are build m1-m2-X where the replacement is X->Y
		//WMO mappings
		mapping.put("Hld-ISD-Afwijzen", "Beslissing");
		mapping.put("Hld-ISD-Eindrapportage adviseur", "Rapportage");
		mapping.put("Hld-ISD-eindrapportage loket", "Rapportage");
		mapping.put("Hld-ISD-Niet in behandeling nemen", "Beslissing");
		mapping.put("Hld-ISD-Onderzoek adviseur", "Onderzoek");
		mapping.put("Hld-ISD-onderzoek loket", "Onderzoek");
		mapping.put("Hld-ISD-Onderzoek/rapportage", "Onderzoek");
		mapping.put("Hld-ISD-Overige besluiten", "Beslissing");
		mapping.put("Hld-ISD-Toekennen", "Beslissing");
		mapping.put("Hld-ISD-Voor advies CIZ-arts", "Extern advies");
		mapping.put("Hld-Zwl-Afwijzen", "Beslissing");
		mapping.put("Hld-Zwl-Eindrapportage adviseur", "Rapportage");
		mapping.put("Hld-Zwl-eindrapportage loket", "Rapportage");
		mapping.put("Hld-Zwl-Niet in behandeling nemen", "Beslissing");
		mapping.put("Hld-Zwl-Onderzoek/rapportage", "Rapportage");
		mapping.put("Hld-Zwl-Overige besluiten", "Beslissing");
		mapping.put("Hld-Zwl-Toekennen", "Beslissing");
		mapping.put("Hld-Zwl-Toetsing", "Toetsing autorisator");
		mapping.put("Hld-Zwl-Voor advies CIZ-arts", "Medisch advies");
		mapping.put("ISD-Hld-Bevestiging aanvraag", "aanmelding");
		mapping.put("Zwl-ISD-Intake / advies extern", "Extern advies");
		mapping.put("Zwl-ISD-Medisch advies", "Extern advies");
		mapping.put("Zwl-ISD-Toetsing autorisator", "Toetsing");

		//And WOZ mappings
		mapping.put("Ers-Ors-RETTAX", "RETOUR");
		mapping.put("Ers-Ors-NATAX", "TTO");

		//Key is log-net-from
		String key = logm + "-" + netm + "-" + label;
		String keyRev = netm + "-" + logm + "-" + label;

		//Check in 2 directions
		if (mapping.containsKey(key)) {
			System.out.println("Changing " + label + " to " + mapping.get(key));
			return mapping.get(key);
		} else if (mapping.containsKey(keyRev)) {
			System.out.println("Changing " + label + " to " + mapping.get(keyRev));
			return mapping.get(keyRev);
		} else {
			//System.out.println("Keeping " + label);
			return label;
		}
	}
}
