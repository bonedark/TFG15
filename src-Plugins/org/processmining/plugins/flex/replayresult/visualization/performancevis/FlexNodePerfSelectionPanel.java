/**
 * 
 */
package org.processmining.plugins.flex.replayresult.visualization.performancevis;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.processmining.framework.util.Pair;
import org.processmining.framework.util.ui.scalableview.ScalableComponent;
import org.processmining.framework.util.ui.scalableview.ScalableViewPanel;
import org.processmining.framework.util.ui.scalableview.interaction.ViewInteractionPanel;
import org.processmining.models.flexiblemodel.CancellationRegion;
import org.processmining.models.flexiblemodel.Flex;
import org.processmining.models.flexiblemodel.FlexEdge;
import org.processmining.models.flexiblemodel.FlexNode;
import org.processmining.models.graphbased.AttributeMap;
import org.processmining.models.graphbased.AttributeMapOwner;
import org.processmining.models.graphbased.ViewSpecificAttributeMap;
import org.processmining.models.jgraph.ProMJGraph;
import org.processmining.plugins.flex.replayer.util.FlexBinding;
import org.processmining.plugins.flex.replayer.util.FlexCodec;
import org.processmining.plugins.flex.replayresult.performance.CaseInstance;
import org.processmining.plugins.flex.replayresult.performance.EdgeInstance;
import org.processmining.plugins.flex.replayresult.performance.FlexBindingInstance;
import org.processmining.plugins.flex.replayresult.performance.FlexPerfRepInfo;

import com.fluxicon.slickerbox.factory.SlickerDecorator;
import com.fluxicon.slickerbox.factory.SlickerFactory;

/**
 * @author aadrians
 * 
 */
public class FlexNodePerfSelectionPanel extends JPanel implements ViewInteractionPanel {
	private static final long serialVersionUID = 3172287032216250110L;

	// taken for granted from ListSelectionPanel
	protected ProMJGraph graph;
	protected SlickerFactory factory = SlickerFactory.instance();
	protected SlickerDecorator decorator = SlickerDecorator.instance();

	protected final String title;
	protected ViewSpecificAttributeMap originalSpecificMap;
	protected ViewSpecificAttributeMap viewSpecificMap;

	protected final JScrollPane scroll;
	protected final JList component;

	private String name;

	// reference to model
	private Flex model = null;
	private FlexCodec codec = null;
	private CancellationRegion cancellationRegion = null;

	// reference to performance information 
	private FlexPerfRepInfo perfResult = null;

	// for formatting purpose
	private NumberFormat nf = NumberFormat.getInstance();

	// type of visualization
	public static final int FPD = 0; // correspond to element of list
	public static final int NORMAL = -1;

	// for hover
	protected int visualizationSelection = -1;

	// VISUALIZATION PARAMETER
	// NODES
	private static int MINNODEWIDTH = 120;
	private static int MINNODEHEIGHT = 50;
	private static int NORMALNODEWIDTH = 120;
	private static int NORMALNODEHEIGHT = 70;
	private static int MAXNODEADDITIONALWIDTH = 10;
	private static int MAXNODEADDITIONALHEIGHT = 50;
	private static int UNVISITEDNODEWIDTH = 80;
	private static int UNVISITEDHEIGHT = 40;

	// EDGES
	private static float NORMALLINEWIDTH = 1f;
	private static float MINARCWIDTH = 1f;
	private static float MAXARCADDITIONWIDTH = 3f;

