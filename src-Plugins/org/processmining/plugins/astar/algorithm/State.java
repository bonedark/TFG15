package org.processmining.plugins.astar.algorithm;

import org.processmining.plugins.astar.interfaces.Head;
import org.processmining.plugins.astar.interfaces.Tail;

/**
 * A State can be constructed with or without a tail
 * 
 * Both equals and hashcode are defined on the head only
 * 
 * The tail is not guaranteed to be non-null
 * 
 * @author bfvdonge
 * 
 */
public final class State<H extends Head, T extends Tail> {

	private final H head;
	private final T tail;

	public State(H head, T tail) {
		this.head = head;
		this.tail = tail;
	}

	public State(H head) {
		this.head = head;
		this.tail = null;
	}

	/**
	 * returns the head
	 * 
	 * @return
	 */
	public H getHead() {
		return head;
	}

	/**
	 * returns the tail
	 * 
	 * @return
	 */
	public T getTail() {
		return tail;
	}

	public boolean equals(Object o) {
		return (o != null) && (o instanceof State) && ((State) o).getHead().equals(head);
	}

	public int hashCode() {
		return head.hashCode();
	}
}
