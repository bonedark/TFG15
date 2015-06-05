/**
 * 
 */
package org.processmining.plugins.petrinet.replayresult.visualization.projection;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingConstants;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.model.XLog;
import org.jgraph.event.GraphSelectionEvent;
import org.jgraph.event.GraphSelectionListener;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.util.ui.scalableview.ScalableComponent;
import org.processmining.framework.util.ui.widgets.InspectorPanel;
import org.processmining.models.connections.GraphLayoutConnection;
import org.processmining.models.graphbased.AttributeMap;
import org.processmining.models.graphbased.AttributeMap.ArrowType;
import org.processmining.models.graphbased.ViewSpecificAttributeMap;
import org.processmining.models.graphbased.directed.DirectedGraph;
import org.processmining.models.graphbased.directed.DirectedGraphNode;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.InhibitorArc;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.ResetArc;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.jgraph.ProMGraphModel;
import org.processmining.models.jgraph.ProMJGraph;
import org.processmining.models.jgraph.elements.ProMGraphCell;
import org.processmining.models.pnetprojection.PetrinetGraphP;
import org.processmining.models.pnetprojection.PlaceP;
import org.processmining.models.pnetprojection.TransitionP;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;
import org.processmining.plugins.replayer.util.StepTypes;

import com.fluxicon.slickerbox.factory.SlickerDecorator;
import com.fluxicon.slickerbox.factory.SlickerFactory;
import com.jgraph.layout.JGraphFacade;
import com.jgraph.layout.hierarchical.JGraphHierarchicalLayout;

/**
 * @author aadrians Oct 26, 2011
 * 
 */
public class PNLogReplayProjectedVisPanel extends InspectorPanel {
	private static final long serialVersionUID = -6674503536171244970L;

	// GUI component
	protected final ScalableComponent scalable;
	protected JScrollPane scroll;
	private ViewPanel viewPanel;
	private JComponent exportPanel;
	private StatPanel statPanel;

	// slicker factory
	protected SlickerFactory factory;
	protected SlickerDecorator decorator;

	// for graph visualization
	private final ProMJGraph graph;
	private PetrinetGraphP newNet;
	private Marking mNewNet;
	private boolean[] placeWithMoveOnLog;

	private Color moveOnLogColor = new Color(255, 255, 0, 200);
	private Color involvedMoveOnLogColor = new Color(255, 0, 0, 200);
	private Color transparentColor = new Color(255, 255, 255, 0);

	private Color moveOnModelColor = new Color(139, 0, 139, 100);
	private Color moveLogModelColor = new Color(0, 255, 0, 200);

	// zoom-related properties
	// The maximal zoom factor for the primary view on the transition system.
	public static final int MAX_ZOOM = 1200;

	// reference to original
	protected Map<PetrinetNode, PetrinetNode> mapOrig2ViewNode = new HashMap<PetrinetNode, PetrinetNode>();

	// reference to info
	private CoreInfoProvider provider;

	// reference to log replay result
	private TransEvClassMapping mapping;
	private XLog log;
	private PNRepResult logReplayResult;

	/**
	 * @return the scalable
	 */
	public ScalableComponent getScalable() {
		return scalable;
	}