	public FlexNodePerfSelectionPanel(String name, String title, Flex model, FlexCodec codec,
			FlexPerfRepInfo perfResult, CancellationRegion cancellationRegion, ProMJGraph graph) {
		super(new BorderLayout());

		this.name = name;
		this.title = title;
		this.component = new JList();
		this.component.setOpaque(false);
		this.scroll = new JScrollPane(this.component);

		// add the title
		JLabel panelTitle = factory.createLabel(title);
		panelTitle.setHorizontalTextPosition(SwingConstants.CENTER);
		panelTitle.setVerticalTextPosition(SwingConstants.CENTER);
		panelTitle.setForeground(Color.WHITE);
		panelTitle.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 16));
		panelTitle.setOpaque(false);
		this.add(panelTitle, BorderLayout.NORTH);
		this.setOpaque(false);

		decorator.decorate(scroll, Color.WHITE, Color.GRAY, Color.DARK_GRAY);
		scroll.getViewport().setOpaque(false);
		scroll.setOpaque(false);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setOpaque(false);

		this.add(scroll, BorderLayout.CENTER);

		ListCellRenderer renderer = new ViewListCellRenderer();
		this.component.setCellRenderer(renderer);
		this.component.setSelectionModel(new ToggleSelectionModel());

		component.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// add reference to original model and other necessary things
		this.model = model;
		this.codec = codec;
		this.perfResult = perfResult;
		this.cancellationRegion = cancellationRegion;

		// formatting numbers
		nf.setMinimumFractionDigits(3);
		nf.setMaximumFractionDigits(3);

		// visualization from JGraph
		setScalableComponent(graph);

		// implement selection listener, it is interactive
		component.addListSelectionListener(new ListSelectionListener() {

			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) {
					return;
				}
				ListSelectionModel lsm = component.getSelectionModel();
				int selectedIndex = lsm.getMinSelectionIndex();
				if (selectedIndex < 0) {
					component.clearSelection();
					uncolorElements();
					return;
				}

				uncolorElements();
				colorElements(selectedIndex);
				visualizationSelection = selectedIndex;
			}

		});

		component.setModel(new DefaultListModel());
		((DefaultListModel) component.getModel()).addElement(new Pair<Integer, String>(FPD,
				"YAWL Performance Diagram (YPD)"));
		//		((DefaultListModel) component.getModel()).addElement(new Pair<Integer, String>(1, "Dummy diagram"));

	}

	public void updated() {
		// ignore (as in ListSelectionPanel)
	}

	public String getPanelName() {
		return name;
	}

	public JComponent getComponent() {
		return this;
	}

	public void setScalableComponent(ScalableComponent scalable) {
		if (scalable instanceof ProMJGraph) {
			this.graph = (ProMJGraph) scalable;
			this.viewSpecificMap = graph.getViewSpecificAttributes();

			originalSpecificMap = new ViewSpecificAttributeMap();
			for (AttributeMapOwner o : viewSpecificMap.keySet()) {
				for (String key : viewSpecificMap.keySet(o)) {
					originalSpecificMap.putViewSpecific(o, key, viewSpecificMap.get(o, key));
				}
			}
		} else {
			throw new IllegalArgumentException("Scalable needs to be instance of ProMJGraph");
		}
	}

	public void setParent(ScalableViewPanel viewPanel) {
		// ignore (as in ListSelectionPanel)		
	}

	public double getHeightInView() {
		return 140;
	}

	public double getWidthInView() {
		return 250;
	}

	public void willChangeVisibility(boolean to) {
		// do nothing if selection becomes empty
	}

	private void colorElements(int selectedVisualization) {
		if (selectedVisualization == FPD) {
			colorElementsWithFPD();
		}
	}

	private void colorElementsWithFPD() {
		// calculate performance values
		Map<FlexNode, Double> workingTime = new HashMap<FlexNode, Double>(); // also store temporary frequency
		Map<FlexNode, Double> waitingTime = new HashMap<FlexNode, Double>();
		Map<FlexNode, Double> syncTime = new HashMap<FlexNode, Double>();

		Map<Short, Double> sumWorkingTime = new HashMap<Short, Double>();
		Map<Short, Double> sumWaitingTime = new HashMap<Short, Double>();
		Map<Short, Double> sumSyncTime = new HashMap<Short, Double>();

		Map<FlexNode, FPDNodeDecorData> mapToDecor = new HashMap<FlexNode, FPDNodeDecorData>();

		// abuse flexBindingInstance to store node performance info

		Map<FlexNode, Long> minWaitingTime = new HashMap<FlexNode, Long>();
		Map<FlexNode, Long> maxWaitingTime = new HashMap<FlexNode, Long>();
		Map<FlexNode, Long> minWorkingTime = new HashMap<FlexNode, Long>();
		Map<FlexNode, Long> maxWorkingTime = new HashMap<FlexNode, Long>();
		Map<FlexNode, Long> minSyncTime = new HashMap<FlexNode, Long>();
		Map<FlexNode, Long> maxSyncTime = new HashMap<FlexNode, Long>();

		Map<Short, Set<FlexBindingInstance>> nodeInstancesMap = perfResult.getNodeInstancesMap();
		for (Short encodedFlexNode : nodeInstancesMap.keySet()) {
			for (FlexBindingInstance bindingInstance : nodeInstancesMap.get(encodedFlexNode)) {
				FlexBinding binding = codec.getFlexBindingFor(bindingInstance.getEncodedBinding());
				short encodedNode = binding.getEncodedNode();
				FlexNode node = codec.decode(encodedNode);

				FPDNodeDecorData decor = mapToDecor.get(node);
				if (decor == null) {
					decor = new FPDNodeDecorData();
					mapToDecor.put(node, decor);
				}
				decor.setFrequency(decor.getFrequency() + bindingInstance.getFrequency());
				updateDecorSemantic(node, decor, binding, decor.getFrequency());

				decor.setCancelledFrequency(perfResult.getCancellationFrequency(encodedNode));

				if (sumWorkingTime.get(encodedNode) != null) {
					// working time
					sumWorkingTime.put(encodedNode, sumWorkingTime.get(encodedNode)
							+ (bindingInstance.getAvgWorkTime() * bindingInstance.getFrequency()));
					workingTime.put(node, bindingInstance.getFrequency() + workingTime.get(node));

					if (Double.compare(minWorkingTime.get(node), bindingInstance.getMinWorkTime()) > 0) {
						minWorkingTime.put(node, bindingInstance.getMinWorkTime());
					}
					if (Double.compare(maxWorkingTime.get(node), bindingInstance.getMaxWorkTime()) < 0) {
						maxWorkingTime.put(node, bindingInstance.getMaxWorkTime());
					}

				} else {
					// working time for the first time
					sumWorkingTime.put(encodedNode, bindingInstance.getAvgWorkTime() * bindingInstance.getFrequency());
					workingTime.put(node, (double) bindingInstance.getFrequency());
					minWorkingTime.put(node, bindingInstance.getMinWorkTime());
					maxWorkingTime.put(node, bindingInstance.getMaxWorkTime());
				}

				if (sumWaitingTime.get(encodedNode) != null) {
					// waiting time
					sumWaitingTime.put(encodedNode, sumWaitingTime.get(encodedNode)
							+ (bindingInstance.getAvgWaitTime() * bindingInstance.getFrequency()));
					waitingTime.put(node, bindingInstance.getFrequency() + waitingTime.get(node));
					if (Double.compare(minWaitingTime.get(node), bindingInstance.getMinWaitTime()) > 0) {
						minWaitingTime.put(node, bindingInstance.getMinWaitTime());
					}
					if (Double.compare(maxWaitingTime.get(node), bindingInstance.getMaxWaitTime()) < 0) {
						maxWaitingTime.put(node, bindingInstance.getMaxWaitTime());
					}
				} else {
					// waiting time for the first time
					sumWaitingTime.put(encodedNode, bindingInstance.getAvgWaitTime() * bindingInstance.getFrequency());
					waitingTime.put(node, (double) bindingInstance.getFrequency());
					minWaitingTime.put(node, bindingInstance.getMinWaitTime());
					maxWaitingTime.put(node, bindingInstance.getMaxWaitTime());
				}

				if (sumSyncTime.get(encodedNode) != null) {
					// sync time
					sumSyncTime.put(encodedNode, sumSyncTime.get(encodedNode)
							+ (bindingInstance.getAvgSyncTime() * bindingInstance.getFrequency()));
					syncTime.put(node, bindingInstance.getFrequency() + syncTime.get(node));
					if (Double.compare(minSyncTime.get(node), bindingInstance.getMinSyncTime()) > 0) {
						minSyncTime.put(node, bindingInstance.getMinSyncTime());
					}
					if (Double.compare(maxSyncTime.get(node), bindingInstance.getMaxSyncTime()) < 0) {
						maxSyncTime.put(node, bindingInstance.getMaxSyncTime());
					}

				} else {
					// sync time for the first time
					sumSyncTime.put(encodedNode, bindingInstance.getAvgSyncTime() * bindingInstance.getFrequency());
					syncTime.put(node, (double) bindingInstance.getFrequency());
					minSyncTime.put(node, bindingInstance.getMinSyncTime());
					maxSyncTime.put(node, bindingInstance.getMaxSyncTime());
				}

			}
		}

		Set<FlexNode> setAllNodes = codec.getFlexNodes();

		Set<Double> sumNodeAvgWorkingTime = new HashSet<Double>();
		Set<Double> sumNodeAvgWaitingTime = new HashSet<Double>();
		Set<Double> sumNodeAvgSyncTime = new HashSet<Double>();

		// also set the height of a node
		int maxNodeFreq = Integer.MIN_VALUE;
		int minNodeFreq = Integer.MAX_VALUE;
		for (FlexNode flexNode : setAllNodes) {
			if (workingTime.get(flexNode) != null) {
				double freq = workingTime.get(flexNode);
				double avg = sumWorkingTime.get(codec.encode(flexNode)) / freq;

				if (maxNodeFreq < mapToDecor.get(flexNode).getFrequency()) {
					maxNodeFreq = mapToDecor.get(flexNode).getFrequency();
				}
				if (minNodeFreq > mapToDecor.get(flexNode).getFrequency()) {
					minNodeFreq = mapToDecor.get(flexNode).getFrequency();
				}

				workingTime.put(flexNode, avg);
				sumNodeAvgWorkingTime.add(avg);
			} else {
				workingTime.put(flexNode, Double.NaN);
			}

			if (waitingTime.get(flexNode) != null) {
				double avg = sumWaitingTime.get(codec.encode(flexNode)) / waitingTime.get(flexNode);
				waitingTime.put(flexNode, avg);
				sumNodeAvgWaitingTime.add(avg);
			} else {
				waitingTime.put(flexNode, Double.NaN);
			}

			if (syncTime.get(flexNode) != null) {
				double avg = sumSyncTime.get(codec.encode(flexNode)) / syncTime.get(flexNode);
				syncTime.put(flexNode, avg);
				sumNodeAvgSyncTime.add(avg);
			} else {
				syncTime.put(flexNode, Double.NaN);
			}
		}

		// prepare color for sync, etc
		// bound for working time
		sumNodeAvgWorkingTime.remove(Double.NaN);
		double lowBoundAvgWorking = Double.POSITIVE_INFINITY;
		double upBoundAvgWorking = Double.POSITIVE_INFINITY;
		if (sumNodeAvgWorkingTime.size() > 1) {
			double[] arrSumNodeAvgWorking = getSortedArrayFromSet(sumNodeAvgWorkingTime);
			if (arrSumNodeAvgWorking.length >= 3) {
				int indexLow = arrSumNodeAvgWorking.length / 3;
				lowBoundAvgWorking = arrSumNodeAvgWorking[indexLow - 1];
				upBoundAvgWorking = arrSumNodeAvgWorking[(indexLow * 2) - 1];
			} else {
				lowBoundAvgWorking = arrSumNodeAvgWorking[0];
			}
		}
		// bound for waiting time
		sumNodeAvgWaitingTime.remove(Double.NaN);
		double lowBoundAvgWaiting = Double.POSITIVE_INFINITY;
		double upBoundAvgWaiting = Double.POSITIVE_INFINITY;
		if (sumNodeAvgWaitingTime.size() > 1) {
			double[] arrSumNodeAvgWaiting = getSortedArrayFromSet(sumNodeAvgWaitingTime);
			if (arrSumNodeAvgWaiting.length >= 3) {
				int indexLow = arrSumNodeAvgWaiting.length / 3;
				lowBoundAvgWaiting = arrSumNodeAvgWaiting[indexLow - 1];
				upBoundAvgWaiting = arrSumNodeAvgWaiting[(indexLow * 2) - 1];
			} else {
				lowBoundAvgWaiting = arrSumNodeAvgWaiting[0];
			}
		}
		// bound for sync time
		sumNodeAvgWorkingTime.remove(Double.NaN);
		double lowBoundAvgSync = Double.POSITIVE_INFINITY;
		double upBoundAvgSync = Double.POSITIVE_INFINITY;
		if (sumNodeAvgSyncTime.size() > 1) {
			double[] arrSumNodeAvgSync = getSortedArrayFromSet(sumNodeAvgSyncTime);
			if (arrSumNodeAvgSync.length >= 3) {
				int indexLow = arrSumNodeAvgSync.length / 3;
				lowBoundAvgSync = arrSumNodeAvgSync[indexLow - 1];
				upBoundAvgSync = arrSumNodeAvgSync[(indexLow * 2) - 1];
			} else {
				lowBoundAvgSync = arrSumNodeAvgSync[0];
			}
		}

		// decorate FPD Nodes
		Map<Short, Integer> caseInvolvement = perfResult.getNodeCaseInvolvement();
		CaseInstance caseInstance = perfResult.getCaseInstance();
		Map<Short, Integer> unfinishedFrequency = perfResult.getUnfinishedFrequency();

		int minMaxNodeDifference = (maxNodeFreq - minNodeFreq) > 0 ? maxNodeFreq - minNodeFreq : 1; // for calculation of height 

		for (FlexNode node : model.getNodes()) {
			// arrange view that cannot be set by decorator
			viewSpecificMap.putViewSpecific(node, AttributeMap.FILLCOLOR, Color.LIGHT_GRAY);
			viewSpecificMap.putViewSpecific(node, AttributeMap.SHOWLABEL, false);

			FPDNodeDecorData nodeDecor = mapToDecor.get(node);
			short encodedNode = codec.encode(node);
			if (nodeDecor != null) {
				nodeDecor.setCaseInvolvementRatio((double) caseInvolvement.get(encodedNode)
						/ (double) caseInstance.getFrequency());
				nodeDecor.setWorkingColor(getColor(workingTime.get(node), lowBoundAvgWorking, upBoundAvgWorking));
				nodeDecor.setWaitColor(getColor(waitingTime.get(node), lowBoundAvgWaiting, upBoundAvgWaiting));
				nodeDecor.setSyncColor(getColor(syncTime.get(node), lowBoundAvgSync, upBoundAvgSync));
				nodeDecor.setAvgWorkingTime(workingTime.get(node));
				nodeDecor.setName(node.getLabel());
				if (cancellationRegion != null){
					nodeDecor.setCancelingOther(cancellationRegion.get(node) != null);
				}
				if (unfinishedFrequency.get(encodedNode) != null) {
					nodeDecor.setUnfinishedActivityFrequency(unfinishedFrequency.get(encodedNode));
				}
				nodeDecor.setCancelledFrequency(perfResult.getCancellationFrequency(encodedNode));
				node.setDecorator(new FPDNodeDecorator(nodeDecor));

				viewSpecificMap.removeViewSpecific(node, AttributeMap.SIZE);
				viewSpecificMap
						.putViewSpecific(
								node,
								AttributeMap.SIZE,
								new Dimension(
										MINNODEWIDTH
												+ (((nodeDecor.getFrequency() - minNodeFreq) * MAXNODEADDITIONALWIDTH) / minMaxNodeDifference),
										MINNODEHEIGHT
												+ (((nodeDecor.getFrequency() - minNodeFreq) * MAXNODEADDITIONALHEIGHT) / minMaxNodeDifference)));

				String additionalText = "</table><br/><table border=0><tr><td colspan=6><b>Waiting time</b></td></tr><tr><td>Avg.</td><td>"
						+ getSuitableTimeStr(waitingTime.get(node))
						+ "</td><td>Min.</td><td>"
						+ getSuitableTimeStr(minWaitingTime.get(node))
						+ " </td><td>Max.</td>"
						+ getSuitableTimeStr(maxWaitingTime.get(node)) + "<td></td></tr>";
				additionalText += "<tr><td colspan=6><b>Sync. time</b></td></tr><tr><td>Avg.</td><td>"
						+ getSuitableTimeStr(syncTime.get(node)) + "</td><td>Min.</td><td>"
						+ getSuitableTimeStr(minSyncTime.get(node)) + "</td><td>Max.</td><td>"
						+ getSuitableTimeStr(maxSyncTime.get(node)) + "</td></tr>";
				additionalText += "<tr><td colspan=6><b>Throughput time</b></td></tr><tr><td>Avg.</td><td>"
						+ getSuitableTimeStr(workingTime.get(node)) + "</td><td>Min.</td><td>"
						+ getSuitableTimeStr(minWorkingTime.get(node)) + "</td><td>Max.</td><td>"
						+ getSuitableTimeStr(maxWorkingTime.get(node)) + "</td></tr>";

				String previousText = (String) originalSpecificMap.get(node, AttributeMap.TOOLTIP);
				viewSpecificMap.putViewSpecific(node, AttributeMap.TOOLTIP, previousText + additionalText);
			} else {
				nodeDecor = new FPDNodeDecorData();
				nodeDecor.setName(node.getLabel());
				if (cancellationRegion != null){
					nodeDecor.setCancelingOther(cancellationRegion.get(node) != null);
				}
				if (unfinishedFrequency.get(encodedNode) != null) {
					nodeDecor.setUnfinishedActivityFrequency(unfinishedFrequency.get(encodedNode));
				}
				nodeDecor.setCancelledFrequency(perfResult.getCancellationFrequency(encodedNode));
				node.setDecorator(new FPDNodeDecorator(nodeDecor));

				viewSpecificMap.removeViewSpecific(node, AttributeMap.SIZE);
				viewSpecificMap.putViewSpecific(node, AttributeMap.SIZE, new Dimension(UNVISITEDNODEWIDTH,
						UNVISITEDHEIGHT));

			}
		}

		// find bound for edges (including frequency)
		int minEdgeFreq = Integer.MAX_VALUE;
		int maxEdgeFreq = Integer.MIN_VALUE;
		Map<Pair<Short, Short>, EdgeInstance> instanceMap = perfResult.getEdgeInstancesMap();
		Set<Double> sumEdgeAvgMoveTime = new HashSet<Double>();
		for (Pair<Short, Short> pair : instanceMap.keySet()) {
			EdgeInstance edgeInstance = instanceMap.get(pair);
			if (minEdgeFreq > edgeInstance.getFrequency()) {
				minEdgeFreq = edgeInstance.getFrequency();
			}
			if (maxEdgeFreq < edgeInstance.getFrequency()) {
				maxEdgeFreq = edgeInstance.getFrequency();
			}
			sumEdgeAvgMoveTime.add(edgeInstance.getAvgMoveTime());
		}
		;

		// bound for moving time
		sumEdgeAvgMoveTime.remove(Double.NaN);
		double lowBoundAvgMove = Double.POSITIVE_INFINITY;
		double upBoundAvgMove = Double.POSITIVE_INFINITY;
		if (sumEdgeAvgMoveTime.size() > 1) {
			double[] arrSumNodeAvgMove = getSortedArrayFromSet(sumEdgeAvgMoveTime);
			if (arrSumNodeAvgMove.length >= 3) {
				int indexLow = arrSumNodeAvgMove.length / 3;
				lowBoundAvgMove = arrSumNodeAvgMove[indexLow - 1];
				upBoundAvgMove = arrSumNodeAvgMove[(indexLow * 2) - 1];
			} else {
				lowBoundAvgMove = arrSumNodeAvgMove[1];
			}
		}

		int minMaxEdgeDifference = maxEdgeFreq - minEdgeFreq;
		for (FlexEdge<? extends FlexNode, ? extends FlexNode> edge : model.getEdges()) {
			Pair<Short, Short> shortRepresentation = new Pair<Short, Short>(codec.encode(edge.getSource()),
					codec.encode(edge.getTarget()));
			EdgeInstance edgeInstance = instanceMap.get(shortRepresentation);

			if (edgeInstance != null) {
				if (edgeInstance.getFrequency() > 0) {
					viewSpecificMap.putViewSpecific(edge, AttributeMap.EDGECOLOR,
							getColor(edgeInstance.getAvgMoveTime(), lowBoundAvgMove, upBoundAvgMove));

					String tooltipStr = "<html><table><tr><td>Freq.</td><td>"
							+ edgeInstance.getFrequency()
							+ "</td></tr>"
							+ "<tr><td>#Canceled</td><td>"
							+ perfResult.getArcCancelationFrequency(new Pair<FlexNode, FlexNode>(edge.getSource(), edge
									.getTarget())) + "</td></tr></table>";
					tooltipStr += "</table><br/><table><tr><td colspan=4><b>Moving time</b></td></tr><tr><td>Avg.</td><td>"
							+ getSuitableTimeStr(edgeInstance.getAvgMoveTime()) + "</td>";
					tooltipStr += "<td>Min.</td><td>" + getSuitableTimeStr(edgeInstance.getMinMoveTime())
							+ "</td><td>Max.</td><td>" + getSuitableTimeStr(edgeInstance.getMaxMoveTime())
							+ "</td><td>Std. Dev.</td><td>" + getSuitableTimeStr(edgeInstance.getStdDevMoveTime())
							+ "</td></tr>";

					viewSpecificMap.putViewSpecific(edge, AttributeMap.TOOLTIP, tooltipStr);
					
					float newSize = MINARCWIDTH + (((edgeInstance.getFrequency() - minEdgeFreq) * MAXARCADDITIONWIDTH)/ minMaxEdgeDifference);
					if ((Float.compare(MINARCWIDTH, newSize) > 0)||(Float.compare(Float.NaN, newSize) == 0)){
						viewSpecificMap.putViewSpecific(edge, AttributeMap.LINEWIDTH, MINARCWIDTH);
					} else {
						viewSpecificMap.putViewSpecific(edge, AttributeMap.LINEWIDTH, newSize);
					}
					
				} else {
					viewSpecificMap.putViewSpecific(edge, AttributeMap.EDGECOLOR, Color.LIGHT_GRAY);
					String tooltipStr = "<html><tr><td>Freq.</td><td>0</td></tr>"
							+ "<tr><td>#Canceled</td><td>"
							+ perfResult.getArcCancelationFrequency(new Pair<FlexNode, FlexNode>(edge.getSource(), edge
									.getTarget())) + "</td></tr></html>";

					viewSpecificMap.putViewSpecific(edge, AttributeMap.TOOLTIP, tooltipStr);
					viewSpecificMap.putViewSpecific(edge, AttributeMap.LINEWIDTH, MINARCWIDTH);
				}
			} else {
				String tooltipStr = "<html><tr><td>Freq.</td><td>0</td></tr>"
						+ "<tr><td>#Canceled</td><td>"
						+ perfResult.getArcCancelationFrequency(new Pair<FlexNode, FlexNode>(edge.getSource(), edge
								.getTarget())) + "</td></tr></html>";

				viewSpecificMap.putViewSpecific(edge, AttributeMap.TOOLTIP, tooltipStr);

				viewSpecificMap.putViewSpecific(edge, AttributeMap.EDGECOLOR, Color.LIGHT_GRAY);
			}
		}

		// refresh the figure
		graph.update(model.getNodes());
		graph.update(model.getEdges());
	}

	private String getSuitableTimeStr(double avgMoveTime) {
		if (Double.compare(avgMoveTime, 1000 * 60 * 60 * 24) > 0) {
			// days...
			return (nf.format(avgMoveTime / (1000 * 60 * 60 * 24)) + " days");
		} else if (Double.compare(avgMoveTime, 1000 * 60 * 60) > 0) {
			// hours
			return (nf.format(avgMoveTime / (1000 * 60 * 60)) + " hours");
		} else if (Double.compare(avgMoveTime, 1000 * 60) > 0) {
			// minutes
			return (nf.format(avgMoveTime / (1000 * 60)) + " min");
		} else if (Double.compare(avgMoveTime, 1000) > 0) {
			// seconds
			return (nf.format(avgMoveTime / (1000)) + " sec");
		}
		return (nf.format(avgMoveTime) + " ms");
	}

	private Color getColor(Double val, double lowBound, double upBound) {
		if (Double.compare(Double.NaN, val) == 0) {
			return Color.GREEN;
		} else {
			if (Double.compare(val, lowBound) <= 0) {
				return Color.GREEN;
			} else if (Double.compare(val, upBound) <= 0) {
				return Color.YELLOW;
			} else {
				return Color.RED;
			}
		}
	}

	private double[] getSortedArrayFromSet(Set<Double> sumNodeAvgWorkingTime) {
		double[] arrSumNodeAvgWorking = new double[sumNodeAvgWorkingTime.size()];
		int counter = 0;
		for (double value : sumNodeAvgWorkingTime) {
			arrSumNodeAvgWorking[counter] = value;
			counter++;
		}
		Arrays.sort(arrSumNodeAvgWorking);
		return arrSumNodeAvgWorking;
	}

	private void updateDecorSemantic(FlexNode node, FPDNodeDecorData decor, FlexBinding binding, int frequency) {
		if ((binding.getEncodedInputBinding() != FlexCodec.BLANK)
				&& (binding.getEncodedInputBinding() != FlexCodec.EMPTYSET)) {
			int sizeInpBind = codec.getIOBindingsFor(binding.getEncodedInputBinding()).size();
			int sizeInp = model.getInEdges(node).size();

			// check for and join
			if (sizeInp == sizeInpBind) {
				decor.setAndJoinFreq(decor.getAndJoinFreq() + frequency);
			} else if (sizeInpBind == 1) {
				decor.setXorJoinFreq(decor.getXorJoinFreq() + frequency);
			} else {
				decor.setOrJoinFreq(decor.getOrJoinFreq() + frequency);
			}
		}

		if ((binding.getEncodedOutputBinding() != FlexCodec.BLANK)
				&& (binding.getEncodedOutputBinding() != FlexCodec.EMPTYSET)) {
			int sizeOutBind = codec.getIOBindingsFor(binding.getEncodedOutputBinding()).size();
			int sizeOut = model.getOutEdges(node).size();
			if (sizeOut == sizeOutBind) {
				decor.setAndSplitFreq(decor.getAndSplitFreq() + frequency);
			} else if (sizeOutBind == 1) {
				decor.setXorSplitFreq(decor.getXorSplitFreq() + frequency);
			} else {
				decor.setOrSplitFreq(decor.getOrSplitFreq() + frequency);
			}
		}
	}

	private void uncolorElements() {
		for (FlexNode flexNode : model.getNodes()) {
			flexNode.setDecorator(null);

			// set the attributes
			viewSpecificMap.putViewSpecific(flexNode, AttributeMap.FILLCOLOR, flexNode.isInvisible() ? Color.GRAY
					: Color.ORANGE);
			viewSpecificMap.putViewSpecific(flexNode, AttributeMap.SHOWLABEL, true);
			viewSpecificMap.putViewSpecific(flexNode, AttributeMap.SIZE, new Dimension(NORMALNODEWIDTH,
					NORMALNODEHEIGHT));

		}
		for (FlexEdge<? extends FlexNode, ? extends FlexNode> edge : model.getEdges()) {
			edge.setDecorator(null);

			// set the attributes
			viewSpecificMap.putViewSpecific(edge, AttributeMap.EDGECOLOR, Color.BLACK);
			viewSpecificMap.putViewSpecific(edge, AttributeMap.LINEWIDTH, NORMALLINEWIDTH);
			viewSpecificMap.removeViewSpecific(edge, AttributeMap.TOOLTIP);
		}

		// refresh the figure
		graph.update(model.getNodes());
		graph.update(model.getEdges());
	}

	public void addElementCollection(Map<FlexNode, Double> workingTime, String label) {
		Pair<Map<FlexNode, Double>, String> pair = new Pair<Map<FlexNode, Double>, String>(workingTime, label);

		if (!component.isEnabled()) {
			((DefaultListModel) component.getModel()).removeAllElements();
			component.setEnabled(true);
		}
		if (!((DefaultListModel) component.getModel()).contains(pair)) {
			((DefaultListModel) component.getModel()).addElement(pair);
		}

	}
}

