package org.processmining.plugins.flex.replayer.ui;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.util.Pair;
import org.processmining.models.connections.flexiblemodel.FlexLogConnection;
import org.processmining.models.flexiblemodel.Flex;
import org.processmining.models.flexiblemodel.FlexNode;
import org.processmining.plugins.flex.replayer.algorithms.CancellationAwareAStarAlgorithm;
import org.processmining.plugins.flex.replayer.algorithms.ExtendedCostBasedAStarLogReplayAlgorithm;

/**
 * @author arya
 * @email arya.adriansyah@gmail.com
 * @version Dec 1, 2009
 */
public class FlexReplayerUI {
	// reference variable
	private final UIPluginContext context;

	public static final int MAPPING = 0;
	public static final int ALGORITHM = 1;
	public static final int PARAMETERS = 2;

	// steps 
	private int nofSteps;
	private int currentStep;
	private int introductionStep;
	private int mappingStep;
	private int algorithmStep;
	private int testingParamStep;

	// gui for each steps
	private ReplayStep[] replaySteps;

	// number of params returned
	private int[] paramNum;

	public FlexReplayerUI(final UIPluginContext context) {
		this.context = context;
	}

	public Object[] getConfiguration(Flex flex, XLog log, boolean isTestingMode) {
		// init local parameter
		FlexLogConnection conn = null;

		// init steps and gui
		nofSteps = 0;
		introductionStep = nofSteps++;

		// check connection in order to determine whether mapping step is needed of not
		try {
			// connection is found, no need for mapping step
			conn = context.getConnectionManager().getFirstConnection(FlexLogConnection.class, context, flex, log);
			mappingStep = -1;
		} catch (Exception e) {
			// connection is not found, ask user to map each nodes in Flexible model to event classes
			mappingStep = nofSteps++;
		}

		// other steps
		algorithmStep = nofSteps++;
		testingParamStep = nofSteps++;

		// init gui for each step
		replaySteps = new ReplayStep[nofSteps];
		replaySteps[introductionStep] = new IntroductionStep();
		replaySteps[algorithmStep] = new AlgorithmStep();
		if (mappingStep > 0) { // mapping step is needed
			replaySteps[mappingStep] = new MappingStep(flex, log);
		}

		// set current step
		currentStep = introductionStep;

		int[] configIndexes = new int[1];
		configIndexes[0] = testingParamStep;

		return showConfiguration(isTestingMode, log, flex, conn, configIndexes);
	}

	public Object[] getExtendedConfiguration(Flex flex, XLog log, boolean isTestingMode, ReplayStep[] addition) {
		// init local parameter
		FlexLogConnection conn = null;

		// init steps and gui
		nofSteps = 0;
		introductionStep = nofSteps++;

		// check connection in order to determine whether mapping step is needed of not
		try {
			// connection is found, no need for mapping step
			conn = context.getConnectionManager().getFirstConnection(FlexLogConnection.class, context, flex, log);
			mappingStep = -1;
		} catch (Exception e) {
			// connection is not found, ask user to map each nodes in Flexible model to event classes
			mappingStep = nofSteps++;
		}

		// other steps
		algorithmStep = nofSteps++;
		testingParamStep = nofSteps++;

		// additional steps
		int additionalCounter = nofSteps;
		nofSteps += addition.length;

		// init gui for each step
		replaySteps = new ReplayStep[nofSteps];
		replaySteps[introductionStep] = new IntroductionStep();
		replaySteps[algorithmStep] = new AlgorithmStep();
		if (mappingStep > 0) { // mapping step is needed
			replaySteps[mappingStep] = new MappingStep(flex, log);
		}

		// configuration index
		int[] configIndexes = new int[addition.length + 1];
		configIndexes[0] = testingParamStep;

		// insert addition step to the end
		for (int i = 0; i < addition.length; i++) {
			replaySteps[additionalCounter] = addition[i];
			configIndexes[i + 1] = additionalCounter;
			additionalCounter++;
		}

		// set current step
		currentStep = introductionStep;

		return showConfiguration(isTestingMode, log, flex, conn, configIndexes);
	}

