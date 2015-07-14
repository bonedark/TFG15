/**
 * 
 */
package org.processmining.plugins.log.filter;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;

import com.fluxicon.slickerbox.components.NiceIntegerSlider;
import com.fluxicon.slickerbox.components.NiceSlider.Orientation;
import com.fluxicon.slickerbox.factory.SlickerFactory;

/**
 * @author aadrians
 * Nov 29, 2011
 *
 */
@Plugin(name = "Remove the n-th events from all traces", returnLabels = { "Filtered event log" }, returnTypes = { XLog.class }, parameterLabels = {
		"Event Log", "N-value" }, help = "Remove the N-th event from all traces.", userAccessible = true)
public class RemovePosNFilter {
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "Arya Adriansyah", email = "a.adriansyah@tue.nl", pack = "Replayer")
	@PluginVariant(variantLabel = "From Event Log", requiredParameterLabels = { 0 })
	public XLog filterLogByRemovingNthEvent(final UIPluginContext context, XLog log){
		// find the longest trace
		int maxSize = 0;
		for (XTrace trace : log){
			int sizeTrace = trace.size();
			maxSize = maxSize < sizeTrace ? sizeTrace : maxSize;
		}
		
		SlickerFactory factory = SlickerFactory.instance();
		NiceIntegerSlider slider = factory.createNiceIntegerSlider("Which index need to be removed?", 0, maxSize-1, 0, Orientation.HORIZONTAL);
		
		InteractionResult result = context.showWizard("Set removed index", true, true, slider);
		if (result == InteractionResult.FINISHED){
			return filterLogByRemovingNthEvent(context, log, slider.getValue());
		} else {
			context.log("Exception occur");
			context.getFutureResult(0).cancel(true);
			return null;
		}
		
	}
	
	@PluginVariant(variantLabel = "From Event Log and N value", requiredParameterLabels = { 0, 1 })
	public XLog filterLogByRemovingNthEvent(final PluginContext context, XLog log, Integer n){
		// create new log
		// create a log based on alignment
		XFactory factory = XFactoryRegistry.instance().currentDefault();

		// result
		XLog outputLog = factory.createLog();
		
		// copy all log information
		outputLog.setAttributes(log.getAttributes());
		
		for (XTrace trace : log){
			XTrace newTrace = ((XTrace) trace.clone());
			if (n.intValue() < trace.size()){
				newTrace.remove(n.intValue());
			}
			outputLog.add(newTrace);
		}
		context.getFutureResult(0).setLabel(XConceptExtension.instance().extractName(log) + "(after idx-" + n.intValue() + " removed)");
		return outputLog;
		
	}
}
