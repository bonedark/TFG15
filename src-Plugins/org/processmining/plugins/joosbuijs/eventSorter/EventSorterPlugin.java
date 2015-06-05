package org.processmining.plugins.joosbuijs.eventSorter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.util.Pair;

public class EventSorterPlugin {
	private static final Integer DEPENDENCY_THRESHOLD = 5;
	private XEventNameClassifier eventNameClassifier = new XEventNameClassifier();
	private XConceptExtension conceptExtension = XConceptExtension.instance();
	private XTimeExtension timeExtension = XTimeExtension.instance();
	private XEventClasses classes;
	private Map<Pair<XEventClass, XEventClass>, Integer> direct;
	private TreeMap<Integer, HashSet<Pair<XEventClass, XEventClass>>> sortedDirect = new TreeMap<Integer, HashSet<Pair<XEventClass, XEventClass>>>();

	@Plugin(
			name = "00JB Event Sorting",
				parameterLabels = { "Event log" },
				returnLabels = { "Sorted Event Log" },
				returnTypes = { XLog.class },
				userAccessible = true,
				help = "Performes resorting of events with equal timestamps based on the order found for events with different timestamps")
	@UITopiaVariant(
			affiliation = "Eindhoven University of Technology",
				author = "J.C.A.M.Buijs",
				email = "j.c.a.m.buijs@tue.nl", pack = "JoosBuijs")
	public XLog eventSorterPlugin(final PluginContext context, XLog eventLog) {
		//Show a progress bar!
		context.getProgress().setMinimum(0);
		context.getProgress().setMaximum(eventLog.size());

		//We will work on a copy of the input event log
		XLog sortedEventLog = (XLog) eventLog.clone();

		//We need some info from it...
		XLogInfo info = XLogInfoImpl.create(eventLog, eventNameClassifier);
		classes = info.getEventClasses();

		//use our log relations that ignores dep. in clusters
		IgnoreEqualTimeLogRelations timeIgnoredRelations = new IgnoreEqualTimeLogRelations(eventLog, info,
				context.getProgress());
		direct = timeIgnoredRelations.getDirectFollowsDependencies();

		//Use the normal log relations, that also uses clusters to find dep.
		//		BasicLogRelations basicLogRelations = new BasicLogRelations(eventLog, info, context.getProgress());
		//		direct = basicLogRelations.getDirectFollowsDependencies();

		sortDirect();
		printSortedDirectMap();

		//printDirectMap(direct);

		//For each trace
		for (XTrace trace : sortedEventLog) {
			String traceID = conceptExtension.extractName(trace);

			if (trace.size() <= 1) {
				System.out.println("Trace " + traceID + " has only 1 event (or none), so no cluster here.");
			} else {
				//Cycle through the events from beginning to second-last
				int i = 0;
				while (i < trace.size() - 1) {
					//Check if we found the beginning of a cluster
					if (compareTimestamp(trace.get(i), trace.get(i + 1))) {
						HashSet<XEventClass> classesInCluster = new HashSet<XEventClass>();
						classesInCluster.add(classes.getClassOf(trace.get(i)));
						classesInCluster.add(classes.getClassOf(trace.get(i + 1)));
						int clusterStart = i;
						i++; //move i to second event

						//find out how long the cluster lasts
						while ((i < trace.size() - 1) && compareTimestamp(trace.get(clusterStart), trace.get(i + 1))) {
							classesInCluster.add(classes.getClassOf(trace.get(i)));
							i++; //move to the next event
						}
						//clusterEnd is the first index NOT belonging to the cluster (which is i)
						int clusterEnd = i + 1;

						//Now loop through our ordered direct dependencies
						for (Map.Entry<Integer, HashSet<Pair<XEventClass, XEventClass>>> entry : sortedDirect
								.entrySet()) {
							//ignore minor dependencies
							if (entry.getKey() >= DEPENDENCY_THRESHOLD) {
								//Cycle through the pairs in this dependency
								Iterator<Pair<XEventClass, XEventClass>> it = entry.getValue().iterator();
								while (it.hasNext()) {
									Pair<XEventClass, XEventClass> pair = it.next();

									//check if both event classes are present in our cluster
									if (classesInCluster.contains(pair.getFirst())
											&& classesInCluster.contains(pair.getSecond())) {
										XEvent secondEvent = null;
										XEvent firstEvent = null;
										int indexOfFirstEvent = -1;

										//We do it the safe way, we first find our events

										//CUT the second event from the trace
										for (int j = clusterStart; j < clusterEnd; j++) {
											int comparison = classes.getClassOf(trace.get(j)).compareTo(
													pair.getSecond());
											if (comparison == 0) {
												secondEvent = trace.get(j); //get
												j=clusterEnd;
											}
										}

										//Find the first event in the cluster
										for (int j = clusterStart; j < clusterEnd; j++) {
											int comparison = classes.getClassOf(trace.get(j))
													.compareTo(pair.getFirst());
											if (comparison == 0) {
												firstEvent = trace.get(j);
												indexOfFirstEvent = j;
												//trace.add(j + 1, secondEvent);//and add the second event behind it
												j = clusterEnd; //force stop for-loop
											}
										}//end for first event loop

										//Now check if we found our events
										if (firstEvent != null && secondEvent != null && indexOfFirstEvent >= 0) {
											//And move the 2nd event behind the 1st
											trace.remove(secondEvent);
											if (indexOfFirstEvent == trace.size())
												trace.add(secondEvent);
											else
												trace.add(indexOfFirstEvent + 1, secondEvent);
											//System.out.println("We moved an event!");
										} else {
											//System.out.println("We had null events so did nothing...");
										}

									}//end if classes of pair in cluster
								}//end while new pairs
							}//end if dependencyCount over threshold
						}//end for dependencies (sorted)
					} //end if 2 events eq timestamp 
					else {
						i++;
					}
				}
				//we finished a trace, notify user
				context.getProgress().inc();
			} //trace count
		}//trace

		System.out.println("Event clusters in SORTED event log:");
		printEventClusters(sortedEventLog);
		
		return sortedEventLog;
	}

