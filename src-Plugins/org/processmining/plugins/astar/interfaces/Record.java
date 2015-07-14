package org.processmining.plugins.astar.interfaces;

import nl.tue.storage.CompressedStore;
import nl.tue.storage.StorageException;

import org.processmining.plugins.astar.algorithm.State;

/**
 * Stores the little bit of information needed per volatile state in the
 * AStarAlgorithm. The memory footprint of this object should be kept to a
 * minimum.
 * 
 * equality should be based purely on the long value of the state and should not
 * include costs. The priorityQueue used in the AStar algorithm will not insert
 * a new Record if it equals an old record and has equal or higher cost
 * (including the estimate)
 * 
 * @author bfvdonge
 * 
 * @param <S>
 */
public interface Record {

	/**
	 * retrieves the state stored at index getState() from the storage.
	 * 
	 * @param storage
	 * @return
	 * @throws StorageException
	 */
	public <H extends Head, T extends Tail> State<H, T> getState(CompressedStore<State<H, T>> storage)
			throws StorageException;

	/**
	 * Returns the index of the state for which this record is kept.
	 * 
	 * @return
	 */
	public long getState();

	/**
	 * returns the cost so far for reaching the corresponding state
	 * 
	 * @return
	 */
	public double getCostSoFar();

	/**
	 * returns an underestimate for the remaining cost
	 * 
	 * @return
	 */
	public double getEstimatedRemainingCost();

	/**
	 * Method should return sum of costSoFar and estimatedCost;
	 */
	public double getTotalCost();

	/**
	 * sets the underestimate of the remaining cost
	 * 
	 * @return
	 */
	public void setEstimatedRemainingCost(double cost);

	/**
	 * returns the predecessor record.
	 * 
	 * @return
	 */
	public Record getPredecessor();

	/**
	 * puts the index of the state corresponding to this record in the record.
	 * 
	 * @param index
	 */
	public void setState(long index);

	/**
	 * creates a new record, based on the operations m and l applied to the old
	 * head.
	 * 
	 * @param modelMove
	 *            the index of the transition that needs to be fired (or
	 *            Move.BOTTOM if none)
	 * @param d
	 *            the delegate
	 * @param trace
	 *            the index of the trace in the log
	 * @param event
	 *            the index of the event in the trace
	 * @return
	 */
	public Record getNextRecord(Delegate<? extends Head, ? extends Tail> d, Head newHead, long state, int modelMove,
			int logMove);

	/**
	 * return the id of the modelmove used to reach this record from previous.If
	 * none, then Move.BOTTOM is returned
	 * 
	 * @return
	 */
	public int getModelMove();

	/**
	 * return the index in the trace representing the event that was moved to
	 * get to this step. If none, then Move.BOTTOM is returned
	 * 
	 * @return
	 */
	public int getMovedEvent();

	/**
	 * return the index in the trace of the next event move that can be made in
	 * the log from this record.
	 * 
	 * @param delegate
	 * @return
	 */
	public int getNextEvent(Delegate<? extends Head, ? extends Tail> delegate);

}
