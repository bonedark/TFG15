package org.processmining.plugins.astar.petrinet.impl;

import gnu.trove.list.TIntList;
import gnu.trove.list.TShortList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TShortArrayList;
import gnu.trove.map.TObjectShortMap;
import gnu.trove.map.TShortIntMap;
import gnu.trove.map.TShortObjectMap;
import gnu.trove.map.hash.TObjectShortHashMap;
import gnu.trove.map.hash.TShortIntHashMap;
import gnu.trove.map.hash.TShortObjectHashMap;
import gnu.trove.procedure.TShortShortProcedure;
import gnu.trove.set.TShortSet;
import gnu.trove.set.hash.TShortHashSet;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import nl.tue.storage.EqualOperation;
import nl.tue.storage.HashOperation;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Arc;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.astar.algorithm.State;
import org.processmining.plugins.astar.interfaces.Delegate;
import org.processmining.plugins.astar.interfaces.Move;
import org.processmining.plugins.astar.interfaces.Record;
import org.processmining.plugins.astar.interfaces.Tail;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;

public abstract class AbstractPDelegate<T extends Tail> implements Delegate<PHead, T> {

	public static final short NEV = -1;
	protected final PHeadCompressor<T> headCompressor;
	protected final TObjectShortMap<Place> place2int = new TObjectShortHashMap<Place>(10, 0.5f, NEV);
	protected final TShortObjectMap<Place> int2place = new TShortObjectHashMap<Place>(10, 0.5f, NEV);
	protected final short places;
	protected final TObjectShortMap<XEventClass> act2int = new TObjectShortHashMap<XEventClass>(10, 0.5f, NEV);
	protected final TShortObjectMap<XEventClass> int2act = new TShortObjectHashMap<XEventClass>(10, 0.5f, NEV);
	protected final short activities;
	protected final TObjectShortMap<Transition> trans2int = new TObjectShortHashMap<Transition>(10, 0.5f, NEV);
	protected final TShortObjectMap<Transition> int2trans = new TShortObjectHashMap<Transition>(10, 0.5f, NEV);
	protected final TShortObjectMap<TShortList> actIndex2trans = new TShortObjectHashMap<TShortList>(10, 0.5f, NEV);
	protected final TShortObjectMap<ShortShortMultiset> transIndex2input = new TShortObjectHashMap<ShortShortMultiset>(
			10, 0.5f, NEV);
	protected final TShortObjectMap<ShortShortMultiset> transIndex2output = new TShortObjectHashMap<ShortShortMultiset>(
			10, 0.5f, NEV);
	protected final TShortObjectMap<TShortSet> transIndex2act = new TShortObjectHashMap<TShortSet>();
	protected final short transitions;
	protected final TShortIntMap trans2cost = new TShortIntHashMap();
	protected final TShortIntMap act2cost = new TShortIntHashMap();
	protected final Petrinet net;
	protected final XEventClasses classes;
	protected final Set<ShortShortMultiset> finalMarkings;
	protected final XLog log;
	protected final TShortList unmapped = new TShortArrayList();
	private final double delta;

	public AbstractPDelegate(Petrinet net, XLog log, XEventClasses classes, TransEvClassMapping map,
			Map<Transition, Integer> mapTrans2Cost, Map<XEventClass, Integer> mapEvClass2Cost, double delta,
			Marking... set) {

		this.net = net;
		this.log = log;
		this.classes = classes;
		this.delta = delta;
		this.places = (short) net.getPlaces().size();
		this.transitions = (short) net.getTransitions().size();

		this.finalMarkings = new HashSet<ShortShortMultiset>(set.length);
		// initialize lookup maps
		short i = 0;
		for (Place p : net.getPlaces()) {
			place2int.put(p, i);
			int2place.put(i, p);
			i++;
		}
		i = 0;
		for (Transition t : net.getTransitions()) {
			trans2int.put(t, i);
			int2trans.put(i, t);
			transIndex2input.put(i, new ShortShortMultiset(places));
			transIndex2output.put(i, new ShortShortMultiset(places));
			transIndex2act.put(i, new TShortHashSet(1));
			for (Place p : net.getPlaces()) {
				short pi = place2int.get(p);
				Arc a = net.getArc(p, t);
				if (a != null) {
					transIndex2input.get(i).adjustValue(pi, (short) a.getWeight());
				}
				a = net.getArc(t, p);
				if (a != null) {
					transIndex2output.get(i).adjustValue(pi, (short) a.getWeight());
				}

			}
			trans2cost.put(i, mapTrans2Cost.get(t));

			i++;
		}
		Collection<XEventClass> eventClasses;
		//		eventClasses = new HashSet<XEventClass>(mapEvClass2Cost.keySet());
		//		eventClasses.addAll(classes.getClasses());
		eventClasses = map.values();
		this.activities = (short) eventClasses.size();
		this.headCompressor = new PHeadCompressor<T>(places, activities);

		i = 0;
		for (XEventClass a : eventClasses) {
			act2int.put(a, i);
			actIndex2trans.put(i, new TShortArrayList());
			int2act.put(i, a);
			act2cost.put(i, mapEvClass2Cost.get(a));
			i++;
		}

		i = 0;
		for (Transition t : net.getTransitions()) {
			Set<XEventClass> s = new HashSet<XEventClass>();
			s.add(map.get(t));
			if (s != null && !s.isEmpty()) {
				for (XEventClass e : s) {
					short a = act2int.get(e);
					if (a == NEV) {
						// somehow, the map contains a event class that is not part of the eventclasses
						// provided (for example a dummy event class).
						// We do not cater for that here.

						continue;
					}
					actIndex2trans.get(a).add(i);
					transIndex2act.get(i).add(a);

				}
			}
			if (transIndex2act.get(i).isEmpty()) {
				unmapped.add(i);
			}
			i++;
		}

		for (Marking m : set) {
			ShortShortMultiset mi = new ShortShortMultiset(places);
			for (Place p : m.baseSet()) {
				mi.put(place2int.get(p), m.occurrences(p).shortValue());
			}
			finalMarkings.add(mi);
		}

	}

