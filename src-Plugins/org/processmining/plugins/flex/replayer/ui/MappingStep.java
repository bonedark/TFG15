package org.processmining.plugins.flex.replayer.ui;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XLog;
import org.processmining.framework.util.LevenshteinDistance;
import org.processmining.framework.util.Pair;
import org.processmining.models.flexiblemodel.Flex;
import org.processmining.models.flexiblemodel.FlexNode;
import org.processmining.plugins.flex.replayer.algorithms.IFlexLogReplayAlgorithm;

import com.fluxicon.slickerbox.factory.SlickerFactory;
import com.fluxicon.slickerbox.ui.SlickerComboBoxUI;

/**
 * @author arya
 * @email arya.adriansyah@gmail.com
 * @version Dec 1, 2009
 */
public class MappingStep extends ReplayStep {
	private static final long serialVersionUID = -7657691516311492851L;

	public static final String VISIBLEUNMAPPEDTASK = "NONE";
	public static final XEventClass DUMMYEVENTCLASS = new XEventClass("DUMMY",Integer.MIN_VALUE);

	/**
	 * Internal data
	 */
	private Object[] flexNodes;
	private final List<JComboBox> comboBoxes = new LinkedList<JComboBox>();
	private XEventClasses eventClasses;

	public MappingStep(Flex flex, XLog log) {
		initComponents(flex, log);
	}

	public boolean precondition() {
		return true;
	}

	public void readSettings(IFlexLogReplayAlgorithm algorithm) {
	}

	private void initComponents(Flex flex, XLog log) {
		// util objects
		SlickerFactory slickerFactory = SlickerFactory.instance();

		// number of flex nodes
		List<FlexNode> listFlexNodes = new LinkedList<FlexNode>();
		for (FlexNode node : flex.getNodes()){
			if (!node.isInvisible()){
				listFlexNodes.add(node);
			}
		}
		flexNodes = listFlexNodes.toArray();
		Arrays.sort(flexNodes);

		setLayout(new BorderLayout());
		String body = "<p>Map each node on the left side panel to an event class on the right side panel</p>";
		add(slickerFactory.createLabel("<html><h1>Mapping</h1>" + body), BorderLayout.NORTH);

		// init mapping panel
		JPanel insidePanel = slickerFactory.createRoundedPanel();
		insidePanel.setLayout(new TableLayout());

		// init row size
		double rowheight[] = new double[flexNodes.length];
		for (int i = 0; i < rowheight.length; i++) {
			rowheight[i] = 30;
		}

		// init table size
		double size[][] = { { TableLayoutConstants.FILL, 10, TableLayoutConstants.FILL }, rowheight };
		insidePanel.setLayout(new TableLayout(size));

		// create array of available event classes
		XLogInfo summary = XLogInfoFactory.createLogInfo(log);

		// init event classes 
		eventClasses = summary.getEventClasses();

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
			// add to comboboxes
			JComboBox combo = new JComboBox(availEventClasses);
			combo.setPreferredSize(new Dimension(150, 25));
			combo.setSize(new Dimension(150, 25));
			combo.setMinimumSize(new Dimension(150, 25));
			combo.setUI(new SlickerComboBoxUI());
			String currEvNodeStr = ((FlexNode) (flexNodes[i])).getLabel();
			
			// find the most suitable index
			int suitableIndex = 0;
			int bestDistance = Integer.MAX_VALUE;
			int calcDistance = Integer.MAX_VALUE;
			for (int j = 0; j < availEventClasses.length; j++) {
				calcDistance = lDist.getLevenshteinDistanceLinearSpace(currEvNodeStr, availEventClasses[j].toString());
				if (calcDistance < bestDistance){
					suitableIndex = j;
					bestDistance = calcDistance;
				}
			}
			combo.setSelectedIndex(suitableIndex);

			// add to GUI 
			insidePanel.add(slickerFactory.createLabel(((FlexNode) flexNodes[i]).getLabel()), "0, " + i);
			insidePanel.add(combo, "2, " + i);
			comboBoxes.add(combo);
		}

		JScrollPane scrollPane = new JScrollPane(insidePanel);

		add(scrollPane, BorderLayout.CENTER);
	}

	public Collection<Pair<FlexNode, XEventClass>> getMapping() {
		Collection<Pair<FlexNode, XEventClass>> result = new HashSet<Pair<FlexNode, XEventClass>>();
		for (int i = 0; i < flexNodes.length; i++) {
			Object selectedItem = comboBoxes.get(i).getSelectedItem();
			Pair<FlexNode, XEventClass> newPair;
			if (selectedItem instanceof XEventClass) {
				newPair = new Pair<FlexNode, XEventClass>((FlexNode) flexNodes[i], ((XEventClass) comboBoxes.get(i)
						.getSelectedItem()));
				result.add(newPair);
			} else if (selectedItem.equals(VISIBLEUNMAPPEDTASK)){
				newPair = new Pair<FlexNode, XEventClass>((FlexNode) flexNodes[i], DUMMYEVENTCLASS);
				result.add(newPair);
			}
			
		}
		return result;
	}

	public XEventClasses getEventClasses() {
		return eventClasses;
	}
}
