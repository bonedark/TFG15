package org.processmining.plugins.astar.interfaces;

import java.io.IOException;

import nl.tue.storage.CompressedStore;

public interface Tail {

	/**
	 * constructs the new tail based on the two operations from the old head.
	 * Preferably, the code to compute the new tail is kept as lightweight as
	 * possible.
	 * 
	 * @param oldHead
	 * @param m
	 * @param l
	 * @return
	 */
	public Tail getNextTail(Delegate<? extends Head, ? extends Tail> d, Head newHead, int modelMove, int logMove,
			int activity);

	/**
	 * constructs the new tail based on the two operations from the old state,
	 * which is stored in the given store at the given index. Preferably, the
	 * code to compute the new tail is kept as lightweight as possible.
	 * 
	 * @param <S>
	 * @param store
	 * @param index
	 * @param m
	 * @param l
	 * @return
	 */
	public <S> Tail getNextTailFromStorage(Delegate<? extends Head, ? extends Tail> d, CompressedStore<S> store,
			long index, int modelMove, int logMove, int activity) throws IOException;

	/**
	 * get the true estimated costs
	 * 
	 * @param delta
	 *            TODO
	 * 
	 * @return
	 */
	public double getEstimatedCosts(Delegate<? extends Head, ? extends Tail> d, Head head);

	/**
	 * returns true if and only if the replay can finish according to this tail,
	 * i.e. for the case
	 * 
	 * @return
	 */
	public boolean canComplete();

}