	/**
	 * Uses the direct relations and sorts them on count, ASCENDING
	 * 
	 * @return
	 */
	private void sortDirect() {
		for (Map.Entry<Pair<XEventClass, XEventClass>, Integer> entry : direct.entrySet()) {
			//If this nr. of occur. is already there, add the pair to the set
			if (sortedDirect.containsKey(entry.getValue())) {
				sortedDirect.get(entry.getValue()).add(entry.getKey());
			} else {
				//Add the key and set
				HashSet<Pair<XEventClass, XEventClass>> set = new HashSet<Pair<XEventClass, XEventClass>>();
				set.add(entry.getKey());
				sortedDirect.put(entry.getValue(), set);
			}
		}
	}

	private void printCluster(HashSet<XEvent> cluster) {
		Iterator<XEvent> it = cluster.iterator();
		String clusterStr = "";

		while (it.hasNext()) {
			clusterStr += conceptExtension.extractName(it.next()) + ", ";
		}

		System.out.println("Cluster: " + clusterStr);
	}

	private void printSortedEvents(LinkedList<XEvent> sortedEvents) {
		String str = "";
		Iterator<XEvent> it = sortedEvents.iterator();

		while (it.hasNext()) {
			str += conceptExtension.extractName(it.next()) + ", ";
		}

		System.out.println("Sorted: " + str);
	}

	private void printDirectMap(Map<Pair<XEventClass, XEventClass>, Integer> direct) {
		System.out.println("Map contents:");
		for (Map.Entry<Pair<XEventClass, XEventClass>, Integer> entry : direct.entrySet()) {
			Pair<XEventClass, XEventClass> pair = entry.getKey();
			System.out.println(pair.toString() + " " + entry.getValue());
		}
	}

	private void printSortedDirectMap() {
		System.out.println("Sorted Map contents:");
		for (Map.Entry<Integer, HashSet<Pair<XEventClass, XEventClass>>> entry : sortedDirect.entrySet()) {
			if (entry.getKey() > 0) {
				String set = "";
				Iterator<Pair<XEventClass, XEventClass>> itr = entry.getValue().iterator();
				while (itr.hasNext()) {
					set += itr.next();

				}
				System.out.println(entry.getKey() + ": " + set);
			}
		}

	}

	/**
	 * Prints the events that are 'clustered' on the same date
	 * 
	 * @param log
	 */
	private void printEventClusters(XLog log) {
		//Build clusters
		Map<String, Integer> clusters = new HashMap<String, Integer>();
		for (XTrace trace : log) {
			if (trace.size() > 1) {
				//Cycle through the events from beginning to second-last
				int i = 0;
				while (i < trace.size() - 1) {
					//Check if we found the beginning of a cluster
					if (compareTimestamp(trace.get(i), trace.get(i + 1))) {
						XEvent firstEvent = trace.get(i);
						XEvent secondEvent = trace.get(i + 1);
						//Start the cluster name
						String clusterEvents = conceptExtension.extractName(firstEvent) + "-"
								+ conceptExtension.extractName(secondEvent);
						i++; //move i to second event
						while ((i < trace.size() - 1) && compareTimestamp(firstEvent, trace.get(i + 1))) {
							//append cluster name
							clusterEvents += "-" + conceptExtension.extractName(trace.get(i + 1));
							i++; //move to the next event
						}
						//count
						int n = clusters.containsKey(clusterEvents) ? clusters.get(clusterEvents) : 0;
						clusters.put(clusterEvents, n + 1);
					} else {
						i++; // move on
					}
				}
			}
		}
		
		//Now sort
		TreeMap<Integer, HashSet<String>> sortedClusters = new TreeMap<Integer, HashSet<String>>();
		
		for(Entry<String, Integer> entry : clusters.entrySet())
		{
				//If this nr. of occur. is already there, add the pair to the set
			if (sortedClusters.containsKey(entry.getValue())) {
				sortedClusters.get(entry.getValue()).add(entry.getKey());
			} else {
				//Add the key and set
				HashSet<String> set = new HashSet<String>();
				set.add(entry.getKey());
				sortedClusters.put(entry.getValue(), set);
			}
		}

		System.out.println("Clusters:");
		for (Map.Entry<Integer, HashSet<String>> entry : sortedClusters.entrySet()) {
			System.out.println(entry.getKey() + " occurences of: ");
			for(String string : entry.getValue())
			{
				System.out.println("    " + string);
			}
		}
	}

	private boolean compareTimestamp(XEvent event1, XEvent event2) {
		try {
			return timeExtension.extractTimestamp(event1).equals(timeExtension.extractTimestamp(event2));
		} catch (Exception e) {
			e.toString();
		}
		return false;
	}
}
