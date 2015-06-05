/**
 * 
 */
package org.processmining.plugins.flex.replayer.algorithms;

import java.util.Collection;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.util.Pair;
import org.processmining.models.flexiblemodel.EndTaskNodesSet;
import org.processmining.models.flexiblemodel.Flex;
import org.processmining.models.flexiblemodel.FlexNode;
import org.processmining.models.flexiblemodel.StartTaskNodesSet;
import org.processmining.plugins.flex.replayer.util.FlexCodec;
import org.processmining.plugins.flex.replayresult.FlexRepResult;
import org.processmining.plugins.replayer.replayresult.CaseReplayResult;

/**
 * @author aadrians
 * 
 */
public interface IFlexLogReplayAlgorithm {
	// REFERENCE FOR replayLog RESULT
	public static int FLEXREPRESULT = 0;
	public static int CODEC = 0;
	
	// STATIC FOR PARAMETERS
	public static int USEMAXNUMOFSTATES = 0;
	public static int USEMAXESTIMATED = 1;
	public static int ANALYSISTYPE = 2;
	public static int OUTPUTFILE = 3;
	
	// STATIC FOR ANALYSIS TYPE OF replayLogForAnalysis
	public static int PREFIXANALYSIS = 0;
	public static int REMOVEEVENTANALYSIS = 1;
	public static int INVERTPREFIXANALYSIS = 2;

	public String toString();	// this is used in comboBox of algorithm selection

	/**
	 * Method to replay a whole log and check for its conformance
	 * @param context
	 * @param flex
	 * @param startTaskNodesSet
	 * @param log
	 * @param mapping
	 * @param parameters
	 * @return
	 */
	public FlexRepResult replayLog(PluginContext context, Flex flex, FlexCodec codec,  
			StartTaskNodesSet startTaskNodesSet, EndTaskNodesSet endTaskNodesSet, XLog log,
			Collection<Pair<FlexNode, XEventClass>> mapping, Object[] parameters);
	
	/**
	 * Method to replay a whole log and check for time needed in order to finish replay
	 * @param context
	 * @param flex
	 * @param log
	 * @param mapping
	 * @param parameters divided into: (1) maximum number of states explored, (2) limit of 
	 * maximum possible states to decided whether a trace is replayed/not, (3) analysis type: 
	 * using gradually incremented prefix (PREFIXANALYSIS), removal of one event and replay
	 * the rest (REMOVEEVENTANALYSIS), and inverting replay from full trace to prefix 
	 * (INVERTPREFIXANALYSIS).  
	 * @return
	 */
	public String replayLogForAnalysis(PluginContext context, Flex flex, StartTaskNodesSet startTaskNodesSet, 
			XLog log, Collection<Pair<FlexNode, XEventClass>> mapping, Object[] parameters);
	
	/**
	 * 
	 * Method to replay a selected trace on flexible model. This method is used to see instances
	 * that are explored to get the best replay result given a metrics 
	 * 
	 * @param context
	 * @param flex
	 * @param trace
	 * @param classes
	 * @param mapping
	 * @param parameters
	 * @return
	 */
	public CaseReplayResult replayXTrace(PluginContext context, Flex flex, StartTaskNodesSet startTaskNodesSet, 
			XTrace trace, XLogInfo logInfo, XEventClasses classes,
			Collection<Pair<FlexNode, XEventClass>> mapping, Object[] parameters);
}
