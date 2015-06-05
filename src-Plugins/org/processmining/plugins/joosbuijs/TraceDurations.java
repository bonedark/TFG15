package org.processmining.plugins.joosbuijs;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.deckfour.xes.classification.XEventAndClassifier;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.classification.XEventLifeTransClassifier;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.util.Pair;
import org.processmining.plugins.tsanalyzer.Duration;

public class TraceDurations {
	XTimeExtension timeExt = XTimeExtension.instance();
	double week = (double) 7 * 24 * 60 * 60 * 1000;
	int deadline = 16; //Nr of weeks to take as deadline
	DecimalFormat decFormat = new DecimalFormat("0.0000", new DecimalFormatSymbols(Locale.US));
	XEventClassifier defaultClassifier = new XEventAndClassifier(new XEventNameClassifier(),
			new XEventLifeTransClassifier());
	XConceptExtension conceptExt = XConceptExtension.instance();

	@Plugin(
			name = "00JB Trace Durations",
				parameterLabels = { "Event log" },
				returnLabels = { "Trace Duration Info" },
				returnTypes = { String.class },
				userAccessible = true,
				help = "Calculates the trace durations and presents some statistical information.")
	@UITopiaVariant(
			uiLabel = "00JB Trace Durations",
				affiliation = "Eindhoven University of Technology",
				author = "J.C.A.M.Buijs",
				email = "j.c.a.m.buijs@tue.nl",
				pack = "JoosBuijs")
	public String traceDurationPlugin(final PluginContext context, XLog eventLog) {
		decFormat.setGroupingUsed(false);

		DescriptiveStatistics durations = calculateDuration(eventLog);

		String html = outputDurationInfo(durations);

		System.out.println("HTML:");
		System.out.println(html);

		return html;
	}

	@Plugin(
			name = "00JB Durations between events",
				parameterLabels = { "Event log" },
				returnLabels = { "Event Duration Info" },
				returnTypes = { String.class },
				userAccessible = true,
				help = "Calculates the trace durations and presents some statistical information.")
	@UITopiaVariant(
			uiLabel = "00JB Durations between events",
				affiliation = "Eindhoven University of Technology",
				author = "J.C.A.M.Buijs",
				email = "j.c.a.m.buijs@tue.nl",
				pack = "JoosBuijs")
	public String eventDurationPlugin(final PluginContext context, XLog eventLog) {
		decFormat.setGroupingUsed(false);
		List<XEventClassifier> classifiers = eventLog.getClassifiers();

		Map<Pair<String, String>, DescriptiveStatistics> durations = calculateEventDuration(eventLog);
		TreeMap<Pair<String, String>, DescriptiveStatistics> sortedDurations = sortDurations(durations);

		String html = outputEventDurationInfo(sortedDurations);

		System.out.println("HTML:");
		System.out.println(html);

		return html;
	}

	@Plugin(
			name = "00JB GBl Bouw Time analysis",
				parameterLabels = { "Event log" },
				returnLabels = { "Trace Duration Info" },
				returnTypes = { String.class },
				userAccessible = true,
				help = "Calculates the trace durations and presents some statistical information.")
	@UITopiaVariant(
			uiLabel = "00JB GBl Bouw Time analysis",
				affiliation = "Eindhoven University of Technology",
				author = "J.C.A.M.Buijs",
				email = "j.c.a.m.buijs@tue.nl",
				pack = "JoosBuijs")
	public String timeAnalisysGBlBouwPlugin(final PluginContext context, XLog eventLog) {
		decFormat.setGroupingUsed(false);

		DescriptiveStatistics durations = calculateDuration(eventLog);

		String durationHTML=outputDurationInfo(durations);

		System.out.println("HTML duration info:");
		System.out.println(durationHTML);
		
		String deadlineHTML = calcEventsBeforeAndAfterDealine(eventLog, deadline*week);
		
		System.out.println("HTML dealine event info:");
		System.out.println(deadlineHTML);

		return durationHTML + "<br><br>" + deadlineHTML;
	}

	private DescriptiveStatistics calculateDuration(XLog eventLog) {
		DescriptiveStatistics stats = new DescriptiveStatistics();

		for (XTrace trace : eventLog) {
			//NOTE: assuming that events are `correctly' ordered!!!
			Date start = timeExt.extractTimestamp(trace.get(0));
			Date end = timeExt.extractTimestamp(trace.get(trace.size() - 1));
			//Don't count `zero duration' cases (ARGUABLE)
			//if (!start.equals(end)) {
			stats.addValue(end.getTime() - start.getTime());
			//}
		}

		return stats;
	}

