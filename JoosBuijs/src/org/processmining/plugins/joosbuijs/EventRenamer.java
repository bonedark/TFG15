package org.processmining.plugins.joosbuijs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.plugins.log.logfilters.LogFilter;
import org.processmining.plugins.log.logfilters.LogFilterException;
import org.processmining.plugins.log.logfilters.XEventEditor;

import uk.ac.shef.wit.simmetrics.similaritymetrics.AbstractStringMetric;
import uk.ac.shef.wit.simmetrics.similaritymetrics.Levenshtein;

public class EventRenamer {
	//threshold above which event names are renamed
	private static final float renameThresholdRelative = new Float(0.8);
	private static final float renameThresholdAbsolute = new Float(4);

	//Concept extension which contains the event name attribute
	private XConceptExtension conceptExtension = XConceptExtension.instance();
	//Event name classifier to look at
	private XEventNameClassifier eventNameClassifier = new XEventNameClassifier();
	//Rename old to new string mapping
	private Map<String, String> renamings = new HashMap<String, String>();

	@Plugin(
			name = "00JB Event Renaming WABO_SSC",
				parameterLabels = { "Event log" },
				returnLabels = { "Normalized Event Log" },
				returnTypes = { XLog.class },
				userAccessible = true,
				help = "Performes STATIC renaming of event concept:name attributes for WABO SSC analysis")
	@UITopiaVariant(
			affiliation = "Eindhoven University of Technology",
				author = "J.C.A.M.Buijs",
				email = "j.c.a.m.buijs@tue.nl",
				pack = "JoosBuijs")
	public XLog eventRenamer(final PluginContext context, XLog eventLog) throws LogFilterException {
		//Calculate renamings
		initializeRenamingsMap(eventLog);

		//Show a progress bar!
		context.getProgress().setMinimum(0);
		context.getProgress().setMaximum(eventLog.size());

		return LogFilter.filter(eventLog, new XEventEditor() {

			public XEvent editEvent(XEvent event) {
				//Reading the event attributes we are going to normalize
				XEvent newEvent = (XEvent) event.clone();
				String eventName = conceptExtension.extractName(newEvent);

				//Lookup the new event name
				String newEventName = normalizeEventNames(eventName);

				//Set it
				conceptExtension.assignName(newEvent, newEventName);

				//Update progress
				context.getProgress().inc();
				return newEvent;
			}
		});
	}

	/**
	 * Returns a normalized version of the event name
	 * 
	 * @param eventName
	 * @return
	 */
	protected String normalizeEventNames(String eventName) {
		if (renamings.containsKey(eventName))
			eventName = renamings.get(eventName);

		return eventName;
	}

