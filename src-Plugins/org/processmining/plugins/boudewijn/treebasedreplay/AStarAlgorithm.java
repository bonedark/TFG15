package org.processmining.plugins.boudewijn.treebasedreplay;

import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.TObjectShortMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.map.hash.TObjectShortHashMap;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.model.XLog;
import org.processmining.plugins.astar.interfaces.Move;

public class AStarAlgorithm {

	private final TObjectIntMap<TIntList> converted;
	private final XEventClasses classes;
	private final TObjectShortMap<XEventClass> act2index;
	private final XEventClass[] index2act;
	private final int[] act2cost;
	private final XLog log;

	public AStarAlgorithm(XLog log, XEventClasses classes, Map<XEventClass, Integer> activity2Cost) {
		this.log = log;
		this.classes = classes;

		act2index = new TObjectShortHashMap<XEventClass>((short) classes.size(), 0.5f, (short) -1);
		index2act = new XEventClass[classes.size()];
		act2cost = new int[classes.size()];
		short i = 0;
		Collection<XEventClass> sorted = new TreeSet<XEventClass>(classes.getClasses());
		for (XEventClass c : sorted) {
			index2act[i] = c;
			Integer cost = activity2Cost.get(c);
			act2cost[i] = (cost == null ? 0 : cost);
			act2index.put(c, i++);
		}

		converted = new TObjectIntHashMap<TIntList>(log.size());
		for (int j = 0; j < log.size(); j++) {
			converted.adjustOrPutValue(getListEventClass(log, j), 1, 1);
		}
	}

	/**
	 * get list of event class. Record the indexes of non-mapped event classes.
	 * 
	 * @param trace
	 * @param classes
	 * @param mapEvClass2Trans
	 * @param listMoveOnLog
	 * @return
	 */
	private TIntList getListEventClass(XLog log, int trace) {
		int s = log.get(trace).size();
		TIntList result = new TIntArrayList(s);
		for (int i = 0; i < s; i++) {
			int act = getActivityOf(trace, i);
			assert (act != Move.BOTTOM);
			result.add(act);
		}
		return result;
	}

	public XEventClasses getClasses() {
		return classes;
	}

	public short getIndexOf(XEventClass c) {
		return act2index.get(c);
	}

	private short getActivityOf(int trace, int event) {
		XEventClass cls = classes.getClassOf(log.get(trace).get(event));
		return act2index.get(cls);
	}

	public XEventClass getEventClass(short act) {
		return index2act[act];
	}

	public int getLogMoveCost(int i) {
		return act2cost[i];
	}

	public int getTraceFreq(TIntList trace) {
		return converted.get(trace);
	}

	public Iterator<TIntList> traceIterator() {
		return new Iterator<TIntList>() {
			private TObjectIntIterator<TIntList> it = converted.iterator();

			public boolean hasNext() {
				return it.hasNext();
			}

			public TIntList next() {
				it.advance();
				return it.key();
			}

			public void remove() {
				throw new UnsupportedOperationException("Cannot remove trace");
			}

		};
	}

	public int getDifferentTraceCount() {
		return converted.size();
	}
}
