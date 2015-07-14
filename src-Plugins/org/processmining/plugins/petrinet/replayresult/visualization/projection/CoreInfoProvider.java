/**
 * 
 */
package org.processmining.plugins.petrinet.replayresult.visualization.projection;

import gnu.trove.list.array.TShortArrayList;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XLog;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.InhibitorArc;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.ResetArc;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.pnetprojection.PetrinetGraphP;
import org.processmining.models.pnetprojection.PlaceP;
import org.processmining.models.pnetprojection.TransitionP;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;
import org.processmining.plugins.replayer.util.StepTypes;

/**
 * @author aadrians Nov 3, 2011
 * 
 */
public class CoreInfoProvider {

	/**
	 * INTERNAL DATA STRUCTURE
	 */
	// pointer to transition that represent move on model/move sync result
	private TransitionP[] transArray;
	private PlaceP[] placeArray;
	private XEventClass[] ecArray;

	// array that stores frequency information as a big array of short:
	// [Freq. of transition related | Freq. of marking related ]
	//
	// 5 elements of Freq. of transition related: 
	// 1. freq move on model,
	// 2. freq unique case where move on model occur
	// 3. freq sync, 
	// 4. freq unique case where sync occur
	// 5. freq sync in 100% fit cases
	//
	// Freq of marking related:
	// index 0 - num. Places : marking where move on log occur
	// next (num. of ev classes) : total occurrence
	// next (num. of ev classes) : num of unique trace where deviation occur
	private TShortArrayList encodedStats = null;
	private int transStatBlockSize; // size of elements of Freq. of transition related 
	private int markingStatBlockSize; // size of elements of Freq. of marking related

	private int sumAllMoveLogModel = 0;
	private int sumAllMoveModelOnly = 0;
	private int sumAllMoveLogOnly = 0;
	private int totalTrace = 0;

	@SuppressWarnings("unused")
	private CoreInfoProvider() {
	};

	public CoreInfoProvider(final PetrinetGraphP newNet, Marking mNewNet, TransEvClassMapping mapping, XLog log,
			Map<PetrinetNode, PetrinetNode> mapOrig2ViewNode, Set<SyncReplayResult> logReplayResult) {
		boolean[] filter = new boolean[logReplayResult.size()];
		Arrays.fill(filter, true);
		
		initializeEvClassArray(log, mapping);
		initializeTransAndPlaceArray(newNet);
		transStatBlockSize = 5;
		markingStatBlockSize = placeArray.length + (ecArray.length * 2);
		extractInfo(newNet, mNewNet, mapOrig2ViewNode, logReplayResult, filter);
	}

	public CoreInfoProvider(PetrinetGraphP newNet, Marking mNewNet, TransEvClassMapping mapping, XLog log,
			Map<PetrinetNode, PetrinetNode> mapOrig2ViewNode, PNRepResult logReplayResult, boolean[] filter) {
		initializeEvClassArray(log, mapping);
		initializeTransAndPlaceArray(newNet);
		transStatBlockSize = 5;
		markingStatBlockSize = placeArray.length + (ecArray.length * 2);
		extractInfo(newNet, mNewNet, mapOrig2ViewNode, logReplayResult, filter);
	}

	private void initializeTransAndPlaceArray(PetrinetGraphP newNet) {
		// transition array
		Collection<Transition> transCol = newNet.getTransitions();
		transArray = transCol.toArray(new TransitionP[transCol.size()]);

		// place array
		Collection<Place> placeCol = newNet.getPlaces();
		placeArray = placeCol.toArray(new PlaceP[placeCol.size()]);
	}

	private void initializeEvClassArray(XLog log, TransEvClassMapping mapping) {
		XLogInfo summary = XLogInfoFactory.createLogInfo(log, mapping.getEventClassifier());
		Collection<XEventClass> classes = summary.getEventClasses().getClasses();

		if (mapping.getDummyEventClass() != null) {
			ecArray = new XEventClass[classes.size() + 1];
			int ecCounter = 0;
			for (XEventClass ec : classes) {
				ecArray[ecCounter++] = ec;
			}
			ecArray[ecArray.length - 1] = mapping.getDummyEventClass();
		} else {
			ecArray = classes.toArray(new XEventClass[classes.size()]);
		}
	}

