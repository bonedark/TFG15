/**
 * 
 */
package org.processmining.plugins.petrinet.replayer.matchinstances.ui;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.model.XLog;
import org.processmining.connections.logmodel.LogPetrinetConnection;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.util.Pair;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.plugins.petrinet.replayer.matchinstances.algorithms.CompleteCostBasedPNMatchInstancesReplayAlgorithm;
import org.processmining.plugins.petrinet.replayer.matchinstances.algorithms.CostBasedPNMatchInstancesReplayAlgorithm;

/**
 * @author aadrians
 *
 */
public class PNMatchInstancesReplayerUI {
	// reference variable
	private final UIPluginContext context;

	public static final int MAPPING = 0;
	public static final int ALGORITHM = 1;
	public static final int PARAMETERS = 2;

	// steps
	private int nofSteps;
	private int currentStep;

	private int algorithmStep;
	private int testingParamStep;

	// gui for each steps
	private PNReplayStep[] replaySteps;

	public PNMatchInstancesReplayerUI(final UIPluginContext context) {
		this.context = context;
	}

	public Object[] getConfiguration(PetrinetGraph net, XLog log) {
		// init local parameter
		LogPetrinetConnection conn = null;

		// init steps and gui
		nofSteps = 0;

		// check connection in order to determine whether mapping step is needed
		// of not
		try {
			// connection is found, no need for mapping step
			// connection is not found, another plugin to create such connection
			// is automatically
			// executed
			conn = context.getConnectionManager().getFirstConnection(
					LogPetrinetConnection.class, context, net, log);
		} catch (Exception e) {
			// creating connection is handled automatically
		}

		// other steps
		algorithmStep = nofSteps++;
		testingParamStep = nofSteps++;

		// init gui for each step
		replaySteps = new PNReplayStep[nofSteps];
		replaySteps[algorithmStep] = new PNRepMatchInstancesAlgorithmStep();

		// set current step
		currentStep = algorithmStep;

		// how many configuration indexes?
		int[] configIndexes = new int[1];
		configIndexes[0] = testingParamStep;

		return showConfiguration(log, net, conn, configIndexes);
	}

	private Object[] showConfiguration(XLog log,
			PetrinetGraph net, LogPetrinetConnection conn, int[] configIndexes) {
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

			result = context.showWizard("Replay in Petri net",
					currentStep == 0, currentStep == nofSteps - 1,
					replaySteps[currentStep]);
			switch (result) {
			case NEXT:
				go(1, log, net, conn);
				break;
			case PREV:
				go(-1, log, net, conn);
				break;
			case FINISHED:
				// mapping variable
				Collection<Pair<Transition, XEventClass>> mapping;

				mapping = new HashSet<Pair<Transition, XEventClass>>();
				Collection<Transition> transitions = net.getTransitions();

				for (Transition transition : transitions) {
					Set<XEventClass> activities = conn
							.getActivitiesFor(transition);
					if (activities != null) {
						if (activities.size() > 0) {
							Pair<Transition, XEventClass> newPair = new Pair<Transition, XEventClass>(
									transition, activities.iterator().next());
							mapping.add(newPair);
						}
					}
				}

				// collect all parameters
				List<Object> allParameters = new LinkedList<Object>();
				for (int i = 0; i < configIndexes.length; i++) {
					PNParamSettingStep testParamGUI = ((PNParamSettingStep) replaySteps[configIndexes[i]]);
					Object[] params = testParamGUI.getAllParameters();
					for (Object o : params) {
						allParameters.add(o);
					}
				}

				return new Object[] {
						mapping,
						((PNRepMatchInstancesAlgorithmStep) replaySteps[algorithmStep])
								.getAlgorithm(), allParameters.toArray() };
			default:
				return new Object[] { null };
			}
		}
	}

	private int go(int direction, XLog log, PetrinetGraph net, LogPetrinetConnection conn) {
		currentStep += direction;

		// check which algorithm is selected and adjust parameter as necessary
		if (currentStep == testingParamStep) {
			if (((PNRepMatchInstancesAlgorithmStep) replaySteps[algorithmStep]).getAlgorithm() instanceof CostBasedPNMatchInstancesReplayAlgorithm){
				replaySteps[testingParamStep] = new ParamSettingCostBasedPNMatchInstancesReplayAlgorithm();
			} else if (((PNRepMatchInstancesAlgorithmStep) replaySteps[algorithmStep]).getAlgorithm() instanceof CompleteCostBasedPNMatchInstancesReplayAlgorithm){
				ParamSettingCompleteCostBasedPNMatchInstancesReplayAlgorithm paramSetting = new ParamSettingCompleteCostBasedPNMatchInstancesReplayAlgorithm();
				paramSetting.populateCostPanel(net, log, conn);
				replaySteps[testingParamStep] = paramSetting;
			} 
		}

		if ((currentStep >= 0) && (currentStep < nofSteps)) {
			return currentStep;
		}
		return currentStep;
	}
}