	public boolean isEnabled(int transition, final ShortShortMultiset marking) {
		ShortShortMultiset needed = transIndex2input.get((short) transition);
		return needed.forEachEntry(new TShortShortProcedure() {

			public boolean execute(short place, short toConsume) {
				return marking.get(place) >= toConsume;
			}
		});
	}

	public short getIndexOf(Place p) {
		return place2int.get(p);
	}

	public short numPlaces() {
		return places;
	}

	public Place getPlace(short i) {
		return int2place.get(i);
	}

	public short getIndexOf(Transition t) {
		return trans2int.get(t);
	}

	public short numTransitions() {
		return transitions;
	}

	public Transition getTransition(short i) {
		return int2trans.get(i);
	}

	public short getIndexOf(XEventClass c) {
		return act2int.get(c);
	}

	public short numEventClasses() {
		return activities;
	}

	public XEventClass getEventClass(short i) {
		return int2act.get(i);
	}

	public Record createInitialRecord(PHead head) {
		PRecord r = new PRecord(0, null, head.getMarking().getNumElts());
		r.setEstimatedRemainingCost(0);
		return r;
	}

	public PHeadCompressor<T> getHeadInflater() {
		return headCompressor;
	}

	public PHeadCompressor<T> getHeadDeflater() {
		return headCompressor;
	}

	public XEventClass getClassOf(XEvent event) {
		return classes.getClassOf(event);
	}

	public short getActivityOf(int trace, int event) {
		if (trace < 0 || event < 0 || trace >= log.size() || event >= log.get(trace).size()) {
			return Move.BOTTOM;
		}
		XEventClass cls = classes.getClassOf(log.get(trace).get(event));
		short a = act2int.get(cls);
		if (a < 0) {
			return Move.BOTTOM;
		} else {
			return a;
		}
	}

	public boolean isFinal(ShortShortMultiset marking) {
		return finalMarkings.contains(marking);
	}

	public HashOperation<State<PHead, T>> getHeadBasedHashOperation() {
		return headCompressor;
	}

	public EqualOperation<State<PHead, T>> getHeadBasedEqualOperation() {
		return headCompressor;
	}

	public Petrinet getPetrinet() {
		return net;
	}

	public XTrace getTrace(int t) {
		return log.get(t);
	}

	public double getCostFor(short modelMove, short activity) {
		if (modelMove == Move.BOTTOM) {
			// move on log only
			return getCostForMoveLog(activity);
		}
		if (activity == Move.BOTTOM) {
			return getCostForMoveModel(modelMove);
		}
		// synchronous move assumed here
		return delta;
	}

	public double getCostForMoveLog(short activity) {
		if (activity == Move.BOTTOM) {
			return 0;
		}
		return delta + act2cost.get(activity);
	}

	public double getCostForMoveModel(short transition) {
		if (transition == Move.BOTTOM) {
			return 0;
		}
		return delta + trans2cost.get(transition);
	}

	public TShortList getTransitions(short activity) {
		return actIndex2trans.get(activity);
	}

	public TShortSet getActivitiesFor(short transition) {
		return transIndex2act.get(transition);
	}

	public boolean hasEnabledTransitions(ShortShortMultiset marking) {
		for (int t = 0; t < transitions; t++) {
			if (isEnabled(t, marking)) {
				return true;
			}
		}
		return false;
	}

	public TIntList getEnabledTransitionsChangingMarking(ShortShortMultiset marking) {
		TIntList list = new TIntArrayList();
		for (short t = 0; t < transitions; t++) {
			if (isEnabled(t, marking) && !transIndex2input.get(t).equals(transIndex2output.get(t))) {
				list.add(t);
			}
		}
		return list;
	}

	public ShortShortMultiset getInputOf(short transition) {
		return transIndex2input.get(transition);
	}

	public ShortShortMultiset getOutputOf(short transition) {
		return transIndex2output.get(transition);
	}

	public Set<ShortShortMultiset> getFinalMarkings() {
		return finalMarkings;
	}

	public double getDelta() {
		return delta;
	}

}