	/**
	 * @return the numRealTrans
	 */
	public int getNumTrans() {
		return transArray.length;
	}

	/**
	 * @param newNet
	 * @param mNewNet
	 * @param mapOrig2ViewNode
	 * @param repResult
	 */
	private void extractInfo(final PetrinetGraphP newNet, Marking mNewNet,
			Map<PetrinetNode, PetrinetNode> mapOrig2ViewNode, Set<SyncReplayResult> repResult, boolean[] filter) {
		// create event class reference for fast lokup
		Map<XEventClass, Integer> mapEc2Int = new HashMap<XEventClass, Integer>(ecArray.length);
		for (int i = 0; i < ecArray.length; i++) {
			mapEc2Int.put(ecArray[i], i);
		}

		// create transition reference for fast lookup
		Map<TransitionP, Integer> mapTrans2Int = new HashMap<TransitionP, Integer>(transArray.length);
		for (int i = 0; i < transArray.length; i++) {
			mapTrans2Int.put(transArray[i], i);
		}

		// create place transition reference for fast lookup
		Map<PlaceP, Integer> mapPlace2Int = new HashMap<PlaceP, Integer>(placeArray.length);
		for (int i = 0; i < placeArray.length; i++) {
			mapPlace2Int.put(placeArray[i], i);
		}

		// initialize all encoded result
		// initial capacity allows that each place is a marking
		encodedStats = new TShortArrayList((transStatBlockSize * transArray.length)
				+ (placeArray.length * markingStatBlockSize));
		for (int i = 0; i < (transStatBlockSize * transArray.length); i++) {
			encodedStats.add((short) 0);
		}

		int index = 0;
		for (SyncReplayResult syncRepRes : repResult) {
			if (filter[index++] && syncRepRes.isReliable()) {
				// iterate through an alignment
				List<Object> nodeInstances = syncRepRes.getNodeInstance();
				List<StepTypes> stepTypes = syncRepRes.getStepTypes();
				Iterator<Object> ni = nodeInstances.iterator();

				// information accumulator
				short traceSize = (short) syncRepRes.getTraceIndex().size();
				boolean isPerfectFitTrace = true;

				// index sequence frequency for this sync rep res: move on model, sync move, ev log move
				int numCurrMarking = (encodedStats.size() - (transStatBlockSize * transArray.length))
						/ markingStatBlockSize;

				// capacity for storing freq move on model, move on log, and marking index+deviating event class 
				int preCapacity = (2 * transArray.length) + (numCurrMarking * ecArray.length);
				TShortArrayList freqAccTrace = new TShortArrayList();
				for (int i = 0; i < preCapacity; i++) {
					freqAccTrace.add((short) 0);
				}

				// keep on track on the marking for constructing event class transitions
				short[] m = getEncodedMarking(mNewNet);
				forLoop: for (StepTypes type : stepTypes) {
					switch (type) {
						case L :
							// find index of the current marking
							int mSeqNumber = getMarkingEncodedIndex(m);
							if (mSeqNumber < 0) {
								// add new marking index in the global stat
								mSeqNumber = addNewMarking(m);
							}

							addMoveOnLogDev(mSeqNumber, mapEc2Int.get(ni.next()), freqAccTrace, traceSize);

							isPerfectFitTrace = false;
							sumAllMoveLogOnly += traceSize;
							break;
						case LMGOOD :
							TransitionP trans = (TransitionP) mapOrig2ViewNode.get(ni.next());

							// find the event class transition index and increment it
							int indexTrans = mapTrans2Int.get(trans);
							freqAccTrace.set(indexTrans, (short) (freqAccTrace.get(indexTrans) + traceSize));

							m = fireTransition(trans, newNet, m, mapPlace2Int);
							sumAllMoveLogModel += traceSize;
							break;
						case MINVI :
						case MREAL :
							// invi is the same as real
							TransitionP transI = (TransitionP) mapOrig2ViewNode.get(ni.next());

							// find the event class transition index and increment it
							int indexI = mapTrans2Int.get(transI) + transArray.length;
							freqAccTrace.set(indexI, (short) (freqAccTrace.get(indexI) + traceSize));
							m = fireTransition(transI, newNet, m, mapPlace2Int);

							if (type.equals(StepTypes.MREAL)) {
								isPerfectFitTrace = false;
							}
							sumAllMoveModelOnly += traceSize;
							break;
						case LMNOGOOD :
							// unable to project this case, stop
							isPerfectFitTrace = false;
							break forLoop;
					}
				}

				// add num of traces
				this.totalTrace += traceSize;
				
				// NOTE: there can be premature stop if lmnogood is executed

				// update sync and move on model
				for (int i = 0; i < transArray.length; i++) {
					int idxGlobal = transStatBlockSize * i;
					if (freqAccTrace.get(i + transArray.length) > 0) { // move on model > 0
						// total frequency move on model
						encodedStats.set(idxGlobal,
								(short) (freqAccTrace.get(i + transArray.length) + encodedStats.get(idxGlobal)));

						// total unique trace where it happens
						encodedStats.set(idxGlobal + 1, (short) (traceSize + encodedStats.get(idxGlobal + 1)));
					}

					if (freqAccTrace.get(i) > 0) { // move sync > 0
						// total frequency move sync
						encodedStats
								.set(idxGlobal + 2, (short) (freqAccTrace.get(i) + encodedStats.get(idxGlobal + 2)));

						// total unique trace where it happens
						encodedStats.set(idxGlobal + 3, (short) (traceSize + encodedStats.get(idxGlobal + 3)));

						if (isPerfectFitTrace) {
							// total unique trace where it happens
							encodedStats.set(idxGlobal + 4, (short) (traceSize + encodedStats.get(idxGlobal + 4)));
						}
					}
				}

				// update move on log
				int tracePointerIdx = 2 * transArray.length;
				int globalPointerIdx = transStatBlockSize * transArray.length;
				while (tracePointerIdx < freqAccTrace.size()) {
					for (int i = 0; i < ecArray.length; i++) {
						if (freqAccTrace.get(tracePointerIdx + i) > 0) {
							int updatedIndex = globalPointerIdx + placeArray.length + i;
							encodedStats.set(updatedIndex,
									(short) (encodedStats.get(updatedIndex) + freqAccTrace.get(tracePointerIdx + i)));
							encodedStats.set(updatedIndex + ecArray.length,
									(short) (encodedStats.get(updatedIndex + ecArray.length) + traceSize));
						}
					}
					globalPointerIdx += markingStatBlockSize;
					tracePointerIdx += ecArray.length;
				}
			} // end of reliable sequence
		}
	}