/**
 * This DefaultSelectionModel subclass enables SINGLE_SELECTION mode and
 * overrides setSelectionInterval so that the first selection update in a
 * gesture (like mouse press, drag, release) toggles the current selection
 * state. A "gesture" starts when the first update to the selection model
 * occurs, and the gesture ends when the isAdjusting ListSelectionModel property
 * is set to false.
 */
class ToggleSelectionModel extends DefaultListSelectionModel {
	private static final long serialVersionUID = -8595521046515667658L;
	boolean gestureStarted = false;

	public ToggleSelectionModel() {
		super();
		this.setSelectionMode(SINGLE_SELECTION);
	}

	public void setSelectionInterval(int index0, int index1) {
		if (isSelectedIndex(index0) && !gestureStarted) {
			super.removeSelectionInterval(index0, index1);
		} else {
			super.setSelectionInterval(index0, index1);
		}
		gestureStarted = true;
	}

	public void setValueIsAdjusting(boolean isAdjusting) {
		if (isAdjusting == false) {
			gestureStarted = false;
		}
	}
}

class ViewListCellRenderer implements ListCellRenderer {
	private static final CompoundBorder BORDER = BorderFactory.createCompoundBorder(
			BorderFactory.createRaisedBevelBorder(), BorderFactory.createEmptyBorder(2, 5, 2, 5));;
	private static final CompoundBorder SELBORDER = BorderFactory.createCompoundBorder(
			BorderFactory.createLoweredBevelBorder(), BorderFactory.createEmptyBorder(2, 5, 2, 5));