	public PNLogReplayProjectedVisPanel(PluginContext context, PetrinetGraph origNet, Marking origMarking, XLog log,
			TransEvClassMapping map, PNRepResult logReplayResult) {
		/**
		 * Get some Slickerbox stuff, required by the Look+Feel of some objects.
		 */
		factory = SlickerFactory.instance();
		decorator = SlickerDecorator.instance();

		/**
		 * Shared stuffs
		 */
		this.log = log;
		this.mapping = map;
		this.logReplayResult = logReplayResult;

		// net and marking to be modified
		newNet = new PetrinetGraphP("Projected " + origNet.getLabel());

		mNewNet = new Marking();
		constructPetrinetP(origNet, origMarking, newNet, mNewNet, mapOrig2ViewNode);

		// calculate info
		provider = new CoreInfoProvider(newNet, mNewNet, map, log, mapOrig2ViewNode, logReplayResult);

		this.placeWithMoveOnLog = new boolean[provider.getNumPlaces()];

		/**
		 * TAB INFO
		 */
		// add info
		statPanel = createStatPanel(provider);
		addInfo("Statistics", statPanel);

		/**
		 * Main visualization (has to be after creating provider)
		 */
		scalable = buildJGraph(newNet);
		graph = (ProMJGraph) scalable;

		graph.addGraphSelectionListener(new GraphSelectionListener() {
			public void valueChanged(GraphSelectionEvent e) {
				// selection of a transition would change the stats
				if (e.getCell() instanceof ProMGraphCell) {
					DirectedGraphNode cell = ((ProMGraphCell) e.getCell()).getNode();
					if (cell instanceof TransitionP) {
						statPanel.setTransition((TransitionP) cell);
					} else if (cell instanceof PlaceP) {
						statPanel.setPlace((PlaceP) cell);
					}

					boolean[] involvedPlacesFlag = statPanel.getInvolvedPlaces();
					PlaceP[] placeArray = provider.getPlaceArray();
					graph.getModel().beginUpdate();
					for (int i = 0; i < involvedPlacesFlag.length; i++) {
						if (involvedPlacesFlag[i]) {
							placeArray[i].getAttributeMap().put(AttributeMap.FILLCOLOR, involvedMoveOnLogColor);
						} else {
							if (placeWithMoveOnLog[i]) {
								placeArray[i].getAttributeMap().put(AttributeMap.FILLCOLOR, moveOnLogColor);
							} else {
								placeArray[i].getAttributeMap().put(AttributeMap.FILLCOLOR, transparentColor);
							}
						}
					}
					graph.getModel().endUpdate();
					graph.refresh();
				}
			}
		});

		for (Place p : mNewNet) {
			String label = "" + mNewNet.occurrences(p);
			p.getAttributeMap().put(AttributeMap.LABEL, label);
			p.getAttributeMap().put(AttributeMap.SHOWLABEL, !label.equals(""));
		}

		scroll = new JScrollPane(scalable.getComponent());
		decorator.decorate(scroll, Color.WHITE, Color.GRAY, Color.DARK_GRAY);
		setLayout(new BorderLayout());
		add(scroll);

		/**
		 * TAB DISPLAY SETTING
		 */
		// add additional tab for display settings
		JPanel displayMP = getInspector().addTab("Display");

		// add view panel (zoom in/out)
		viewPanel = createViewPanel(this, MAX_ZOOM);
		addInteractionViewports(viewPanel);

		// add filtering
		ShowHideMovementPanel visFilter = createVisualizationPanel(this);

		// add export image
		exportPanel = createExportPanel(scalable);

		// add elements
		getInspector().addGroup(displayMP, "View", viewPanel);
		getInspector().addGroup(displayMP, "Show/Ignore Movements in Projection", visFilter);
		getInspector().addGroup(displayMP, "Export", exportPanel);

		/**
		 * TAB FILTER ALIGNMENT
		 */
		// add additional tab for filtering alignments
		JPanel filterMP = getInspector().addTab("Filter");

		AlignmentFilterPanel alignFilter = createAlignmentFilter();
		getInspector().addGroup(filterMP, "Set Minimum Movement that Should be Contained in an Alignment", alignFilter);

		// initialize decorator for transitions
		TransitionP[] transArr = provider.getTransArray();
		int pointer = 0;
		while (pointer < provider.getNumTrans()) {
			int[] info = provider.getInfoNode(pointer);
			transArr[pointer].setDecorator(new TransDecorator(info[3], info[0], transArr[pointer].getLabel(),
					moveLogModelColor, moveOnModelColor));
			pointer++;
		}

		constructVisualization(true, true);
		constructPlaceVisualization();

		validate();
		repaint();
	}