	/*
	 * private void addNewMarkingDeviations(short[] m, XEventClass evClass,
	 * TShortArrayList freqAccTrace, int traceSize) { // get last marking int
	 * markingIndex = (encodedStats.size() - (transStatBlockSize *
	 * transArray.length)) / markingStatBlockSize;
	 * 
	 * // copy marking first for (short i : m){ this.encodedStats.add(i); } //
	 * then, copy to index freqAccTrace.add((short)markingIndex); for (int i=0;
	 * i < ecArray.length; i++){ if (ecArray[i].equals(evClass)){
	 * freqAccTrace.add((short)traceSize); } else { freqAccTrace.add((short) 0);
	 * } } }
	 */

	private void addMoveOnLogDev(int mSeqNumber, int evClassIdx, TShortArrayList freqAccTrace, int traceSize) {
		int pointerTrace = (transArray.length * 2) + (mSeqNumber * ecArray.length);
		if (pointerTrace >= freqAccTrace.size()) {
			freqAccTrace.fill(pointerTrace, pointerTrace + ecArray.length, (short) 0);
			freqAccTrace.set(pointerTrace + evClassIdx, (short) traceSize);
		} else {
			// add the existing
			freqAccTrace.set(pointerTrace + evClassIdx,
					(short) (freqAccTrace.get(pointerTrace + evClassIdx) + traceSize));
		}
	}

