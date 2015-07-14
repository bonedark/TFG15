/**
 * 
 */
package org.processmining.plugins.petrinet.invisiblemapper;

import java.awt.GridLayout;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

import com.fluxicon.slickerbox.factory.SlickerFactory;

/**
 * @author Arya Adriansyah
 * @email a.adriansyah@tue.nl
 * @version Feb 17, 2011
 */
public class InvisibleTransMapperPanel extends JPanel {
	private static final long serialVersionUID = 3406856963492945923L;

	private List<Transition> transV = new LinkedList<Transition>();

	private List<JComboBox> eBoxes = new LinkedList<JComboBox>();

	private Object[] isInvi = new Object[] {"Invisible", "Not invisible"};
	
	public InvisibleTransMapperPanel(PetrinetGraph net) {
		super();
		//Factory to create ProM swing components
		SlickerFactory factory = SlickerFactory.instance();
		
		//Setting the Layout (table of 2 columns and N rows)
		setLayout(new GridLayout(0, 2));

		//Setting the "table"
		add(factory.createLabel("Transition"));
		add(factory.createLabel("Is invisible?"));
		
		for (Transition transition : net.getTransitions()) {
			//Add the transition in that position to the vector
			transV.add(transition);

			//Create a Label with the name of the Transition
			add(factory.createLabel(transition.getLabel()));

			//Create, store, and show the box of the events for that transition
			JComboBox boxE = factory.createComboBox(isInvi);
			boxE.setSelectedIndex(transition.isInvisible() ? 0 : 1);
			eBoxes.add(boxE);
			add(boxE);
		}
	}

	public Set<Transition> getInviTransitions() {
		Set<Transition> res = new HashSet<Transition>();
		Iterator<JComboBox> it = eBoxes.iterator();
		for (Transition t : transV){
			if (it.next().getSelectedIndex() == 0){
				res.add(t);
			}
		}
		return res;
	}
}