	private void constructPlaceVisualization() {
		// update place visualization
		PlaceP[] placeArr = provider.getPlaceArray();
		short[] freq = provider.getPlaceFreq();

		short min = Short.MAX_VALUE;
		short max = Short.MIN_VALUE;

		for (int i = 0; i < freq.length; i++) {
			if (freq[i] < min) {
				min = freq[i];
			}
			if (freq[i] > max) {
				max = freq[i];
			}
		}

		double median = (max - min) / 2;
		int medianPlaceRadius = 30;
		int flexibility = 10;
		for (int i = 0; i < placeArr.length; i++) {
			int size = medianPlaceRadius + (int) Math.floor((freq[i] - median) * flexibility / median);
			if (freq[i] > 0) {
				placeArr[i].getAttributeMap().put(AttributeMap.FILLCOLOR, moveOnLogColor);
				this.placeWithMoveOnLog[i] = true;
			} else {
				placeArr[i].getAttributeMap().put(AttributeMap.FILLCOLOR, transparentColor);
				this.placeWithMoveOnLog[i] = false;
			}
			placeArr[i].getAttributeMap().put(AttributeMap.SIZE, new Dimension(size, size));
		}
	}

	public void constructVisualization(boolean isShowMoveLogModel, boolean isShowMoveModelOnly) {
		graph.getModel().beginUpdate();
		/**
		 * Update main visualization (add decoration, size)
		 */
		int defTransWidth = 80;
		int defTransHeight = 50;

		int elasticityWidth = 30;
		int elasticityHeight = 30;

		int[] minMaxFreq = provider.getMinMaxFreq(isShowMoveLogModel, isShowMoveModelOnly);

		double median = minMaxFreq[1] + minMaxFreq[0] / 2;
		double distance = minMaxFreq[1] - minMaxFreq[0] / 2;

		TransitionP[] transArr = provider.getTransArray();
		int pointer = 0;

		while (pointer < provider.getNumTrans()) {
			TransDecorator dec = (TransDecorator) transArr[pointer].getDecorator();
			int[] info = provider.getInfoNode(pointer);
			int occurrence = 0;
			if (isShowMoveLogModel) {
				dec.setMoveSyncFreq(info[0]);
				occurrence += info[0];
			} else {
				dec.setMoveSyncFreq(0);
			}
			if (isShowMoveModelOnly) {
				dec.setMoveOnModelFreq(info[3]);
				occurrence += info[3];
			} else {
				dec.setMoveOnModelFreq(0);
			}

			int suggestedWidth = defTransWidth + (int) Math.floor(elasticityWidth * (occurrence - median) / distance);
			int suggestedHeight = defTransHeight
					+ (int) Math.floor(elasticityHeight * (occurrence - median) / distance);
			float suggestedArcWidth = new Float(0.1f + Math.log(Math.E) * Math.log10(occurrence));
			assert(occurrence <= minMaxFreq[1]);
			int suggestedColor = minMaxFreq[1] > 0 ? (int) ((1 - (occurrence / (double) minMaxFreq[1])) * 255) : 255;

			Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> edges = newNet
					.getInEdges(transArr[pointer]);
			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : edges) {
				edge.getAttributeMap().put(AttributeMap.EDGECOLOR,
						new Color(suggestedColor, suggestedColor, suggestedColor));
				edge.getAttributeMap().put(AttributeMap.EDGEEND, ArrowType.ARROWTYPE_NONE);
				edge.getAttributeMap().put(AttributeMap.LINEWIDTH, suggestedArcWidth);
			}
			edges = newNet.getOutEdges(transArr[pointer]);
			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : edges) {
				edge.getAttributeMap().put(AttributeMap.EDGECOLOR,
						new Color(suggestedColor, suggestedColor, suggestedColor));
				edge.getAttributeMap().put(AttributeMap.EDGEEND, ArrowType.ARROWTYPE_NONE);
				edge.getAttributeMap().put(AttributeMap.LINEWIDTH, suggestedArcWidth);
			}
			transArr[pointer].getAttributeMap().put(AttributeMap.SIZE, new Dimension(suggestedWidth, suggestedHeight));
			pointer++;
		}

		graph.getModel().endUpdate();
		graph.refresh();
	}

	private AlignmentFilterPanel createAlignmentFilter() {
		return new AlignmentFilterPanel(this, provider.getTransArray(), provider.getEvClassArray());
	}

	private ShowHideMovementPanel createVisualizationPanel(PNLogReplayProjectedVisPanel mainPanel) {
		return new ShowHideMovementPanel(mainPanel);
	}

	private JComponent createExportPanel(ScalableComponent graph) {
		return new ExportPanel(graph);
	}

	private ViewPanel createViewPanel(PNLogReplayProjectedVisPanel mainPanel, int maxZoom) {
		return new ViewPanel(this, maxZoom);
	}

	private StatPanel createStatPanel(CoreInfoProvider provider) {
		return new StatPanel(provider);
	}

	private static ProMJGraph buildJGraph(DirectedGraph<?, ?> net) {

		ViewSpecificAttributeMap map = new ViewSpecificAttributeMap();
		GraphLayoutConnection layoutConnection = new GraphLayoutConnection(net);

		ProMGraphModel model = new ProMGraphModel(net);
		ProMJGraph jGraph = new ProMJGraph(model, map, layoutConnection);

		JGraphHierarchicalLayout layout = new JGraphHierarchicalLayout();
		layout.setDeterministic(false);
		layout.setCompactLayout(false);
		layout.setFineTuning(true);
		layout.setParallelEdgeSpacing(15);
		layout.setFixRoots(false);

		layout.setOrientation(map.get(net, AttributeMap.PREF_ORIENTATION, SwingConstants.SOUTH));

		if (!layoutConnection.isLayedOut()) {

			JGraphFacade facade = new JGraphFacade(jGraph);

			facade.setOrdered(false);
			facade.setEdgePromotion(true);
			facade.setIgnoresCellsInGroups(false);
			facade.setIgnoresHiddenCells(false);
			facade.setIgnoresUnconnectedCells(false);
			facade.setDirected(true);
			facade.resetControlPoints();
			facade.run(layout, true);

			java.util.Map<?, ?> nested = facade.createNestedMap(true, true);

			jGraph.getGraphLayoutCache().edit(nested);
			layoutConnection.setLayedOut(true);
		}

		jGraph.setUpdateLayout(layout);

		layoutConnection.updated();

		return jGraph;
	}

	public JComponent getComponent() {
		return scalable.getComponent();
	}

	public void setScale(double d) {
		double b = Math.max(d, 0.01);
		b = Math.min(b, MAX_ZOOM / 100.);
		scalable.setScale(b);
	}

	public double getScale() {
		return scalable.getScale();
	}

	public JViewport getViewport() {
		return scroll.getViewport();
	}

	public Component getVerticalScrollBar() {
		return scroll.getVerticalScrollBar();
	}

	public Component getHorizontalScrollBar() {
		return scroll.getHorizontalScrollBar();
	}

	public void addInteractionViewports(final ViewPanel viewPanel) {
		this.scroll.addComponentListener(new ComponentListener() {

			public void componentHidden(ComponentEvent arg0) {
			}

			public void componentMoved(ComponentEvent arg0) {
			}

			public void componentResized(ComponentEvent arg0) {

				if (arg0.getComponent().isValid()) {

					Dimension size = arg0.getComponent().getSize();

					int width = 250, height = 250;

					if (size.getWidth() > size.getHeight())
						height *= size.getHeight() / size.getWidth();
					else
						width *= size.getWidth() / size.getHeight();

					viewPanel.getPIP().setPreferredSize(new Dimension(width, height));
					viewPanel.getPIP().initializeImage();

					viewPanel.getZoom().computeFitScale();
				}
			}

			public void componentShown(ComponentEvent arg0) {
			}

		});
	}

	/**
	 * Copy original net to for a visualization graph
	 * 
	 * @param net
	 * @param initMarking
	 * @param newNet
	 * @param newMarking
	 */
	private void constructPetrinetP(PetrinetGraph net, Marking initMarking, PetrinetGraphP newNet, Marking newMarking,
			Map<PetrinetNode, PetrinetNode> mapOrig2ViewNode) {
		// copy entire petrinet and visualize

		for (Place p : net.getPlaces()) {
			PlaceP newPlace = newNet.addPlace(p.getLabel());
			mapOrig2ViewNode.put(p, newPlace);
		}
		for (Transition t : net.getTransitions()) {
			TransitionP newTrans = newNet.addTransition(t.getLabel());
			newTrans.setInvisible(t.isInvisible());
			newTrans.getAttributeMap().put(AttributeMap.SHOWLABEL, false);
			mapOrig2ViewNode.put(t, newTrans);
		}
		for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : net.getEdges()) {
			if (edge instanceof ResetArc) {
				newNet.addResetArc((PlaceP) mapOrig2ViewNode.get(edge.getSource()),
						(TransitionP) mapOrig2ViewNode.get(edge.getTarget()), edge.getLabel());
			} else if (edge instanceof InhibitorArc) {
				newNet.addInhibitorArc((PlaceP) mapOrig2ViewNode.get(edge.getSource()),
						(TransitionP) mapOrig2ViewNode.get(edge.getTarget()), edge.getLabel());
			} else {
				if (edge.getSource() instanceof Transition) {
					newNet.addArc((TransitionP) mapOrig2ViewNode.get(edge.getSource()),
							(PlaceP) mapOrig2ViewNode.get(edge.getTarget()),
							net.getArc(edge.getSource(), edge.getTarget()).getWeight());
				} else {
					newNet.addArc((PlaceP) mapOrig2ViewNode.get(edge.getSource()),
							(TransitionP) mapOrig2ViewNode.get(edge.getTarget()),
							net.getArc(edge.getSource(), edge.getTarget()).getWeight());
				}
			}
		}

		// copy initial marking
		for (Place p : initMarking) {
			newMarking.add((PlaceP) mapOrig2ViewNode.get(p), initMarking.occurrences(p));
		}
	}

	/**
	 * Recalculate all info as alignment is filtered
	 * 
	 * @param existMoveSync
	 * @param existsMoveModelOnly
	 * @param existsMoveLogOnly
	 */
	public void filterAlignment(boolean[] existMoveSync, boolean[] existsMoveModelOnly, boolean[] existsMoveLogOnly) {

		// create mapping from transition to index
		TransitionP[] temp = this.provider.getTransArray();
		Map<Transition, Integer> mapTrans2Idx = new HashMap<Transition, Integer>(temp.length);
		loop: for (PetrinetNode t : mapOrig2ViewNode.keySet()) {
			PetrinetNode trans = mapOrig2ViewNode.get(t);
			for (int i = 0; i < temp.length; i++) {
				if (temp[i].equals(trans)) {
					mapTrans2Idx.put((Transition) t, i);
					continue loop;
				}
			}
		}

		// create mapping from event class to index
		XEventClass[] ecArray = this.provider.getEvClassArray();
		Map<XEventClass, Integer> mapEc2Int = new HashMap<XEventClass, Integer>(ecArray.length);
		for (int i = 0; i < ecArray.length; i++) {
			mapEc2Int.put(ecArray[i], i);
		}

		// now filter all syncRepResult
		boolean[] filter = new boolean[this.logReplayResult.size()];
		Arrays.fill(filter, false);

		int idx = 0;
		repResultLoop: for (SyncReplayResult repResult : this.logReplayResult) {
			Iterator<Object> nit = repResult.getNodeInstance().iterator();
			Iterator<StepTypes> sit = repResult.getStepTypes().iterator();
			while (sit.hasNext()) {
				switch (sit.next()) {
					case L :
						if (existsMoveLogOnly[mapEc2Int.get(nit.next())]) {
							filter[idx++] = true;
							continue repResultLoop;
						}
						break;
					case MINVI :
					case MREAL :
						if (existsMoveModelOnly[mapTrans2Idx.get(nit.next())]) {
							filter[idx++] = true;
							continue repResultLoop;
						}
						break;
					case LMGOOD :
						if (existMoveSync[mapTrans2Idx.get(nit.next())]) {
							filter[idx++] = true;
							continue repResultLoop;
						}
						break;
					case LMNOGOOD :
						// no need to consider this case
						filter[idx++] = false;
						continue repResultLoop;
				}
			}
			// not wanted by all filtering
			filter[idx++] = false;
		}

		this.provider = new CoreInfoProvider(newNet, mNewNet, mapping, log, mapOrig2ViewNode, logReplayResult, filter);

		// reconstruct stats panel
		statPanel.setInfoProvider(provider);
		statPanel.repaint();

		constructVisualization(true, true);
		constructPlaceVisualization();

		validate();
		repaint();
	}
}
