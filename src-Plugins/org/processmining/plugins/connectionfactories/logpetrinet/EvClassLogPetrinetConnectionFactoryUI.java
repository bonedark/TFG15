/**
 * 
 */
package org.processmining.plugins.connectionfactories.logpetrinet;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XLog;
import org.processmining.framework.util.ArrayUtils;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

import uk.ac.shef.wit.simmetrics.similaritymetrics.AbstractStringMetric;
import uk.ac.shef.wit.simmetrics.similaritymetrics.Levenshtein;

import com.fluxicon.slickerbox.factory.SlickerFactory;

/**
 * GUI to map event class (with any classifiers) to transitions of Petri net
 *  
 * @author aadrians
 * 
 */
public class EvClassLogPetrinetConnectionFactoryUI extends JPanel {
	private static final long serialVersionUID = -699953189980632566L;

	// dummy event class (for unmapped transitions)
	public final static XEventClass DUMMY = new XEventClass("DUMMY", -1) {
		public boolean equals(Object o) {
			return this == o;
		}
	};

	// internal attributes
	@SuppressWarnings("unused")
	private final XLog log;
	private Map<Transition, JComboBox> mapTrans2ComboBox = new HashMap<Transition, JComboBox>();
	private JComboBox classifierSelectionCbBox;

	public EvClassLogPetrinetConnectionFactoryUI(final XLog log, final PetrinetGraph net, Object[] availableClassifier) {
		super();
		
		// index for row
		int rowCounter = 0;

		// import variable
		this.log = log;
		
		// swing factory
		SlickerFactory factory = SlickerFactory.instance();

		// set layout
		double size[][] = { { TableLayoutConstants.FILL, TableLayoutConstants.FILL }, { 80, 70 } };
		TableLayout layout = new TableLayout(size);
		setLayout(layout);

		// label
		add(factory
				.createLabel("<html><h1>Map Transitions to Event Class</h1><p>First, select a classifier. Unmapped transitions will be mapped to a dummy event class.</p></html>"),
				"0, " + rowCounter + ", 1, " + rowCounter);
		rowCounter++;

		// add classifier selection
		classifierSelectionCbBox = factory.createComboBox(availableClassifier);
		classifierSelectionCbBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {				
				Object[] boxOptions = extractEventClasses(log, (XEventClassifier) classifierSelectionCbBox.getSelectedItem());
				
				for (Transition transition : mapTrans2ComboBox.keySet()) {
					JComboBox cbBox = mapTrans2ComboBox.get(transition);
					cbBox.removeAllItems(); // remove all items
					
					for (Object item : boxOptions){
						cbBox.addItem(item);
					}
					cbBox.setSelectedIndex(preSelectOption(transition.getLabel(), boxOptions));
				}
			}
		});
		classifierSelectionCbBox.setSelectedIndex(0);
		classifierSelectionCbBox.setPreferredSize(new Dimension(350, 30));
		classifierSelectionCbBox.setMinimumSize(new Dimension(350, 30));

		add(factory.createLabel("Choose classifier"), "0, " + rowCounter + ", l, c");
		add(classifierSelectionCbBox, "1, " + rowCounter + ", l, c");
		rowCounter++;

		// add mapping between transitions and selected event class 
		Object[] boxOptions = extractEventClasses(log, (XEventClassifier) classifierSelectionCbBox.getSelectedItem());
		for (Transition transition : net.getTransitions()){
			layout.insertRow(rowCounter, 30);
			JComboBox cbBox = factory.createComboBox(boxOptions);
			cbBox.setPreferredSize(new Dimension(350, 30));
			cbBox.setMinimumSize(new Dimension(350, 30));
			mapTrans2ComboBox.put(transition, cbBox);
			cbBox.setSelectedIndex(preSelectOption(transition.getLabel(), boxOptions));
			
			add(factory.createLabel(transition.getLabel()), "0, " + rowCounter + ", l, c");
			add(cbBox, "1, " + rowCounter + ", l, c");
			rowCounter++;
		}
	
	}

	
	/**
	 * get all available event classes using the selected classifier, add with NONE
	 * 
	 * @param log
	 * @param selectedItem
	 * @return
	 */
	private Object[] extractEventClasses(XLog log, XEventClassifier selectedItem) {
		XLogInfo summary = XLogInfoFactory.createLogInfo(log, (XEventClassifier) classifierSelectionCbBox.getSelectedItem());
		XEventClasses eventClasses = summary.getEventClasses();
		
		// sort event class
		Collection<XEventClass> classes = eventClasses.getClasses();
		
		// create possible event classes
		Object[] arrEvClass = classes.toArray();
		Arrays.sort(arrEvClass);
		Object[] notMappedAct = { "NONE" };
		Object[] boxOptions = ArrayUtils.concatAll(notMappedAct, arrEvClass);
		
		return boxOptions;
	}
	
	/**
	 * Returns the Event Option Box index of the most similar event for the
	 * transition.
	 * 
	 * @param transition
	 *            Name of the transitions
	 * @param events
	 *            Array with the options for this transition
	 * @return Index of option more similar to the transition
	 */
	private int preSelectOption(String transition, Object[] events) {
		//The metric to get the similarity between strings
		AbstractStringMetric metric = new Levenshtein();

		int index = 0;
		float simOld = metric.getSimilarity(transition, "none");
		simOld = Math.max(simOld, metric.getSimilarity(transition, "invisible"));
		simOld = Math.max(simOld, metric.getSimilarity(transition, "skip"));
		simOld = Math.max(simOld, metric.getSimilarity(transition, "tau"));

		for (int i = 1; i < events.length; i++) {
			String event = ((XEventClass) events[i]).toString();
			float sim = metric.getSimilarity(transition, event);

			if (simOld < sim) {
				simOld = sim;
				index = i;
			}
		}

		return index;
	}
	
	/**
	 * Generate the map between Transitions and Event according to the user
	 * selection.
	 * 
	 * @return Map between Transitions and Events.
	 */
	public TransEvClassMapping getMap() {
		TransEvClassMapping map = new TransEvClassMapping((XEventClassifier) this.classifierSelectionCbBox.getSelectedItem(), DUMMY);
		for (Transition trans : mapTrans2ComboBox.keySet()){
			Object selectedValue = mapTrans2ComboBox.get(trans).getSelectedItem();
			if (selectedValue instanceof XEventClass){
				// a real event class
				map.put(trans, (XEventClass) selectedValue);
			} else {
				// this is "NONE"
				map.put(trans, DUMMY);
			}
		}
		return map;
	}

	/**
	 * Get the selected classifier
	 * @return
	 */
	public XEventClassifier getSelectedClassifier() {
		return (XEventClassifier) classifierSelectionCbBox.getSelectedItem();
	}

}