	/**
	 * add a new marking in the encoded stats return the index of the marking
	 * (the sequence index of all stored marking)
	 */
	private int addNewMarking(short[] m) {
		// get last marking index
		int lastMarkingIndex = (encodedStats.size() - (transStatBlockSize * transArray.length)) / markingStatBlockSize;
		for (int i = 0; i < m.length; i++) {
			encodedStats.add(m[i]);
		}
		encodedStats.fill(encodedStats.size(), encodedStats.size() + (2 * ecArray.length), (short) 0);
		return lastMarkingIndex;
	}

	/**
	 * get marking sequence number in the main array. If not found, return
	 * negative value
	 * 
	 * @param m
	 * @return
	 */
	private int getMarkingEncodedIndex(short[] m) {
		int pointer = (transStatBlockSize * transArray.length);
		int limit = pointer + placeArray.length;
		while (pointer < encodedStats.size()) {
			whileLoop: while (pointer < limit) {
				// check if the marking is the same
				for (int i = 0; i < placeArray.length; i++) {
					if (m[i] != encodedStats.get(i + pointer)) {
						break whileLoop;
					}
				}
				// marking is found
				return ((pointer - (transStatBlockSize * transArray.length)) / markingStatBlockSize);
			}
			// not correct marking
			pointer += markingStatBlockSize;
			limit = pointer + placeArray.length;
		}
		return -1; // marking is not found
	}

	/**
	 * return marking in encoded form
	 * 
	 * @param mNewNet
	 * @return
	 */
	private short[] getEncodedMarking(Marking mNewNet) {
		short[] res = new short[placeArray.length];
		for (int i = 0; i < placeArray.length; i++) {
			res[i] = mNewNet.occurrences(placeArray[i]).shortValue();
		}
		return res;
	}

	/**
	 * Assuming Transition is fireable, no need to check anymore
	 * 
	 * @param trans
	 * @param newNet
	 * @param m
	 * @param mapPlace2Int
	 */
	private short[] fireTransition(TransitionP trans, PetrinetGraphP newNet, short[] m,
			Map<PlaceP, Integer> mapPlace2Int) {
		Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> edges = newNet.getInEdges(trans);
		for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : edges) {
			if (edge instanceof ResetArc) {
				m[mapPlace2Int.get(edge.getSource())] = 0;
			} else if (!(edge instanceof InhibitorArc)) {
				// ordinary edges
				int removedTokens = newNet.getArc(edge.getSource(), edge.getTarget()).getWeight();
				if (m[mapPlace2Int.get(edge.getSource())] < removedTokens) {
					m[mapPlace2Int.get(edge.getSource())] = 0;
				} else {
					m[mapPlace2Int.get(edge.getSource())] -= removedTokens;
				}
			}
		}

