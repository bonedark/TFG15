/**
 * 
 */
package org.processmining.plugins.flex.replayer;


/**
 * @author aadrians
 *
 */
//@Plugin(name = "Replay log on Flexible model for algorithm evaluation", 
//		returnLabels = { "String" }, 
//		returnTypes = {	String.class }, 
//		parameterLabels = { "Flexible model", "Event Log" }, 
//		help = "Evaluation of replay algorithm on Flexible model.", userAccessible = true)
public class FlexReplayBenchmark {
//	@SuppressWarnings("unchecked")
//	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "Arya Adriansyah", email = "a.adriansyah@tue.nl")
//	@PluginVariant(variantLabel = "From Flexible model and Log", requiredParameterLabels = {
//			0, 1 })
//	public String replayLog(final UIPluginContext context, Flex flex, XLog log)
//			throws Exception {
//		// get configuration values from user
//		// checking for connection is performed inside FlexReplayerGUI class
//		FlexReplayerUI flexReplayerUI = new FlexReplayerUI(context);
//		Object[] resultConfiguration = flexReplayerUI.getConfiguration(flex,
//				log, true);
//
//		// if all paramaters are set, replay log
// 		if (resultConfiguration[FlexReplayerUI.MAPPING] != null) {
//			context.log("replay is performed. All parameters are set.");
//
//			// we need flex model, log, mapping, algorithm, and parameters
//			return replayLogPrivate(context, 
//					flex, log, (Collection<Pair<FlexNode, XEventClass>>) resultConfiguration[FlexReplayerUI.MAPPING],
//					(IFlexLogReplayAlgorithm) resultConfiguration[FlexReplayerUI.ALGORITHM],
//					(Object[]) resultConfiguration[FlexReplayerUI.PARAMETERS]);
//		} else {
//			context.log("replay is not performed because not enough parameter is submitted");
//			context.getFutureResult(0).cancel(true);
//			return null;
//		}
//	}
//	
//	private String replayLogPrivate(UIPluginContext context, Flex flex,
//			XLog log, Collection<Pair<FlexNode, XEventClass>> mapping,
//			IFlexLogReplayAlgorithm iFlexLogReplayAlgorithm,
//			Object[] parameters) {
//		// progress update
//		Progress progress = context.getProgress();
//		progress.setMinimum(0);
//		progress.setMaximum(log.size() + 4);
//		progress.setIndeterminate(false);
//
//		// for each trace, replay according to the algorithm
//		String replayRes = iFlexLogReplayAlgorithm.replayLogForAnalysis(context, flex,
//				log, mapping, parameters);
//
//		context.getFutureResult(0).setLabel(
//				"Evaluation of replaying log "
//						+ XConceptExtension.instance().extractName(log)
//						+ " on " + flex.getLabel());
//		
//		return replayRes;
//	}
}