	private Map<Pair<String, String>, DescriptiveStatistics> calculateEventDuration(XLog eventLog) {
		Map<Pair<String, String>, DescriptiveStatistics> eventpairStats = new HashMap<Pair<String, String>, DescriptiveStatistics>();

		for (XTrace trace : eventLog) {
			//Evaluate direct pairs of events
			for (int i = 0; i < trace.size() - 1; i++) {
				XEvent firstEvent = trace.get(i);
				XEvent secondEvent = trace.get(i + 1);
				String firstClass = defaultClassifier.getClassIdentity(firstEvent);
				String secondClass = defaultClassifier.getClassIdentity(secondEvent);

				Pair<String, String> eventClassPair = new Pair<String, String>(firstClass, secondClass);
				if (!eventpairStats.containsKey(eventClassPair)) {
					eventpairStats.put(eventClassPair, new DescriptiveStatistics());
				}

				//NOTE: assuming that events are `correctly' ordered!!!
				Date start = timeExt.extractTimestamp(firstEvent);
				Date end = timeExt.extractTimestamp(secondEvent);

				eventpairStats.get(eventClassPair).addValue(end.getTime() - start.getTime());
			}
		}

		return eventpairStats;
	}

	private String calcEventsBeforeAndAfterDealine(
			XLog log, double limit) {
		//Store the information as: (trace,total duration) -> (eventBefore, after Time),(eventAfter, after Time)
		//Map<Pair<String, String>, Pair<Pair<String, String>, Pair<String, String>>> info = new HashMap<Pair<String, String>, Pair<Pair<String, String>, Pair<String, String>>>();

		
		StringBuilder html = new StringBuilder();
		html.append("<html><body>");
		html.append("<table><tr><th>Zaak</th><th>Zaakduur</th><th>Zaakduur (ruw)</th><th>Actie voor deadline</th><th>uitgevoerd na start zaak</th><th>Actie na dealine</th><th>uitgevoerd na start zaak</th></tr>");
		
		//loop through the traces in the log
		for (XTrace trace : log) {
			//First check if this trace takes longer than the limit
			Date traceStart = timeExt.extractTimestamp(trace.get(0));
			Date traceEnd = timeExt.extractTimestamp(trace.get(trace.size() - 1));

			double traceDuration = traceEnd.getTime() - traceStart.getTime();

			if (traceDuration > limit) {
				//Now get to the point in the trace where the limit is exceeded

				for (int i = 1; i < trace.size(); i++) {
					Date eventTime = timeExt.extractTimestamp(trace.get(i));

					//If we exceeded the limit
					if (eventTime.getTime() - traceStart.getTime() > limit) {
						XEvent thisEvent = trace.get(i);
						XEvent previousEvent = trace.get(i - 1);

						Date previousEventTime = timeExt.extractTimestamp(previousEvent);
						Date thisEventTime = timeExt.extractTimestamp(thisEvent);

						//Store the information in the html table
						html.append("<tr>");
						html.append("<td>"+conceptExt.extractName(trace)+"</td>"); //trace name
						html.append("<td>"+new Duration(traceEnd.getTime() - traceStart.getTime())
						.toString()+"</td>"); //trace duration
						html.append("<td>"+decFormat.format(traceEnd.getTime() - traceStart.getTime())+"</td>"); //trace duration raw
						html.append("<td>"+conceptExt.extractName(previousEvent)+"</td>"); //event before
						html.append("<td>"+new Duration(previousEventTime.getTime() - traceStart.getTime())
						.toString()+"</td>"); //after x time
						html.append("<td>"+conceptExt.extractName(thisEvent)+"</td>"); //event after
						html.append("<td>"+new Duration(thisEventTime.getTime() - traceStart.getTime()).toString()+"</td>"); //after x time
						html.append("</tr>");

						//And stop with this trace
						break;
					}
				}
			}
		}
		
		html.append("</table></body></html>");

		return html.toString();
	}

	@SuppressWarnings("unchecked")
	private TreeMap<Pair<String, String>, DescriptiveStatistics> sortDurations(
			Map<Pair<String, String>, DescriptiveStatistics> durations) {
		ValueComparator bvc = new ValueComparator(durations);
		TreeMap<Pair<String, String>, DescriptiveStatistics> sortedDurations = new TreeMap<Pair<String, String>, DescriptiveStatistics>(
				bvc);
		sortedDurations.putAll(durations);
		return sortedDurations;
	}

	private String outputDurationInfo(DescriptiveStatistics stats) {
		StringBuilder html = new StringBuilder();

		html.append("<html><body>");
		html.append("<table><tr><th>Stat</th><th>Value</th></tr>");

		html.append("<tr><td>Mean</td><td>" + new Duration((long) stats.getMean()).toString() + "</td></tr>");
		html.append("<tr><td>Std. Dev.</td><td>" + new Duration((long) stats.getStandardDeviation()).toString()
				+ "</td></tr>");
		html.append("<tr><td>CV</td><td>" + ((stats.getStandardDeviation()) / (stats.getMean())) + "</td></tr>");
		html.append("<tr><td>Quicker than " + deadline + " weeks</td><td>" + checkPercBelow(stats, deadline * week)
				+ "</td></tr>");
		html.append("<tr><td>#Slower than " + deadline + " weeks</td><td>" + checkNrAbove(stats, deadline * week)
				+ "</td></tr>");

		/*-
		html.append("<tr><td>Minimum</td><td>" + new Duration((long) stats.getMin()).toString() + "</td></tr>");
		html.append("<tr><td>Maximum</td><td>" + new Duration((long) stats.getMax()).toString() + "</td></tr>");
		html.append("<tr><td>25 percentile</td><td>" + new Duration((long) stats.getPercentile(0.25)).toString()
				+ "</td></tr>");
		html.append("<tr><td>50 percentile</td><td>" + new Duration((long) stats.getPercentile(0.75)).toString()
				+ "</td></tr>");
		html.append("<tr><td>75 percentile</td><td>" + new Duration((long) stats.getPercentile(0.5)).toString()
				+ "</td></tr>");
		/**/

		html.append("</body></html>");

		System.out.println("INFO: " + new Duration((long) stats.getMean()).toString() + " & "
				+ decFormat.format((stats.getStandardDeviation()) / (stats.getMean())) + " & "
				+ decFormat.format(checkPercBelow(stats, deadline * week))
		//+ new Duration((long) stats.getStandardDeviation()).toString() + " "
		//+ new Duration((long) stats.getMin()).toString() + " "
		//+ new Duration((long) stats.getMax()).toString()
				);

		return html.toString();
	}

