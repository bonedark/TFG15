/**
 * 
 */
package org.processmining.plugins.flex.replayresult.visualization.performancevis;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.connections.ConnectionManager;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.framework.util.ui.scalableview.interaction.CompoundViewInteractionPanel;
import org.processmining.models.connections.flexiblemodel.AbstractFlexAnalysisInfoConnection;
import org.processmining.models.connections.flexiblemodel.FlexCancellationRegionConnection;
import org.processmining.models.connections.flexiblemodel.FlexCodecConnection;
import org.processmining.models.connections.flexiblemodel.FlexPerfRepInfoConnection;
import org.processmining.models.connections.flexiblemodel.FlexRepResultConnection;
import org.processmining.models.connections.flexiblemodel.FlexStartTaskNodeConnection;
import org.processmining.models.flexiblemodel.CancellationRegion;
import org.processmining.models.flexiblemodel.Flex;
import org.processmining.models.flexiblemodel.StartTaskNodesSet;
import org.processmining.models.graphbased.directed.DirectedGraphElement;
import org.processmining.models.jgraph.visualization.ProMJGraphPanel;
import org.processmining.models.util.ListSelectionPanel;
import org.processmining.plugins.flex.FlexVisualization;
import org.processmining.plugins.flex.analysis.result.FlexAnalysisInformation;
import org.processmining.plugins.flex.replayer.util.FlexCodec;
import org.processmining.plugins.flex.replayresult.performance.CaseInstance;
import org.processmining.plugins.flex.replayresult.performance.FlexPerfRepInfo;

/**
 * @author aadrians
 * 
 */
@Plugin(name = "Visualize Analysis of Flexible model", returnLabels = { "Analysis perspective of Flexible model" }, returnTypes = { JComponent.class }, parameterLabels = { "Flexible model" }, userAccessible = false)
@Visualizer
public class FlexLogReplayPerfResultVis {
	@PluginVariant(requiredParameterLabels = { 0 })
	public JComponent visualize(PluginContext context, Flex flex) {
		System.gc();
		return visualizeWithAll(context, flex);
	}