	/**
	 * Loops through all the event names, compares them and maps names that are
	 * similar
	 */
	private void initializeRenamingsMap(XLog eventLog) {

		//		XLogInfo info = XLogInfoImpl.create(eventLog, eventNameClassifier);

		//		XEventClasses eventClasses = info.getEventClasses();
		//		ArrayList<XEventClass> sortedEventClasses = sortEvents(eventClasses);
		//		ArrayList<XEventClass> sortedEventClasses2 = (ArrayList<XEventClass>) sortedEventClasses.clone();

		/*-
		 * Possible metrics to test are:
		 * -block
		 * -Jaccard
		 * -Euclidean
		 * -Cosine?
		 * -q-gram
		 * - Levenshtein?
		 */
		//Initialize string metric
		AbstractStringMetric metric = new Levenshtein();

		/*-* /
		//Loop through event classes (ordered by count)
		for (XEventClass mainEventClass : sortedEventClasses) {
			//then compare to less-used event classes
			for (XEventClass lowerEventClass : sortedEventClasses2)
				//Check if we have a lesser used event class
				if (lowerEventClass.size() < mainEventClass.size()) {
					//Compare event name similiarity to a threshold
					float comparison = metric.getSimilarity(lowerEventClass.toString(), mainEventClass.toString());
					float comparisonAbsolute = metric.getUnNormalisedSimilarity(lowerEventClass.toString(),
							mainEventClass.toString());
					if (comparison >= renameThresholdRelative) {
						//if (comparisonAbsolute <= renameThresholdAbsolute && comparisonAbsolute > 0) {
						//if comparison > threshold, rename lesser used to more used class
						renamings.put(lowerEventClass.toString(), mainEventClass.toString());
						System.out.println("Renaming " + lowerEventClass.toString() + " to "
								+ mainEventClass.toString());
					}
				}
		}
		/**/

		/*-* /
		//Bergeijk WABO log
		renamings.put("aanvraag compleet 2", "Aanvraag volledig");
		renamings.put("aanvraag compleet", "Aanvraag volledig");
		renamings.put("Activiteiten reguliere procedure 1", "Activiteiten reguliere procedure");
		renamings.put("Beeindigen op verzoek", "Beëindigen op verzoek");
		renamings.put("einde proces", "Einde proces");
		renamings.put("melding compleet", "Melding compleet");
		renamings.put("Monumentenvergunning nodig", "Monumentenvergunning vereist");
		renamings.put("ontvangst aanvraag", "Ontvangst aanvraag");
		renamings.put("ontvangst melding", "Ontvangst melding");
		renamings.put("ontvangstbevestiging versturen", "Ontvangstbevestiging versturen");
		renamings.put("registreren ontvangst", "Registreren ontvangst");
		renamings.put("vergunningplichtig", "Vergunningplichtig");
		renamings.put("Waw vergunning aspect 1", "Waw vergunningsaspect 1");
		/**/

		/*-* /
		//Emmen Bouw log voor Kempen vergelijking
		//First fix some typos
		renamings.put("Aanmaken ontvangsbevestiging","Aanmaken ontvangstbevestiging");
		renamings.put("Procedure volledigheid verwijderen.", "Procedure volledigheid verwijderen");
		//And merge some mut.ex. events with slightly different names
		renamings.put("Aanmaken ontvangstbevestiging","Verzenden ontvangstbevestiging");
		renamings.put("Controle op volledigheid","Controle volledigheid");
		
		/*-
		//Then try to map the Emmen names to the Kempen names... TRICKY!
		//renamings.put("Registreren documenten","Ontvangst aanvraag");
		//renamings.put("Verzenden ontvangstbevestiging","Verzenden ontvangstbevestiging");
		renamings.put("Toets volledigheid","Ontvankelijkheidstoets");
		renamings.put("Vastleggen resultaat toets bouwverorden","Bouwverordeningtoets toepassen");
		renamings.put("Toetsing aan bestemmingsplan","Bestemmingsplantoets toepassen");
		renamings.put("Aanvragen advies welstand","Welstand toepassen");
		renamings.put("Verzenden beschikking","Besluit datum besluit");
		renamings.put("Bepalen aanhoudingsplicht","Aanhouding van toepassing");
		/**/

		/*- * /
		//WMO ALLE logs
		renamings.put("Aanmelding","Aanvraag"); //Hld->Zwl
		renamings.put("Adm. afhandeling","administratie"); //Zwl->ISD
		renamings.put("Administratieve verwerking","Administratie"); //Hld->ISD
		renamings.put("Advies","ADVIES");
		renamings.put("Archief","Archivering");
		renamings.put("beslissen","Beslissing"); //ISD (correct?)
		renamings.put("Besluit","Beslissing"); //ISD+Coevorden->ISD
		renamings.put("NIET IN BEHANDELING nemen","Niet in behandeling nemen");
		renamings.put("rapportage","Rapportage"); //ISD
		renamings.put("retour van toetser","Retour van toetser"); //ISD
		renamings.put("toetsing","Toetsing"); //ISD
		renamings.put("verzenden beschikking","Verzenden beschikking");

		/*-*/
		//WMO Hld mappen op ISD model
		//onderzoeken
		renamings.put("Onderzoek adviseur", "Onderzoek");
		renamings.put("onderzoek loket", "Onderzoek");
		renamings.put("Onderzoek/rapportage", "Onderzoek");
		//rapportage
		renamings.put("Eindrapportage adviseur", "Rapportage");
		renamings.put("eindrapportage loket", "Rapportage");
		//renamings.put("",""); //loket variant heeft 1 taak onderzoek/rapportage... geen mapping naar rapportage, FOUT!
		//besluiten
		renamings.put("Toekennen", "Beslissing");
		renamings.put("Afwijzen", "Beslissing");
		renamings.put("Niet in behandeling nemen", "Beslissing");
		renamings.put("Overige besluiten", "Beslissing");
		/**/
		//Small Zwolle remap to ISD
		renamings.put("Intake / advies extern", "Extern advies");
		renamings.put("Medisch advies", "Extern advies");

		//renamings.put("","");

	}

	/**
	 * Sorts the given event classes from high to low occurrence. From package
	 * org.processmining.plugins.log.ui.logdialog.LogInfoUI
	 * 
	 * @param eventClasses
	 *            The given event classes.
	 * @return The sorted event classes.
	 */
	private ArrayList<XEventClass> sortEvents(XEventClasses eventClasses) {
		ArrayList<XEventClass> sortedEvents = new ArrayList<XEventClass>();
		for (XEventClass event : eventClasses.getClasses()) {
			boolean inserted = false;
			XEventClass current = null;
			for (int i = 0; i < sortedEvents.size(); i++) {
				current = sortedEvents.get(i);
				if (current.size() < event.size()) {
					// insert at correct position and set marker
					sortedEvents.add(i, event);
					inserted = true;
					break;
				}
			}
			if (inserted == false) {
				// append to end of list
				sortedEvents.add(event);
			}
		}
		return sortedEvents;
	}
}