package org.processmining.plugins.joosbuijs;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.plugins.log.logfilters.LogFilter;
import org.processmining.plugins.log.logfilters.XEventCondition;

public class EventFilter {
	//Concept extension which contains the event name attribute
	private XConceptExtension conceptExtension = XConceptExtension.instance();
	//Event name classifier to look at
	private XEventNameClassifier eventNameClassifier = new XEventNameClassifier();
	//Rename old to new string mapping
	private Set<String> filter = new HashSet<String>();

	@Plugin(
			name = "00JB Event static filter",
				parameterLabels = { "Event log" },
				returnLabels = { "Filtered Event Log" },
				returnTypes = { XLog.class },
				userAccessible = true,
				help = "Performes STATIC event filtering")
	@UITopiaVariant(
			affiliation = "Eindhoven University of Technology",
				author = "J.C.A.M.Buijs",
				email = "j.c.a.m.buijs@tue.nl",
				pack = "JoosBuijs")
	public XLog eventFilter(final PluginContext context, XLog eventLog) {
		XLogInfo info = XLogInfoImpl.create(eventLog, eventNameClassifier);

		final XEventClasses allEventClasses = info.getEventClasses();
		XEventClass[] eventClassesToKeep = { allEventClasses.getByIdentity("administratie"),
				allEventClasses.getByIdentity("Beslissing"), allEventClasses.getByIdentity("Rapportage"),
				allEventClasses.getByIdentity("Aanvraag"), allEventClasses.getByIdentity("Afsluiting werkproces"),
				allEventClasses.getByIdentity("Hoogwaardige intake"),
				allEventClasses.getByIdentity("Beoordeling med. advies"),
				allEventClasses.getByIdentity("Uitkeringsadministratie"),
				allEventClasses.getByIdentity("Eenvoudige intake"),
				allEventClasses.getByIdentity("Intake / advies extern"),
				allEventClasses.getByIdentity("Bouwkundig advies"), allEventClasses.getByIdentity("Medisch advies"),
				allEventClasses.getByIdentity("Passing / offerte Welzorg"),
				allEventClasses.getByIdentity("Toetsing autorisator"),
				allEventClasses.getByIdentity("Opdracht tot uitvoering"),
				allEventClasses.getByIdentity("Beschikking retour consulent"),
				allEventClasses.getByIdentity("Berekening uren HH") };

		/*-XLog filteredLog = context.tryToFindOrConstructFirstNamedObject(XLog.class, "Event Log Filter", "", "",
				eventLog, allEventClasses, eventClassesToKeep);/**/

		// Construct a sorted set of names for easy lookup
		final HashSet<XEventClass> toKeep = new HashSet<XEventClass>(Arrays.asList(eventClassesToKeep));

		return LogFilter.filter(context.getProgress(), 100, eventLog, info, new XEventCondition() {

			public boolean keepEvent(XEvent event) {
				// only keep the event if:
				// 1) its name is in toKeep
				XEventClass c = allEventClasses.getClassOf(event);
				if (!toKeep.contains(c)) {
					return false;
				}
				return true;
			}

		});

	}
}