	private JComponent visualizeWithAll(PluginContext context, Flex flex) {
		// visualize Flexible model as usual
		FlexVisualization flexVisualization = new FlexVisualization();
		ProMJGraphPanel panel = (ProMJGraphPanel) flexVisualization.visualize(context, flex);

		ConnectionManager conn = context.getConnectionManager();

		// get additional stuffs
		FlexStartTaskNodeConnection startTaskNodeConn = null;
		StartTaskNodesSet startTaskNodesSet = null;
		try {
			startTaskNodeConn = conn.getFirstConnection(FlexStartTaskNodeConnection.class, context, flex);
			startTaskNodesSet = (StartTaskNodesSet) startTaskNodeConn
					.getObjectWithRole(FlexStartTaskNodeConnection.STARTTASKNODES);
		} catch (ConnectionCannotBeObtained e1) {
			return new JLabel("No start task nodes set can be found. No conformance and performance view available");
		}

		FlexCodec codec = null;
		try {
			FlexCodecConnection codecConn = conn.getFirstConnection(FlexCodecConnection.class, context, flex);
			codec = (FlexCodec) codecConn.getObjectWithRole(FlexCodecConnection.FLEXCODEC);
		} catch (ConnectionCannotBeObtained e1) {
			// no codec
		}

		CancellationRegion cancellationRegion = null;
		try {
			FlexCancellationRegionConnection cancelConn = conn.getFirstConnection(FlexCancellationRegionConnection.class, context,
					flex);
			cancellationRegion = cancelConn.getObjectWithRole(FlexCancellationRegionConnection.CANCELLATIONREGION);
		} catch (ConnectionCannotBeObtained e1) {
			// no codec
		}

		// add conformance view if exists
		try {
			conn.getFirstConnection(FlexRepResultConnection.class, context,
					flex, startTaskNodesSet);

//			FlexRepResultConnection repResultConn = conn.getFirstConnection(FlexRepResultConnection.class, context,
//					flex, startTaskNodesSet);
//			FlexRepResult repResult = (FlexRepResult) repResultConn
//					.getObjectWithRole(FlexRepResultConnection.FLEXREPRESULT);

			CompoundViewInteractionPanel conformance = new CompoundViewInteractionPanel("Conformance");

			ListSelectionPanel view = new ListSelectionPanel("Conformance panel", "content", false);
			view.addElementCollection(Collections.<DirectedGraphElement>emptySet(), "conformance information test");
			conformance.addViewInteractionPanel(view);

			if (conformance.length() > 0) {
				panel.addViewInteractionPanel(conformance, SwingConstants.SOUTH);
			}
		} catch (ConnectionCannotBeObtained e) {
			// no conformance information yet
		}

		// Check for available analysis information
		List<FlexAnalysisInformation<?>> infoList = new ArrayList<FlexAnalysisInformation<?>>();
		// First structural analysis (no marking needed)
		ConnectionManager cm = context.getConnectionManager();
		try {
			for (AbstractFlexAnalysisInfoConnection analysis : cm.getConnections(
					AbstractFlexAnalysisInfoConnection.class, context, flex)) {
				FlexAnalysisInformation<?> info = (FlexAnalysisInformation<?>) analysis
						.getObjectWithRole(AbstractFlexAnalysisInfoConnection.ANALYSISINFORMATION);
				infoList.add(info);
			}
		} catch (ConnectionCannotBeObtained e) {
			// No connections available
		}
		if (!infoList.isEmpty()) {
			ListSelectionPanel view = new ListSelectionPanel("Properties", "Analysis properties", false);
			for (FlexAnalysisInformation<?> info : infoList) {
				view.addElementCollection(Collections.<DirectedGraphElement>emptySet(),
						info.getLabel() + ": " + info.getValue());
			}
			CompoundViewInteractionPanel properties = new CompoundViewInteractionPanel("Analysis");
			properties.addViewInteractionPanel(view);
			panel.addViewInteractionPanel(properties, SwingConstants.SOUTH);
		}
		infoList.clear();
		
		// add performance view if exists
		try {
			FlexPerfRepInfoConnection flexPerfConn = conn.getFirstConnection(FlexPerfRepInfoConnection.class, context,
					flex, startTaskNodesSet);
			FlexPerfRepInfo perfResult = (FlexPerfRepInfo) flexPerfConn
					.getObjectWithRole(FlexPerfRepInfoConnection.FLEXPERFREPINFO);

			// take also the mapping from node instances to its lifecycle
			CompoundViewInteractionPanel performance = new CompoundViewInteractionPanel("Performance");

			FlexNodePerfSelectionPanel view = new FlexNodePerfSelectionPanel("Projection",
					"Choose projection", flex, codec, perfResult, cancellationRegion, panel.getGraph());
			
			// add metrics view
			ListSelectionPanel metricsView = new ListSelectionPanel("Metrics", "Performance metrics", false);
			CaseInstance caseInstance = perfResult.getCaseInstance();
			NumberFormat nf = NumberFormat.getInstance();
			nf.setMinimumFractionDigits(3);
			nf.setMaximumFractionDigits(3);
			metricsView.addElementCollection(Collections.<DirectedGraphElement>emptySet(), "Num. of cases : " + caseInstance.getFrequency());
			metricsView.addElementCollection(Collections.<DirectedGraphElement>emptySet(), "Avg. throughput (case) : " + getSuitableTimeStr(caseInstance.getAvgTimeSpan()));
			metricsView.addElementCollection(Collections.<DirectedGraphElement>emptySet(), "Min. throughput (case) : " + getSuitableTimeStr(caseInstance.getMinTimeSpan()));
			metricsView.addElementCollection(Collections.<DirectedGraphElement>emptySet(), "Max. throughput (case) : " + getSuitableTimeStr(caseInstance.getMaxTimeSpan()));
			metricsView.addElementCollection(Collections.<DirectedGraphElement>emptySet(), "Std. deviation throughput (case) : " + getSuitableTimeStr(caseInstance.getStdDevTimeSpan()));			
			
			performance.addViewInteractionPanel(view);
			performance.addViewInteractionPanel(metricsView);
			
			if (performance.length() > 0) {
				panel.addViewInteractionPanel(performance, SwingConstants.SOUTH);
			}

		} catch (ConnectionCannotBeObtained e) {
			// no performance information yet
		}

		return panel;
	}
	
	private String getSuitableTimeStr(double avgMoveTime) {
		NumberFormat nf = NumberFormat.getInstance();
		if (Double.compare(avgMoveTime, 1000 * 60 * 60 * 24) > 0){
			// days...
			return (nf.format(avgMoveTime / (1000 * 60 * 60 * 24)) + " days");
		} else if (Double.compare(avgMoveTime, 1000 * 60 * 60) > 0){
			// hours
			return (nf.format(avgMoveTime / (1000 * 60 * 60)) + " hours");
		} else if (Double.compare(avgMoveTime, 1000 * 60) > 0){
			// minutes
			return (nf.format(avgMoveTime / (1000 * 60)) + " min");
		} else if (Double.compare(avgMoveTime, 1000) > 0){
			// seconds
			return (nf.format(avgMoveTime / (1000)) + " sec");
		} 
		return (avgMoveTime + " ms");
	}
}
