package org.processmining.plugins.astar.interfaces;

import gnu.trove.list.TIntList;

public interface Head {

	public Head getNextHead(Record rec, Delegate<? extends Head, ? extends Tail> d, int modelMove, int logMove,
			int activity);

	/**
	 * get the synchronous moves that are possible on activity;
	 * 
	 * @param delegate
	 * @param enabled
	 *            TODO
	 * @param activity
	 * @return
	 */
	public TIntList getModelMoves(Record rec, Delegate<? extends Head, ? extends Tail> delegate, TIntList enabled,
			int activity);

	/**
	 * get the move on model only moves that are possible
	 * 
	 * @param delegate
	 * @return
	 */
	public TIntList getModelMoves(Record rec, Delegate<? extends Head, ? extends Tail> delegate);

	/**
	 * checks if this head belongs to a final state;
	 * 
	 * @return
	 */
	public boolean isFinal(Delegate<? extends Head, ? extends Tail> delegate);

}
