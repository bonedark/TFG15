/**
 * 
 */
package org.processmining.plugins.flex.replayer.performance.ui;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.processmining.framework.util.LevenshteinDistance;
import org.processmining.models.flexiblemodel.Flex;
import org.processmining.models.flexiblemodel.FlexNode;
import org.processmining.plugins.flex.replayer.algorithms.IFlexLogReplayAlgorithm;
import org.processmining.plugins.flex.replayer.performance.util.FlexSpecialNodes;
import org.processmining.plugins.flex.replayer.ui.ReplayStep;

import com.fluxicon.slickerbox.factory.SlickerFactory;
import com.fluxicon.slickerbox.ui.SlickerComboBoxUI;

/**
 * @author aadrians
 * 
 */
public class SimpleMappingStep extends ReplayStep {
	private static final long serialVersionUID = 2155933966875514474L;

	public static final String VISIBLEUNMAPPEDTASK = "NONE";
	public static final XEventClass DUMMYEVENTCLASS = new XEventClass("DUMMY", Integer.MIN_VALUE);

	/**
	 * Internal data
	 */
	private FlexNode[] flexNodes;
	private JComboBox[] allComboBox;

	public SimpleMappingStep(Flex flex, FlexSpecialNodes specialNodes, XEventClasses eventClasses) {
		initComponents(flex, specialNodes, eventClasses);
	}

	private void initComponents(Flex flex, FlexSpecialNodes specialNodes, XEventClasses eventClasses) {
		// util objects
		SlickerFactory slickerFactory = SlickerFactory.instance();
		this.setLayout(new BorderLayout());

		Set<FlexNode> setLateBindingNodes = (specialNodes != null) ? specialNodes.getSetLateBindingNodes() : new HashSet<FlexNode>();
		
		// number of (not invisible) flex nodes
		List<FlexNode> listFlexNodes = new LinkedList<FlexNode>();
		for (FlexNode node : flex.getNodes()) {
			if (!node.isInvisible()) {
				if (!setLateBindingNodes.contains(node)){ // place does not have to be mapped
					listFlexNodes.add(node);
				}
			}
		}

		flexNodes = new FlexNode[listFlexNodes.size()];
		flexNodes = listFlexNodes.toArray(flexNodes);
		Arrays.sort(flexNodes);
		allComboBox = new JComboBox[flexNodes.length];
		
		setLayout(new BorderLayout());
		String body = "<p>Map each node on the left side panel to a set of event class on the right side panel</p>";
		add(slickerFactory.createLabel("<html>" + body), BorderLayout.NORTH);

		// init mapping panel
		JPanel insidePanel = slickerFactory.createRoundedPanel();
		insidePanel.setLayout(new TableLayout());

		// init row size
		double rowheight[] = new double[flexNodes.length * 2];
		boolean flag = true;
		for (int i = 0; i < rowheight.length; i++) {
			if (flag) {
				rowheight[i] = 40;
			} else {
				rowheight[i] = 5;
			}
			flag = !flag;
		}

		// init table size
		double size[][] = { { TableLayoutConstants.FILL, 10, TableLayoutConstants.FILL }, rowheight };
		insidePanel.setLayout(new TableLayout(size));

		// create array of available event classes
		Collection<XEventClass> colEventClasses = eventClasses.getClasses();
		Object[] arrEvClasses = colEventClasses.toArray();
		Arrays.sort(arrEvClasses);

		Object[] availEventClasses = new Object[arrEvClasses.length + 1];
		availEventClasses[0] = VISIBLEUNMAPPEDTASK;

		for (int i = 0; i < arrEvClasses.length; i++) {
			availEventClasses[i + 1] = arrEvClasses[i];
		}

		// insert correct item
		LevenshteinDistance lDist = new LevenshteinDistance();

		for (int i = 0; i < flexNodes.length; i++) {
			// add to list
			// add to comboboxes
			JComboBox combo = new JComboBox(availEventClasses);
			combo.setPreferredSize(new Dimension(150, 25));
			combo.setSize(new Dimension(150, 25));
			combo.setMinimumSize(new Dimension(150, 25));
			combo.setUI(new SlickerComboBoxUI());

			// find the most suitable index
			String currEvNodeStr = flexNodes[i].getLabel();

			int suitableIndex = 0;
			int bestDistance = Integer.MAX_VALUE;
			int calcDistance = Integer.MAX_VALUE;
			for (int j = 0; j < availEventClasses.length; j++) {
				calcDistance = lDist.getLevenshteinDistanceLinearSpace(currEvNodeStr, availEventClasses[j].toString());
				if (calcDistance < bestDistance) {
					suitableIndex = j;
					bestDistance = calcDistance;
				}
			}
			combo.setSelectedIndex(suitableIndex);

			// add to GUI 
			insidePanel.add(slickerFactory.createLabel((flexNodes[i]).getLabel()), "0, " + (2 * i));
			insidePanel.add(combo, "2, " + (2 * i));
			allComboBox[i] = combo;
		}

		JScrollPane scrollPane = new JScrollPane(insidePanel);
		this.add(scrollPane, BorderLayout.CENTER);

	}

	public Map<FlexNode, XEventClass> getConfiguration() {
		Map<FlexNode, XEventClass> res = new HashMap<FlexNode, XEventClass>();
		for (int i = 0; i < flexNodes.length; i++) {
			if (allComboBox[i].getSelectedItem().equals(VISIBLEUNMAPPEDTASK)) {
				res.put(flexNodes[i], DUMMYEVENTCLASS);
			} else {
				res.put(flexNodes[i], (XEventClass) allComboBox[i].getSelectedItem());
			}
		}
		return res;
	}

	public boolean precondition() {
		return true;
	}

	public void readSettings(IFlexLogReplayAlgorithm algorithm) {
	}
}