	private Object[] showConfiguration(boolean isTestingMode, XLog log, Flex flex, FlexLogConnection conn,
			int[] configIndexes) {
		// init result variable
		InteractionResult result = InteractionResult.NEXT;

		// configure interaction with user
		while (true) {
			if (currentStep < 0) {
				currentStep = 0;
			}
			if (currentStep >= nofSteps) {
				currentStep = nofSteps - 1;
			}

			result = context.showWizard("Replay Log in Flexible model", currentStep == 0, currentStep == nofSteps - 1,
					replaySteps[currentStep]);
			switch (result) {
				case NEXT :
					// special checking if going after mapping
					if (currentStep == mappingStep) {
						// check if all (<activity types>+<lifecycle combination>) are mapped to at least a node
						MappingStep mappingStepGUI = (MappingStep) replaySteps[mappingStep];
						Collection<Pair<FlexNode, XEventClass>> mapping = mappingStepGUI.getMapping();

						// create array of available event classes
						XLogInfo summary = XLogInfoFactory.createLogInfo(log);
						XEventClasses eventClasses = summary.getEventClasses();
						Collection<XEventClass> colEventClasses = eventClasses.getClasses();

						for (Pair<FlexNode, XEventClass> map : mapping) {
							colEventClasses.remove(map.getSecond());
						}

						if (!colEventClasses.isEmpty()) {
							// requirement is not satisfied
							String message = "";
							String limiter = "";
							for (XEventClass evClass : colEventClasses) {
								message += limiter;
								message += evClass.toString();
								limiter = "<br />- ";
							}

							JOptionPane
									.showMessageDialog(
											null,
											"<html>The following event class: <br/> - "
													+ message
													+ "<br/> are not mapped to any nodes. Map them to nodes, or filter them out from the log and try again.</html>",
											"Unsatisfied Replay Requirement", JOptionPane.ERROR_MESSAGE);
						} else {
							replaySteps[currentStep].readSettings(((AlgorithmStep) replaySteps[algorithmStep])
									.getAlgorithm());
							go(1, isTestingMode);
						}
					} else {
						replaySteps[currentStep].readSettings(((AlgorithmStep) replaySteps[algorithmStep])
								.getAlgorithm());
						go(1, isTestingMode);
					}
					break;
				case PREV :
					go(-1, isTestingMode);
					break;
				case FINISHED :
					// mapping variable
					Collection<Pair<FlexNode, XEventClass>> mapping;

					// create connection (if necessary)
					if (mappingStep > 0) { // need to create connection
						MappingStep mappingStepGUI = (MappingStep) replaySteps[mappingStep];

						// create new connection
						mapping = mappingStepGUI.getMapping();
						context.getConnectionManager().addConnection(
								new FlexLogConnection(log, mappingStepGUI.getEventClasses(), flex, mapping));
					} else {
						mapping = new HashSet<Pair<FlexNode, XEventClass>>();
						Set<FlexNode> flexNodes = flex.getNodes();

						for (FlexNode flexNode : flexNodes) {
							Set<XEventClass> activities = conn.getActivitiesFor(flexNode);
							if (activities != null) {
								if (activities.size() > 0) {
									Pair<FlexNode, XEventClass> newPair = new Pair<FlexNode, XEventClass>(flexNode,
											activities.iterator().next());
									mapping.add(newPair);
								}
							}
						}
					}

					// collect all parameters
					paramNum = new int[configIndexes.length];
					List<Object> allParameters = new LinkedList<Object>();
					for (int i = 0; i < configIndexes.length; i++) {
						ParamSettingStep testParamGUI = ((ParamSettingStep) replaySteps[configIndexes[i]]);
						Object[] params = testParamGUI.getAllParameters();
						paramNum[i] = params.length;
						for (Object o : params) {
							allParameters.add(o);
						}
					}

					return new Object[] { mapping, ((AlgorithmStep) replaySteps[algorithmStep]).getAlgorithm(),
							allParameters.toArray() };
				default :
					return new Object[] { null };
			}
		}
	}

	public int[] getParamNum() {
		return this.paramNum;
	}

	private int go(int direction, boolean isTestingMode) {
		currentStep += direction;

		// check which algorithm is selected and adjust parameter as necessary
		if (currentStep == testingParamStep) {
			if (((AlgorithmStep) replaySteps[algorithmStep]).getAlgorithm() instanceof ExtendedCostBasedAStarLogReplayAlgorithm) {
				if (isTestingMode) {
					replaySteps[testingParamStep] = new ParamSettingExtendedCostBasedReplay(true);
				} else {
					replaySteps[testingParamStep] = new ParamSettingExtendedCostBasedReplay(false);
				}
			} else if (((AlgorithmStep) replaySteps[algorithmStep]).getAlgorithm() instanceof CancellationAwareAStarAlgorithm) {
				if (isTestingMode) {
					replaySteps[testingParamStep] = new ParamSettingCancellationAwareAStarAlgorithm(true);
				} else {
					replaySteps[testingParamStep] = new ParamSettingCancellationAwareAStarAlgorithm(false);
				}
			}
		}

		if ((currentStep >= 0) && (currentStep < nofSteps)) {
			if (replaySteps[currentStep].precondition()) {
				return currentStep;
			} else {
				return go(direction, isTestingMode);
			}
		}
		return currentStep;
	}
}