	private String outputEventDurationInfo(TreeMap<Pair<String, String>, DescriptiveStatistics> durations) {
		StringBuilder html = new StringBuilder();

		html.append("<html><body>");

		//Now for each combination
		for (Map.Entry<Pair<String, String>, DescriptiveStatistics> entry : durations.entrySet()) {
			//Show combi
			html.append("<h3>" + entry.getKey().getFirst().toString() + " - " + entry.getKey().getSecond().toString()
					+ "</h3>");

			DescriptiveStatistics stats = entry.getValue();

			html.append("<table><tr><th>Stat</th><th>Value</th></tr>");

			html.append("<tr><td>Number</td><td>" + stats.getN() + "</td></tr>");
			html.append("<tr><td>Mean</td><td>" + new Duration((long) stats.getMean()).toString() + "</td></tr>");
			html.append("<tr><td>Std. Dev.</td><td>" + new Duration((long) stats.getStandardDeviation()).toString()
					+ "</td></tr>");
			html.append("<tr><td>CV</td><td>" + ((stats.getStandardDeviation()) / (stats.getMean())) + "</td></tr>");
			html.append("<tr><td>Max</td><td>" + new Duration((long) stats.getMax()).toString() + "</td></tr>");
			html.append("<tr><td>Min</td><td>" + new Duration((long) stats.getMin()).toString() + "</td></tr>");
			//			html.append("<tr><td>Quicker than "+deadline+" weeks</td><td>" + checkPercBelow(stats, deadline * week) + "</td></tr>");
			html.append("</table>");

			/*-
			html.append("<tr><td>Minimum</td><td>" + new Duration((long) stats.getMin()).toString() + "</td></tr>");
			html.append("<tr><td>Maximum</td><td>" + new Duration((long) stats.getMax()).toString() + "</td></tr>");
			html.append("<tr><td>25 percentile</td><td>" + new Duration((long) stats.getPercentile(0.25)).toString()
					+ "</td></tr>");
			html.append("<tr><td>50 percentile</td><td>" + new Duration((long) stats.getPercentile(0.75)).toString()
					+ "</td></tr>");
			html.append("<tr><td>75 percentile</td><td>" + new Duration((long) stats.getPercentile(0.5)).toString()
					+ "</td></tr>");
			/**/

			System.out.println("INFO: " + new Duration((long) stats.getMean()).toString() + " & "
					+ decFormat.format((stats.getStandardDeviation()) / (stats.getMean())) + " & "
					+ decFormat.format(checkPercBelow(stats, deadline * week))
			//+ new Duration((long) stats.getStandardDeviation()).toString() + " "
			//+ new Duration((long) stats.getMin()).toString() + " "
			//+ new Duration((long) stats.getMax()).toString()
					);
		}

		html.append("</body></html>");

		return html.toString();
	}

	/**
	 * Checks the percentage of elements less or eq than the given limit
	 * 
	 * @param stats
	 * @param i
	 * @return
	 */
	private double checkPercBelow(DescriptiveStatistics stats, double limit) {
		int passing = 0;

		for (int i = 0; i < stats.getN(); i++) {
			double el = stats.getElement(i);
			if (el <= limit)
				passing++;
		}

		return (double) passing / stats.getN();
	}

	private int checkNrAbove(DescriptiveStatistics stats, double limit) {
		int above = 0;

		for (int i = 0; i < stats.getN(); i++) {
			double el = stats.getElement(i);
			if (el > limit) {
				above++;
			}
		}

		return above;
	}

	class ValueComparator implements Comparator {

		Map<Pair<String, String>, DescriptiveStatistics> base;

		public ValueComparator(Map<Pair<String, String>, DescriptiveStatistics> base) {
			this.base = base;
		}

		public int compare(Object a, Object b) {
			DescriptiveStatistics aDS = base.get(a);
			DescriptiveStatistics bDS = base.get(b);

			if (aDS.getN() < bDS.getN()) {
				return 1;
			} else if (aDS == bDS) {
				return 0;
			} else {
				return -1;
			}
		}
	}

}
