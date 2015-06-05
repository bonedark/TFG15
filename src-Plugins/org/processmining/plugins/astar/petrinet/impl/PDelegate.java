package org.processmining.plugins.astar.petrinet.impl;

import java.util.Map;

import nl.tue.storage.CompressedHashSet;
import nl.tue.storage.Deflater;
import nl.tue.storage.Inflater;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.model.XLog;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.plugins.astar.algorithm.State;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;

public class PDelegate extends AbstractPDelegate<DijkstraTail> {

	private final DijkstraTail compressor;
	private final boolean allMarkingsAreFinal;

	public PDelegate(Petrinet net, XLog log, XEventClasses classes, TransEvClassMapping map,
			Map<Transition, Integer> mapTrans2Cost, Map<XEventClass, Integer> mapEvClass2Cost, double delta,
			boolean allMarkingsAreFinal) {
		super(net, log, classes, map, mapTrans2Cost, mapEvClass2Cost, delta);
		this.allMarkingsAreFinal = allMarkingsAreFinal;

		compressor = DijkstraTail.EMPTY;
	}

	public DijkstraTail createTail(PHead head) {
		return DijkstraTail.EMPTY;
	}

	public Inflater<DijkstraTail> getTailInflater() {
		return compressor;
	}

	public Deflater<DijkstraTail> getTailDeflater() {
		return compressor;
	}

	public void setStateSpace(CompressedHashSet<State<PHead, DijkstraTail>> statespace) {

	}

	public boolean isFinal(ShortShortMultiset marking) {
		if (allMarkingsAreFinal) {
			return true;
		} else {
			return super.isFinal(marking);
		}
	}

}