	private final static JLabel LABEL = SlickerFactory.instance().createLabel("test");
	private static final Border EMPTYBORDER = BorderFactory.createEmptyBorder(2, 5, 2, 5);

	public ViewListCellRenderer() {
		LABEL.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 12));

		LABEL.setBorder(EMPTYBORDER);
		LABEL.setOpaque(false);
		SlickerDecorator.instance().decorate(LABEL);
	}

	@SuppressWarnings("unchecked")
	public Component getListCellRendererComponent(JList component, Object value, int index, boolean isSelected,
			boolean cellHasFocus) {

		Pair<Map<FlexNode, Double>, String> pair = (Pair<Map<FlexNode, Double>, String>) value;

		String label = pair.getSecond();
		LABEL.setText(fitLabelToWidth(LABEL, label, component.getWidth()));
		LABEL.setToolTipText(label);
		LABEL.setForeground(isSelected ? Color.WHITE : Color.BLACK);
		LABEL.setBorder(isSelected ? SELBORDER : BORDER);

		return LABEL;
	}

	public static String fitLabelToWidth(JComponent c, String label, int maxWidth) {
		FontMetrics metrics = c.getFontMetrics(c.getFont());
		Graphics g = c.getGraphics();
		boolean abbreviated = false;
		int width = Integer.MAX_VALUE;
		while (true) {
			if (label.length() < 2) {
				break;
			}
			String test = label;
			if (abbreviated) {
				test += "...";
			}
			Rectangle2D stringBounds = metrics.getStringBounds(test, g);
			width = (int) stringBounds.getWidth();
			if (width > maxWidth) {
				label = label.substring(0, label.length() - 1);
				if (!abbreviated) {
					abbreviated = true;
				}
			} else {
				break;
			}
		}
		if (abbreviated) {
			label += "...";
		}
		return label;
	}
}