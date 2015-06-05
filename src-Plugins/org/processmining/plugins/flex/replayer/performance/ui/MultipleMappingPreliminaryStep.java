/**
 * 
 */
package org.processmining.plugins.flex.replayer.performance.ui;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

import java.awt.BorderLayout;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.processmining.framework.util.LevenshteinDistance;
import org.processmining.models.flexiblemodel.Flex;
import org.processmining.models.flexiblemodel.FlexNode;
import org.processmining.plugins.flex.replayer.algorithms.IFlexLogReplayAlgorithm;
import org.processmining.plugins.flex.replayer.ui.ReplayStep;

import com.fluxicon.slickerbox.factory.SlickerFactory;

/**
 * @author aadrians
 *
 */
public class MultipleMappingPreliminaryStep extends ReplayStep {
	private static final long serialVersionUID = 2155933966875514474L;

	public static final String VISIBLEUNMAPPEDTASK = "NONE";
	public static final XEventClass DUMMYEVENTCLASS = new XEventClass("DUMMY", Integer.MIN_VALUE);
		
	/**
	 * Internal data
	 */
	private FlexNode[] flexNodes;
	private JList[] selectionList;
	
	public MultipleMappingPreliminaryStep(Flex flex, XEventClasses eventClasses){
		initComponents(flex, eventClasses);
	}
	
	private void initComponents(Flex flex, XEventClasses eventClasses) {
		// util objects
		SlickerFactory slickerFactory = SlickerFactory.instance();
		this.setLayout(new BorderLayout());

		// number of (not invisible) flex nodes
		List<FlexNode> listFlexNodes = new LinkedList<FlexNode>();
		for (FlexNode node : flex.getNodes()) {
			if (!node.isInvisible()) {
				listFlexNodes.add(node);
			}
		}

		flexNodes = new FlexNode[listFlexNodes.size()];
		flexNodes = listFlexNodes.toArray(flexNodes);
		Arrays.sort(flexNodes);

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
				rowheight[i] = 100;
			} else {
				rowheight[i] = 10;
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

		// promList init
		selectionList = new JList[flexNodes.length];

		DefaultListModel listModel = new DefaultListModel();

		for (Object obj : availEventClasses) {
			listModel.addElement(obj);
		}

		for (int i = 0; i < flexNodes.length; i++) {
			// add to list
			JList list = new JList(listModel);

//			list.setBackground(WidgetColors.COLOR_LIST_BG);
//			list.setForeground(WidgetColors.COLOR_LIST_FG);
//			list.setSelectionBackground(WidgetColors.COLOR_LIST_SELECTION_BG);
//			list.setSelectionForeground(WidgetColors.COLOR_LIST_SELECTION_FG);

			final JScrollPane scroller = new JScrollPane(list);
			scroller.setOpaque(false);
			scroller.setBorder(BorderFactory.createEmptyBorder());
			scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
			scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
//			final JScrollBar vBar = scroller.getVerticalScrollBar();
//			vBar.setUI(new SlickerScrollBarUI(vBar, new Color(0, 0, 0, 0), new Color(160, 160, 160),
//					WidgetColors.COLOR_NON_FOCUS, 4, 12));
//			vBar.setOpaque(true);
//			vBar.setBackground(WidgetColors.COLOR_ENCLOSURE_BG);

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
			list.setSelectedIndex(suitableIndex);

			// add to GUI 
			insidePanel.add(slickerFactory.createLabel((flexNodes[i]).getLabel()), "0, " + (2 * i));
			insidePanel.add(scroller, "2, " + (2 * i));
			selectionList[i] = list;
		}

		JScrollPane scrollPane = new JScrollPane(insidePanel);
		this.add(scrollPane, BorderLayout.CENTER);

	}

	public Map<FlexNode, Set<XEventClass>> getConfiguration() {
		Map<FlexNode, Set<XEventClass>> res = new HashMap<FlexNode, Set<XEventClass>>();

		for (int i=0; i < flexNodes.length; i++){
			Set<XEventClass> setEvtClass = new HashSet<XEventClass>();
			for (Object value : selectionList[i].getSelectedValues()){
				if (value.toString().equals(VISIBLEUNMAPPEDTASK)){
					setEvtClass.add(DUMMYEVENTCLASS);
				} else {
					setEvtClass.add((XEventClass) value);
				}
			}			
			res.put(flexNodes[i], setEvtClass);
		}
		
		return res;
	}
	
	public boolean precondition() {
		return true;
	}

	public void readSettings(IFlexLogReplayAlgorithm algorithm) {
	}

}
