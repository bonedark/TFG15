package org.processmining.plugins.astar.algorithm;

import org.processmining.plugins.astar.interfaces.Head;
import org.processmining.plugins.astar.interfaces.Record;
import org.processmining.plugins.astar.interfaces.Tail;

public interface AStarThread<H extends Head, T extends Tail> {

	public static interface Canceller {
		public boolean isCancelled();
	}

	public Record run(Canceller c) throws Exception;

	public Record run(Canceller c, double stopAt) throws Exception;

	public int getQueuedStateCount();

	public int getTraversedArcCount();

}