		edges = newNet.getOutEdges(trans);
		for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : edges) {
			m[mapPlace2Int.get(edge.getTarget())] += newNet.getArc(edge.getSource(), edge.getTarget()).getWeight();
		}
		return m;
	}

	public int[] getInfoNode(int selectedIndex) {
		int[] info = null;
		if (selectedIndex < transArray.length) {
			// refer to the long array for exact statistics on transitions
			int pointer = selectedIndex * transStatBlockSize;
			info = new int[5];
			info[0] = encodedStats.get(pointer + 2);
			info[1] = encodedStats.get(pointer + 4);
			info[2] = encodedStats.get(pointer + 3);
			info[3] = encodedStats.get(pointer);
			info[4] = encodedStats.get(pointer + 1);

		} else if (selectedIndex < (transArray.length + placeArray.length)){
			// return marking index that contains this particular place
			Set<Integer> mIndexWPlaces = new HashSet<Integer>();
			int pointer = (transArray.length * transStatBlockSize) + (selectedIndex - transArray.length);
			int markID = 0;
			while (pointer < encodedStats.size()) {
				if (encodedStats.get(pointer) > 0) {
					mIndexWPlaces.add(markID);
				}
				pointer += markingStatBlockSize;
				markID++;
			}
			info = new int[mIndexWPlaces.size()];
			int i = 0;
			for (Integer index : mIndexWPlaces) {
				info[i] = index;
				i++;
			}
		}
		return info;
	}

	/**
	 * Get marking info: [marking][freq event class][freq unique trace event class]
	 * @param markingIndex
	 * @return
	 */
	public short[] getInfoMarking(int markingIndex) {
		int startMarkingIndex = (transArray.length * transStatBlockSize) + (markingIndex * markingStatBlockSize);
		if (startMarkingIndex > encodedStats.size()) {
			return null;
		} else {
			return encodedStats.toArray(startMarkingIndex, placeArray.length + (2 * ecArray.length));
		}
	}

	public int[] getAllStats() {
		return new int[] { sumAllMoveLogModel, sumAllMoveModelOnly, sumAllMoveLogOnly, totalTrace };
	}

	public TransitionP[] getTransArray() {
		return this.transArray;
	}

	/**
	 * Return negative if it does not exist
	 * 
	 * @param t
	 * @return
	 */
	public int getIndexOf(TransitionP t) {
		int i = 0;
		while ((!t.equals(transArray[i])) && (i < transArray.length)) {
			i++;
		}
		return i < transArray.length ? i : -1;
	}

	/**
	 * Return minimum and maximum values of frequency
	 * 
	 * @param isShowMoveSyncModel
	 * @param isShowMoveModelOnly
	 * @return
	 */
	public int[] getMinMaxFreq(boolean isShowMoveSyncModel, boolean isShowMoveModelOnly) {
		if (!isShowMoveSyncModel && !isShowMoveModelOnly) {
			return new int[] { 0, 0 };
		} else {
			int counter = 0;
			int min = Integer.MAX_VALUE;
			int max = Integer.MIN_VALUE;

			int logIndex = transStatBlockSize * transArray.length;

			while (counter < logIndex) {
				if ((isShowMoveModelOnly)&&(!isShowMoveSyncModel)) {
					if (encodedStats.get(counter) < min) {
						min = encodedStats.get(counter);
					}
					if (encodedStats.get(counter) > max) {
						max = encodedStats.get(counter);
					}
				} else 
				if ((isShowMoveSyncModel)&&(!isShowMoveModelOnly)) {
					if (encodedStats.get(counter + 2) < min) {
						min = encodedStats.get(counter + 2);
					}

					if (encodedStats.get(counter + 2) > max) {
						max = encodedStats.get(counter + 2);
					}
				} else {
					assert(isShowMoveSyncModel && isShowMoveModelOnly);
					// show move on log and show move on model
					if (encodedStats.get(counter) + encodedStats.get(counter + 2) < min) {
						min = encodedStats.get(counter) + encodedStats.get(counter + 2);
					}

					if (encodedStats.get(counter + 2) + encodedStats.get(counter + 2) > max) {
						max = encodedStats.get(counter) + encodedStats.get(counter + 2);
					}
				}
				counter += transStatBlockSize;
			}

			return new int[] { min, max };
		}
	}

	public int getNumPlaces() {
		return placeArray.length;
	}

	public PlaceP[] getPlaceArray() {
		return placeArray;
	}

	public XEventClass[] getEvClassArray() {
		return ecArray;
	}

	/**
	 * get frequency occurrence of places from markings
	 * @return
	 */
	public short[] getPlaceFreq() {
		short[] res = new short[placeArray.length];
		Arrays.fill(res, (short) 0);
		
		// iterate all markings
		int pointerMarking = transStatBlockSize * transArray.length;
		while (pointerMarking < encodedStats.size()){
			for (int i=0; i < placeArray.length; i++){
				// if the marking contains this place
				if (encodedStats.get(pointerMarking + i) > 0){
					// sum up all move on log that occurred before
					int limit = pointerMarking + placeArray.length + ecArray.length;
					for (int j=pointerMarking + placeArray.length; j < limit; j++){
						res[i] += encodedStats.get(j);
					}
				}
			}
			pointerMarking += markingStatBlockSize;
		}
		
		return res;
	}

	public int getPlaceIndexOf(PlaceP p) {
		int i = 0;
		while ((!p.equals(placeArray[i])) && (i < placeArray.length)) {
			i++;
		}
		return i < placeArray.length ? i : -1;
	}
}
