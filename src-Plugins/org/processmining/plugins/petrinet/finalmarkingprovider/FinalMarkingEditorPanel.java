/**
 * 
 */
package org.processmining.plugins.petrinet.finalmarkingprovider;

import info.clearthought.layout.TableLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.util.ui.widgets.ProMList;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.semantics.petrinet.Marking;

import com.fluxicon.slickerbox.factory.SlickerFactory;

/**
 * @author aadrians
 * Nov 24, 2011
 *
 */
public class FinalMarkingEditorPanel extends JPanel {
	private static final long serialVersionUID = 8759205978114685628L;

	// GUI
	private ProMList placeList;
	private DefaultListModel placeListMdl;
	
	private ProMList commitedMarkings;
	private DefaultListModel commitedMarkingsMdl;
	
	private ProMList candidateMarkings;
	private DefaultListModel candidateMarkingsMdl;
	
	private JButton addPlacesBtn;
	private JButton commitMarkingBtn;
	private JButton removeMarkingBtn;
	
	public Marking[] getMarkings(UIPluginContext context, PetrinetGraph net) {
		init(net);
		
		// init result variable
		InteractionResult result = context.showWizard("Select mapping", true, true, this);

		// configure interaction with user
		if (result == InteractionResult.FINISHED){
			if (!commitedMarkingsMdl.isEmpty()){
				Marking[] res = new Marking[commitedMarkingsMdl.getSize()];
				Enumeration<?> el = commitedMarkingsMdl.elements();
				for (int i=0; i < res.length; i++){
					res[i] = (Marking) el.nextElement();
				}
				return res;
			}
		}
		return null;
	}

	private void init(PetrinetGraph net) {
		// factory 
		SlickerFactory factory = SlickerFactory.instance();
		
		// place selection
		placeListMdl = new DefaultListModel();
		for (Place p : net.getPlaces()){
			placeListMdl.addElement(p);
		}
		placeList = new ProMList("List of Places", placeListMdl);
		placeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		// add places
		candidateMarkingsMdl = new DefaultListModel();
		candidateMarkings = new ProMList("Candidate Final Marking", candidateMarkingsMdl);
		
		addPlacesBtn = factory.createButton("Add Place to Candidate Marking");
		addPlacesBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				candidateMarkingsMdl.addElement(placeList.getSelectedValues()[0]);
			}
		});
		
		// commit marking
		commitedMarkingsMdl = new DefaultListModel();
		commitedMarkings = new ProMList("Committed Final Markings", commitedMarkingsMdl);

		commitMarkingBtn = factory.createButton("Commit Final Marking");
		commitMarkingBtn.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				if (!candidateMarkingsMdl.isEmpty()){
					Marking newMarking = new Marking();
					Enumeration<?> elements = candidateMarkingsMdl.elements();
					while (elements.hasMoreElements()){
						newMarking.add((Place) elements.nextElement());
					}
					// add the marking to committed marking
					commitedMarkingsMdl.addElement(newMarking);
				}
				// reset all candidate
				candidateMarkingsMdl.removeAllElements();
			}
		});
		
		removeMarkingBtn = factory.createButton("Remove selected final marking(s)");
		removeMarkingBtn.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				for (Object obj : commitedMarkings.getSelectedValues()){
					commitedMarkingsMdl.removeElement(obj);
				};
			}
		});
		
		// now add the elements
		double[][] size = new double[][]{ {250,10,200,10,250},{ 125, 45, 125, 25, 125, 25} };
		TableLayout layout = new TableLayout(size);
		setLayout(layout);
		add(placeList, "0,0,0,2");
		add(addPlacesBtn, "2,1");
		add(candidateMarkings, "4,0,4,2");
		
		add(commitMarkingBtn, "4,3");
		add(commitedMarkings, "0,4,4,4");
		add(removeMarkingBtn, "4,5");

	}

	
	
}
