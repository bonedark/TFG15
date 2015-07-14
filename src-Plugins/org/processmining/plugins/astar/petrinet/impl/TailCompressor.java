package org.processmining.plugins.astar.petrinet.impl;

import nl.tue.storage.Deflater;
import nl.tue.storage.Inflater;

import org.processmining.plugins.astar.interfaces.Tail;

public interface TailCompressor<T extends Tail> extends Deflater<T>, Inflater<T> {

}
