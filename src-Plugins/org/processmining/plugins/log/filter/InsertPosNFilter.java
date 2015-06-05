/**
 * 
 */
package org.processmining.plugins.log.filter;

import javax.swing.JComboBox;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.framework.util.ui.widgets.ProMPropertiesPanel;

/**
 * @author aadrians
 * Dec 1, 2011
 *
 */
@Plugin(name = "Insert an activity to the n-th index", returnLabels = { "Filtered event log" }, returnTypes = { XLog.class }, parameterLabels = {
		"Event Log", "N-value", "Event class" }, help = "Remove the N-th event from all traces.", userAccessible = true)
public class InsertPosNFilter {
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "Arya Adriansyah", email = "a.adriansyah@tue.nl", pack = "Replayer")
	@PluginVariant(variantLabel = "From Event Log only", requiredParameterLabels = { 0 })
	public XLog filterLogByRemovingNthEvent(final UIPluginContext context, XLog log){
		// assuming the traces are with the same length
		XLogInfo logInfo = XLogInfoFactory.createLogInfo(log, XLogInfoImpl.NAME_CLASSIFIER);
		XEventClasses classes = logInfo.getEventClasses();
		
		// find the longest trace
		int maxSize = 0;
		for (XTrace trace : log){
			int sizeTrace = trace.size();
			maxSize = maxSize < sizeTrace ? sizeTrace : maxSize;
		}
		Object[] indexCounter = new Object[maxSize+1];
		for (int i=0; i <= maxSize; i++){
			indexCounter[i] = i;
		}
		
		ProMPropertiesPanel mainPanel = new ProMPropertiesPanel("Set values");
		JComboBox evClassCb = mainPanel.addComboBox("Inserted event class", classes.getClasses().toArray());
		JComboBox positionCb = mainPanel.addComboBox("Insert in position", indexCounter);
		
		InteractionResult result = context.showWizard("Set removed index", true, true, mainPanel);
		if (result == InteractionResult.FINISHED){
			return filterLogByInsertingNthEvent(context, log, positionCb.getSelectedIndex(), (XEventClass) evClassCb.getSelectedItem());
		} else {
			context.log("Exception occur");
			context.getFutureResult(0).cancel(true);
			return null;
		}
		
	}
	
	@PluginVariant(variantLabel = "From Event Log and N value", requiredParameterLabels = { 0, 1 })
	public XLog filterLogByInsertingNthEvent(final PluginContext context, XLog log, Integer n, XEventClass eventClass){
		// create new log
		XFactory factory = XFactoryRegistry.instance().currentDefault();
		XConceptExtension conceptExtension = XConceptExtension.instance();

		// result
		XLog outputLog = factory.createLog();
		
		// copy all log information
		outputLog.setAttributes(log.getAttributes());
		
		for (XTrace trace : log){
			XTrace newTrace = ((XTrace) trace.clone());
			if (newTrace.size() >= n){
				XEvent outputEvent = factory.createEvent();
				XAttribute name = factory.createAttributeLiteral("concept:name", eventClass.toString(), conceptExtension);
				outputEvent.getAttributes().put("concept:name", name);
				
				XAttribute transition = factory.createAttributeLiteral("lifecycle:transition", "complete", conceptExtension);
				outputEvent.getAttributes().put("lifecycle:transition", transition);
				
				newTrace.add(n, outputEvent);
			}
			outputLog.add(newTrace);
		}
		context.getFutureResult(0).setLabel(XConceptExtension.instance().extractName(log) + " after insertion idx-" + n.intValue());
		return outputLog;
		
	}
}
