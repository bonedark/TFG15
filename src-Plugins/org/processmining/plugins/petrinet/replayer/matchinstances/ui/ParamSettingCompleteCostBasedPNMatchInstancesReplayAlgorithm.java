/**
 * 
 */
package org.processmining.plugins.petrinet.replayer.matchinstances.ui;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XLog;
import org.processmining.connections.logmodel.LogPetrinetConnection;
import org.processmining.framework.util.ui.widgets.ProMTable;
import org.processmining.framework.util.ui.widgets.ProMTextField;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.plugins.petrinet.replayer.matchinstances.algorithms.CompleteCostBasedPNMatchInstancesReplayAlgorithm;

import com.fluxicon.slickerbox.components.NiceIntegerSlider;
import com.fluxicon.slickerbox.components.NiceSlider.Orientation;
import com.fluxicon.slickerbox.factory.SlickerFactory;

/**
 * @author aadrians
 * 
 */
public class ParamSettingCompleteCostBasedPNMatchInstancesReplayAlgorithm extends PNParamSettingStep {
	private static final long serialVersionUID = -441770156733336644L;

	// default value 
	private static final int DEFCOSTMOVEONLOG = 5;
	private static final int DEFCOSTMOVEONMODEL = 2;
	private static final int MAXLIMMAXNUMINSTANCES = 10001;
	private static final int DEFLIMMAXNUMINSTANCES = 2000;

	// parameter-related GUI
	private NiceIntegerSlider limExpInstances;
	private Map<Transition, Integer> mapTrans2RowIndex = new HashMap<Transition, Integer>();
	private DefaultTableModel tableModel = null;
	private ProMTable promTable;

	private Map<XEventClass, Integer> mapXEvClass2RowIndex = new HashMap<XEventClass, Integer>();
	private DefaultTableModel evClassTableModel = null;
	private ProMTable promEvClassTable;

	public ParamSettingCompleteCostBasedPNMatchInstancesReplayAlgorithm() {
		super();

		double size[][] = { { TableLayoutConstants.FILL },
				{ 80, 40, TableLayoutConstants.FILL, 35, TableLayoutConstants.FILL, 30 } };
		setLayout(new TableLayout(size));
		setMaximumSize(new Dimension(400, 300));
		setPreferredSize(new Dimension(400, 300));

		// label
		SlickerFactory slickerFactoryInstance = SlickerFactory.instance();
		add(slickerFactoryInstance
				.createLabel("<html><h1>Set parameters</h1><p>Double click costs on table to change their values. Use only non-negative integers.</p></html>"),
				"0, 0, l, t");

		// max instance
		limExpInstances = slickerFactoryInstance.createNiceIntegerSlider(
				"<html><h4># Maximum explored states (in hundreds). Set max for unlimited.</h4></html>", 1,
				MAXLIMMAXNUMINSTANCES, DEFLIMMAXNUMINSTANCES, Orientation.HORIZONTAL);
		limExpInstances.setPreferredSize(new Dimension(700, 20));
		limExpInstances.setMaximumSize(new Dimension(700, 20));
		limExpInstances.setMinimumSize(new Dimension(700, 20));

		add(limExpInstances, "0, 1, l, t");
	}

	public void populateCostPanel(PetrinetGraph net, XLog log, LogPetrinetConnection conn) {
		populateMoveOnModelPanel(net, log);
		populateSetAllButton("2", tableModel, "0, 3, c, t");
		populateMoveOnLogPanel(log, conn);
		populateSetAllButton("5", evClassTableModel, "0, 5, c, t");
	}

