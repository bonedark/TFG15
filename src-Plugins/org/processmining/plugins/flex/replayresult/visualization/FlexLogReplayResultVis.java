/**
 * 
 */
package org.processmining.plugins.flex.replayresult.visualization;

import javax.swing.JComponent;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.connections.flexiblemodel.FlexCodecConnection;
import org.processmining.models.connections.flexiblemodel.FlexRepResultConnection;
import org.processmining.models.flexiblemodel.Flex;
import org.processmining.plugins.flex.replayer.util.FlexCodec;
import org.processmining.plugins.flex.replayresult.FlexRepResult;
import org.processmining.plugins.flex.replayresult.visualization.conformancevis.FlexLogReplayResultVisPanel;

/**
 * @author Arya Adriansyah
 * @email a.adriansyah@tue.nl
 * @version Feb 15, 2011
 */
@Plugin(name = "Visualize Flexible model Log Replay Result", returnLabels = { "Visualized Log Replay Result" }, returnTypes = { JComponent.class }, parameterLabels = { "Log Replay Result" }, userAccessible = false)
@Visualizer
public class FlexLogReplayResultVis {
	@PluginVariant(requiredParameterLabels = { 0 })
	public JComponent visualize(PluginContext context, FlexRepResult logReplayResult) {
		System.gc();
		Flex flex = null;
		XLog log = null;
		try {
			FlexRepResultConnection conn = context.getConnectionManager().getFirstConnection(FlexRepResultConnection.class, context, logReplayResult);
			flex = conn.getObjectWithRole(FlexRepResultConnection.FLEX);
			log = conn.getObjectWithRole(FlexRepResultConnection.LOG);
		} catch (Exception exc){
			// no flex
			context.log("Fail to visualize as the original Flexible model is removed");
			return null;
		}
		
		FlexCodec codec = null; 
		try {
			codec = context.tryToFindOrConstructFirstObject(FlexCodec.class, FlexCodecConnection.class, FlexCodecConnection.FLEXCODEC, flex);
		} catch (Exception exc){
			// no codec
			context.log("Fail to visualize as the original codec is removed");
			return null;
		}
		
		return new FlexLogReplayResultVisPanel(flex, log, logReplayResult, codec, context.getProgress());
	}
}
