package org.processmining.plugins.boudewijn.treebasedreplay;

import gnu.trove.set.TShortSet;
import nl.tue.storage.CompressedHashSet;
import nl.tue.storage.Deflater;
import nl.tue.storage.EqualOperation;
import nl.tue.storage.HashOperation;
import nl.tue.storage.Inflater;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.model.XEvent;
import org.processmining.plugins.astar.algorithm.State;
import org.processmining.plugins.astar.interfaces.Delegate;
import org.processmining.plugins.astar.interfaces.Head;
import org.processmining.plugins.astar.interfaces.Record;
import org.processmining.plugins.astar.interfaces.Tail;
import org.processmining.plugins.boudewijn.tree.Node;
import org.processmining.plugins.boudewijn.tree.Node.Type;

public interface TreeDelegate<H extends Head, T extends Tail> extends Delegate<H, T> {

	public abstract Record createInitialRecord(H head);

	public abstract Inflater<H> getHeadInflater();

	public abstract Deflater<H> getHeadDeflater();

	public abstract void setStateSpace(CompressedHashSet<State<H, T>> statespace);

	public abstract XEventClass getClassOf(XEvent e);

	public abstract short getIndexOf(XEventClass c);

	public abstract short numEventClasses();

	public abstract Type getFunctionType(int c);

	public abstract int getCostFor(int node, int activity);

	public abstract TShortSet getActivitiesFor(int node);

	public abstract Node getNode(int m);

	public abstract XEventClass getEventClass(short act);

	public abstract int getIndexOf(Node root);

	public abstract boolean isLeaf(int modelMove);

	public abstract boolean isLoopLeaf(int modelMove);

	public abstract int getLogMoveCost(int i);

	public abstract int getModelMoveCost(int node);

	public abstract int numNodes();

	public abstract HashOperation<State<H, T>> getHeadBasedHashOperation();

	public abstract EqualOperation<State<H, T>> getHeadBasedEqualOperation();

	public abstract int getScaling();

	public abstract String toString(short modelMove, short activity);

}