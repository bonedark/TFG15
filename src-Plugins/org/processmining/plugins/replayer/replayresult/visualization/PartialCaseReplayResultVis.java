/**
 * 
 */
package org.processmining.plugins.replayer.replayresult.visualization;

import javax.swing.JComponent;

import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.plugins.replayer.replayresult.CaseReplayResult;

/**
 * @author aadrians
 * 
 */
@Plugin(name = "Partial Visualize Case Replay Result", returnLabels = { "Partially Visualized Case Replay Result" }, returnTypes = { JComponent.class }, parameterLabels = { "Case Replay Result" }, userAccessible = false)
@Visualizer
public class PartialCaseReplayResultVis {
	@PluginVariant(requiredParameterLabels = { 0 })
	public JComponent visualize(PluginContext context, CaseReplayResult caseReplayResult) {
		System.gc();
		return new CaseRepResultVisPanel(context, caseReplayResult, false);
	}
}
