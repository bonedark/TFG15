package org.processmining.plugins.astar.petrinet.impl;

import gnu.trove.iterator.TShortIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.TShortList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.procedure.TShortShortProcedure;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.astar.interfaces.Delegate;
import org.processmining.plugins.astar.interfaces.Head;
import org.processmining.plugins.astar.interfaces.Move;
import org.processmining.plugins.astar.interfaces.Record;
import org.processmining.plugins.astar.interfaces.Tail;

/**
 * The head is basically two vectors, one for the marking and one for the parikh
 * vector.
 * 
 * @author bfvdonge
 * 
 */
public class PHead implements Head {

	// The maximum size of this.super:             16
	private final ShortShortMultiset marking; //    8 + 2 * numplaces + 48 
	private final ShortShortMultiset parikh; //     8 + 2 * numactivities + 48
	private final int bitsForParikh; //             4
	private final int hashCode; //                  4  

	public static int computeBitsForParikh(short acts, short places) {
		return Math.max(4, (int) (32.0 * acts / (acts + places)));
	}

	public static int getSizeFor(int places, int activities) {
		return 8 * (1 + (40 + ShortShortMultiset.getSizeInMemory(places)
				+ ShortShortMultiset.getSizeInMemory(activities) - 1) / 8);
	}

	public PHead(AbstractPDelegate<?> delegate, Marking m, XTrace t) {
		marking = new ShortShortMultiset(delegate.numPlaces());
		parikh = new ShortShortMultiset(delegate.numEventClasses());

		for (Place p : m.baseSet()) {
			short i = delegate.getIndexOf(p);
			marking.put(i, m.occurrences(p).shortValue());
		}
		for (XEvent e : t) {
			XEventClass c = delegate.getClassOf(e);
			if (c != null) {
				short key = delegate.getIndexOf(c);
				if (key >= 0) {
					parikh.adjustValue(key, (short) 1);
				}
			}
		}
		bitsForParikh = computeBitsForParikh(delegate.numEventClasses(), delegate.numPlaces());
		hashCode = hashMarking(marking, delegate.numPlaces()) + hashParikh(parikh, delegate.numEventClasses());
	}

	PHead(ShortShortMultiset marking, ShortShortMultiset parikh, int hashCode, int bitsForParikh) {
		this.marking = marking;
		this.parikh = parikh;
		this.hashCode = hashCode;
		this.bitsForParikh = bitsForParikh;
	}

	private int hashParikh(ShortShortMultiset set, short max) {
		int h = 0;
		for (short i = max; i-- > 0;) {
			// for the multiset, flip the corresponding bit as
			// often as given in the set.
			if (set.get(i) % 2 == 1) {
				h ^= 1 << (i % bitsForParikh);
			}
		}
		return h;
	}

	private int hashMarking(ShortShortMultiset set, short max) {
		int h = 0;
		for (short i = max; i-- > 0;) {
			// for the multiset, flip the corresponding bit as
			// often as given in the set.
			if (set.get(i) % 2 == 1) {
				h ^= 1 << (bitsForParikh + (i % (32 - bitsForParikh)));
			}
		}
		return h;
	}

	public PHead getNextHead(Record rec, Delegate<? extends Head, ? extends Tail> d, int modelMove, int logMove,
			int activity) {
		AbstractPDelegate<?> delegate = (AbstractPDelegate<?>) d;

		final HashContainer hash = new HashContainer(hashCode);
		final ShortShortMultiset newMarking;
		if (modelMove != Move.BOTTOM) {
			newMarking = marking.clone();
			// clone the marking
			ShortShortMultiset in = delegate.getInputOf((short) modelMove);
			ShortShortMultiset out = delegate.getOutputOf((short) modelMove);

			in.forEachEntry(new TShortShortProcedure() {

				public boolean execute(short place, short needed) {
					newMarking.adjustValue(place, (short) -needed);
					if (needed % 2 == 1) {
						hash.hash ^= 1 << (bitsForParikh + (place % (32 - bitsForParikh)));
					}
					return true;
				}
			});
			out.forEachEntry(new TShortShortProcedure() {

				public boolean execute(short place, short produced) {
					newMarking.adjustValue(place, produced);
					if (produced % 2 == 1) {
						hash.hash ^= 1 << (bitsForParikh + (place % (32 - bitsForParikh)));
					}
					return true;
				}
			});
		} else {
			newMarking = marking;
		}

		final ShortShortMultiset newParikh;
		if (logMove != Move.BOTTOM) {
			newParikh = parikh.clone();
			newParikh.adjustValue((short) activity, (short) -1);
			hash.hash ^= 1 << (activity % bitsForParikh);
		} else {
			newParikh = parikh;
		}

		return new PHead(newMarking, newParikh, hash.hash, bitsForParikh);
	}

	public TIntList getModelMoves(Record rec, Delegate<? extends Head, ? extends Tail> d) {
		return ((AbstractPDelegate<?>) d).getEnabledTransitionsChangingMarking(marking);
	}

	public TIntList getModelMoves(Record rec, Delegate<? extends Head, ? extends Tail> d, TIntList enabled, int activity) {
		final AbstractPDelegate<?> delegate = (AbstractPDelegate<?>) d;

		// only consider transitions mapped to activity
		final TIntList result = new TIntArrayList();
		TShortList trans = delegate.getTransitions((short) activity);
		TShortIterator it = trans.iterator();
		while (it.hasNext()) {
			int i = it.next();
			if (delegate.isEnabled(i, marking)) {
				result.add(i);
			}
		}

		return result;

	}

	public boolean isFinal(Delegate<? extends Head, ? extends Tail> d) {
		AbstractPDelegate<?> delegate = (AbstractPDelegate<?>) d;
		return parikh.isEmpty() && (delegate.isFinal(marking) || !delegate.hasEnabledTransitions(marking));
	}

	protected Marking fromMultiSet(AbstractPDelegate<?> delegate) {
		Marking m = new Marking();
		for (short i = 0; i < delegate.numPlaces(); i++) {
			if (marking.get(i) > 0) {
				m.add(delegate.getPlace(i), (int) marking.get(i));
			}
		}
		return m;
	}

	public ShortShortMultiset getMarking() {
		return marking;
	}

	public ShortShortMultiset getParikhVector() {
		return parikh;
	}

	public int hashCode() {
		return hashCode;
	}

	public boolean equals(Object o) {
		return (o != null) && (o instanceof PHead) && (((PHead) o).marking.equals(marking))
				&& (((PHead) o).parikh.equals(parikh));
	}

	public String toString() {
		return "[m:" + marking + " s:" + parikh + "]";
	}

	private static final class HashContainer {

		public HashContainer(int hashCode) {
			hash = hashCode;
		}

		public int hash;
	}

}
