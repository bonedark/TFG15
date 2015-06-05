/**
 * 
 */
package org.processmining.plugins.replayer.replayresult;

import java.util.List;
import java.util.Map;

import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.model.XTrace;
import org.processmining.models.instancetree.ITEdge;
import org.processmining.models.instancetree.ITGraph;
import org.processmining.models.instancetree.ITNode;
import org.processmining.plugins.replayer.util.StepTypes;

/**
 * @author aadrians
 *
 */
public class CaseReplayResult {
	private ITGraph<? extends ITNode, ? extends ITEdge<? extends ITNode, ? extends ITNode>> graph = null;
	private XLogInfo logInfo = null;
	private XTrace trace = null;
	private List<Object> nodeInstance = null;
	private List<StepTypes> stepTypes = null;
	
	private Map<String, String> infoTable = null;
	
	public CaseReplayResult(ITGraph<? extends ITNode, ? extends ITEdge<? extends ITNode, ? extends ITNode>> graph, 
			Map<String, String> infoTable){
		this.graph = graph;
		this.infoTable = infoTable;
	}
	
	public CaseReplayResult(ITGraph<? extends ITNode, ? extends ITEdge<? extends ITNode, ? extends ITNode>> graph, 
			Map<String, String> infoTable,
			 XTrace trace, XLogInfo logInfo){
		this.graph = graph;
		this.infoTable = infoTable;
		this.trace = trace;
		this.logInfo = logInfo;
	}
	
	/**
	 * @return the graph
	 */
	public ITGraph<? extends ITNode, ? extends ITEdge<? extends ITNode, ? extends ITNode>> getGraph() {
		return graph;
	}
	/**
	 * @param graph the graph to set
	 */
	public void setGraph(
			ITGraph<? extends ITNode, ? extends ITEdge<? extends ITNode, ? extends ITNode>> graph) {
		this.graph = graph;
	}

	/**
	 * @return the information
	 */
	public Map<String, String> getInfoTable() {
		return infoTable;
	}

	/**
	 * @param infoTable the information to set
	 */
	public void setInfoTable(Map<String, String> infoTable) {
		this.infoTable = infoTable;
	}
	
	/**
	 * @return the logInfo
	 */
	public XLogInfo getLogInfo() {
		return logInfo;
	}

	/**
	 * @param logInfo the logInfo to set
	 */
	public void setLogInfo(XLogInfo logInfo) {
		this.logInfo = logInfo;
	}

	/**
	 * @return the trace
	 */
	public XTrace getTrace() {
		return trace;
	}

	/**
	 * @param trace the trace to set
	 */
	public void setTrace(XTrace trace) {
		this.trace = trace;
	}

	/**
	 * @return the nodeInstance
	 */
	public List<Object> getNodeInstance() {
		return nodeInstance;
	}

	/**
	 * @param nodeInstance the nodeInstance to set
	 */
	public void setNodeInstance(List<Object> nodeInstance) {
		this.nodeInstance = nodeInstance;
	}

	/**
	 * @return the stepTypes
	 */
	public List<StepTypes> getStepTypes() {
		return stepTypes;
	}

	/**
	 * @param stepTypes the stepTypes to set
	 */
	public void setStepTypes(List<StepTypes> stepTypes) {
		this.stepTypes = stepTypes;
	}	
}