	private void populateSetAllButton(String defaultCost, final DefaultTableModel tableModel, String addLocation) {
		SlickerFactory factory = SlickerFactory.instance();

		final ProMTextField textField = new ProMTextField(defaultCost);
		textField.setMaximumSize(new Dimension(70, 20));
		textField.setPreferredSize(new Dimension(70, 20));
		JButton setButton = factory.createButton("Set");
		setButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				try {
					int cost = Integer.parseInt(textField.getText().trim());
					if (cost >= 0) {
						for (int i = 0; i < tableModel.getRowCount(); i++) {
							tableModel.setValueAt(cost, i, 1);
						}
					}
				} catch (Exception exc) {
					// no action is performed
				}
			}
		});

		JPanel bgPanel = new JPanel();
		bgPanel.setBackground(new Color(150, 150, 150));
		bgPanel.add(factory.createLabel("Set all costs above to "));
		bgPanel.add(textField);
		bgPanel.add(setButton);
		add(bgPanel, addLocation);
	}

	/**
	 * Generate move on model panel
	 * 
	 * @param net
	 * @param log
	 */
	private void populateMoveOnModelPanel(PetrinetGraph net, final XLog log) {
		// create table to map move on model cost
		Collection<Transition> transitions = net.getTransitions();
		Object[][] tableContent = new Object[transitions.size()][2];
		int rowCounter = 0;
		for (Transition trans : transitions) {
			if (trans.isInvisible()) {
				tableContent[rowCounter] = new Object[] { trans.getLabel(), 0 };
			} else {
				tableContent[rowCounter] = new Object[] { trans.getLabel(), DEFCOSTMOVEONMODEL };
			}
			mapTrans2RowIndex.put(trans, rowCounter);
			rowCounter++;
		}
		tableModel = new DefaultTableModel(tableContent, new Object[] { "Transition", "Move on Model Cost" }) {
			private static final long serialVersionUID = -3870068318560745604L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return (column != 0);
			}
		};
		promTable = new ProMTable(tableModel);
		add(promTable, "0, 2, c, t");
	}

	/**
	 * Generate move on log panel
	 * 
	 * @param log
	 * @param conn
	 */
	private void populateMoveOnLogPanel(XLog log, LogPetrinetConnection conn) {
		// move on log cost (determined by the selection of event class in mapping)
		XLogInfo summary = XLogInfoFactory.createLogInfo(log);
		XEventClasses eventClassesName = summary.getEventClasses();
		evClassTableModel = new DefaultTableModel() {
			private static final long serialVersionUID = 5656621614933096102L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return (column != 0);
			}
		};
		;

		mapXEvClass2RowIndex.clear();

		// move on log cost
		Object[][] evClassTableContent = new Object[eventClassesName.size()][2];
		int evClassRowCounter = 0;
		for (XEventClass evClass : eventClassesName.getClasses()) {
			evClassTableContent[evClassRowCounter] = new Object[] { evClass, DEFCOSTMOVEONLOG };
			mapXEvClass2RowIndex.put(evClass, evClassRowCounter);
			evClassRowCounter++;
		}
		evClassTableModel.setDataVector(evClassTableContent, new Object[] { "Event Class", "Move on Log Cost" });

		promEvClassTable = new ProMTable(evClassTableModel);
		add(promEvClassTable, "0, 4, c, t");
	}

	/**
	 * This method is not necessary, but require as we extend PNParamSettingStep
	 */
	public Object getParameterValue(int paramVariableValIndex) {
		return null;
	}

	/**
	 * Get all parameters for this algorithm
	 */
	public Object[] getAllParameters() {
		Object[] res = new Object[3];

		// create map trans to cost
		res[CompleteCostBasedPNMatchInstancesReplayAlgorithm.MAPTRANSTOCOST] = getTransitionWeight();
		res[CompleteCostBasedPNMatchInstancesReplayAlgorithm.MAXEXPLOREDINSTANCES] = limExpInstances.getValue() == MAXLIMMAXNUMINSTANCES ? Integer.MAX_VALUE
				: limExpInstances.getValue() * 100;
		res[CompleteCostBasedPNMatchInstancesReplayAlgorithm.MAPXEVENTCLASSTOCOST] = getMapEvClassToCost();
		return res;
	}

	/**
	 * Get map from event class to cost of move on log
	 * 
	 * @return
	 */
	private Map<XEventClass, Integer> getMapEvClassToCost() {
		Map<XEventClass, Integer> mapEvClass2Cost = new HashMap<XEventClass, Integer>();
		for (XEventClass evClass : mapXEvClass2RowIndex.keySet()) {
			int index = mapXEvClass2RowIndex.get(evClass);
			if (evClassTableModel.getValueAt(index, 1) instanceof Integer) {
				mapEvClass2Cost.put(evClass, (Integer) evClassTableModel.getValueAt(index, 1));
			} else {
				try {
					mapEvClass2Cost.put(evClass,
							Integer.parseInt(evClassTableModel.getValueAt(index, 1).toString().trim()));
				} catch (Exception exc) {
					mapEvClass2Cost.put(evClass, DEFCOSTMOVEONLOG);
				}

			}
		}
		return mapEvClass2Cost;
	}

	/**
	 * get penalty when move on model is performed
	 * 
	 * @return
	 */
	private Map<Transition, Integer> getTransitionWeight() {
		Map<Transition, Integer> costs = new HashMap<Transition, Integer>();
		for (Transition trans : mapTrans2RowIndex.keySet()) {
			int index = mapTrans2RowIndex.get(trans);
			if (tableModel.getValueAt(index, 1) instanceof Integer) {
				costs.put(trans, (Integer) tableModel.getValueAt(index, 1));
			} else { // instance of other
				try {
					costs.put(trans, Integer.parseInt(tableModel.getValueAt(index, 1).toString().trim()));
				} catch (Exception exc) {
					costs.put(trans, DEFCOSTMOVEONMODEL);
				}

			}
		}
		return costs;
	}
}
