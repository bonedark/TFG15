/**
 * 
 */
package org.processmining.plugins.petrinet.replayer.matchinstances.algorithms;

import java.util.Collection;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.util.Pair;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.petrinet.replayresult.PNMatchInstancesRepResult;

/**
 * @author aadrians
 * 
 */
public interface IPNMatchInstancesLogReplayAlgorithm {
	public String toString(); // this is used in comboBox of algorithm selection

	/**
	 * Method to replay a whole log on a model and return all best matching
	 * instance between each trace and the model
	 * 
	 * Assumption: all markings are given, no need to check for connection to the original net
	 * 
	 * @param context
	 * @param net
	 * @param initMarking
	 * @param finalMarking
	 * @param log
	 * @param mapping
	 * @param parameters
	 * @return
	 */
	public PNMatchInstancesRepResult replayLog(PluginContext context, PetrinetGraph net, Marking initMarking,
			Marking finalMarking, XLog log, Collection<Pair<Transition, XEventClass>> mapping, Object[] parameters);

}
