package org.processmining.plugins.astar.interfaces;

import nl.tue.storage.CompressedHashSet;
import nl.tue.storage.Deflater;
import nl.tue.storage.EqualOperation;
import nl.tue.storage.HashOperation;
import nl.tue.storage.Inflater;

import org.processmining.plugins.astar.algorithm.State;

public interface Delegate<H extends Head, T extends Tail> {

	/**
	 * instantiates a record for the given head. Cost is 0
	 * 
	 * @param head
	 * 
	 * @return
	 */
	public Record createInitialRecord(H head);

	/**
	 * creats a tail for the given head. No need to look in storage, just
	 * construct the tail.
	 * 
	 * @param head
	 * @return
	 */
	public T createTail(H head);

	public Inflater<H> getHeadInflater();

	public Deflater<H> getHeadDeflater();

	public Inflater<T> getTailInflater();

	public Deflater<T> getTailDeflater();

	public HashOperation<State<H, T>> getHeadBasedHashOperation();

	public EqualOperation<State<H, T>> getHeadBasedEqualOperation();

	public void setStateSpace(CompressedHashSet<State<H, T>> statespace);

}
