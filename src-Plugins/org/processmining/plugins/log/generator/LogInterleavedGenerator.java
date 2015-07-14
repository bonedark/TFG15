/**
 * 
 */
package org.processmining.plugins.log.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension.StandardModel;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XAttributeLiteral;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.framework.util.ui.widgets.ProMTextField;

@Plugin(name = "Generate logs permutation", returnLabels = { "Event log" }, returnTypes = { XLog.class }, parameterLabels = { "Activity classes" }, help = "Generate a log, consisting of permutation of event classes.", userAccessible = true)
public class LogInterleavedGenerator {
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "Arya Adriansyah", email = "a.adriansyah@tue.nl", pack = "Replayer")
	@PluginVariant(variantLabel = "Generate trace", requiredParameterLabels = {})
	public XLog createInterleavedLog(final UIPluginContext context) {
		// insert name of activities
		ProMTextField textField = new ProMTextField();

		InteractionResult result = context.showWizard("Insert event classes name", true, true, textField);
		if (result == InteractionResult.FINISHED) {
			return createInterleavedLog(120, textField.getText().split(","));
		} else {
			context.log("Exception occur");
			context.getFutureResult(0).cancel(true);
			return null;
		}

	}

	public static XLog createInterleavedLog(int tracelimit, String... evts) {
		// create a log based on alignment
		XFactory factory = XFactoryRegistry.instance().currentDefault();
		XConceptExtension conceptExtension = XConceptExtension.instance();
		
		XLog log = factory.createLog();

		XTrace trace = factory.createTrace();
		
		Integer counter = 0;
		
		createInterleavedTrace(tracelimit, log, trace, Arrays.asList(evts), counter, factory, conceptExtension);

		return log;

	}

	private static void createInterleavedTrace(int tracelimit, XLog log, XTrace trace, List<String> evts, Integer counter, XFactory factory, XConceptExtension conceptExtension) {

		if (log.size() >= tracelimit) {
			return;
		}

		for (String evt : evts) {
			// select one,
			// add as event
			XEvent e = XFactoryRegistry.instance().currentDefault().createEvent();
			XConceptExtension.instance().assignName(e, evt);
			XLifecycleExtension.instance().assignStandardTransition(e, StandardModel.COMPLETE);
			trace.add(e);
			XAttributeLiteral attribute = factory.createAttributeLiteral("concept:name", String.valueOf(counter), conceptExtension);
			trace.getAttributes().put("concept:name", attribute);
			
			// continue with rest
			List<String> rest = new ArrayList<String>(evts);
			rest.remove(evt);
			counter++;
			createInterleavedTrace(tracelimit, log, trace, rest, counter, factory, conceptExtension);
			trace.remove(e);
		}

		if (evts.isEmpty()) {
			log.add((XTrace) trace.clone());
		}
	}

}
