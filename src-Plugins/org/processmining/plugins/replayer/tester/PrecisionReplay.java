/**
 * 
 */
package org.processmining.plugins.replayer.tester;

import java.util.List;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.connections.petrinets.PNMatchInstancesRepResultConnection;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.plugins.petrinet.replayresult.PNMatchInstancesRepResult;
import org.processmining.plugins.replayer.replayresult.AllSyncReplayResult;
import org.processmining.plugins.replayer.util.StepTypes;

/**
 * @author aadrians
 *
 */

public class PrecisionReplay {
	@Plugin(name = "Measure Precision of a Petri Net to a Log", returnLabels = { "String result" }, returnTypes = { String.class }, parameterLabels = {
			"Petri net", "Event Log" }, help = "Measure Precision of a Petri Net to a Log.", userAccessible = true)
//	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "Arya Adriansyah", email = "a.adriansyah@tue.nl", pack="Replayer")
	@PluginVariant(variantLabel = "From Petri net and Log", requiredParameterLabels = { 0, 1 })
	public String testing(final UIPluginContext context, PetrinetGraph net, XLog log) throws Exception {
		PNMatchInstancesRepResult res = context.tryToFindOrConstructFirstObject(PNMatchInstancesRepResult.class, PNMatchInstancesRepResultConnection.class, PNMatchInstancesRepResultConnection.PNREPRESULT, net, log);
		if (res != null){
			String result = "<html>";
			result += "<p>Matching instances</p><br/>";
			for (AllSyncReplayResult allSyncRepRes: res){
				List<List<Object>> lstlstNodeInstance = allSyncRepRes.getNodeInstanceLst();
				List<List<StepTypes>> lstlstStepTypes = allSyncRepRes.getStepTypesLst();
				
				for (int i=0; i < lstlstNodeInstance.size(); i++){
					List<Object> lstNodeInstance = lstlstNodeInstance.get(i);
					List<StepTypes> lstStepTypes = lstlstStepTypes.get(i);
					result += "<p>[" + i + "] ";
					for (int j=0; j < lstNodeInstance.size(); j++){
						switch (lstStepTypes.get(j)){
							case L: 
								result += "- (L) " + lstNodeInstance.get(j);
								break;
							case LMGOOD: 
								result += "- (L/M) " + lstNodeInstance.get(j);
								break;
							case MINVI : 
								result += "- (M-invi) " + lstNodeInstance.get(j);
								break;
							case MREAL : 
								result += "- (M-real) " + lstNodeInstance.get(j);
								break;
							default : 
								result += "- (unknown)";
								break;
						}
					}
					result += "</p><br/>";
				}
			}
			return result;
		} else {
			return "NOK";
		}
	}
}
