package de.uniwue.informatik.graph.embedding;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import org.javatuples.Triplet;

import edu.uci.ics.jung.graph.util.Pair;

public class Face<V, E> extends ArrayList<Triplet<E, EdgeSide, Pair<V>>> {

	public Face() {
		super();
	}
	
	/**
     * Copy-Constructor
     * <p>
     * Generic V- and E-objects are the same but the inherited list and the {@link Triplet}s and the {@link Pair}s are new
     * 
     * @param copyThat
     */
	public Face(Face<V, E> copyThat) {
		super();
		for (Triplet<E, EdgeSide, Pair<V>> element : copyThat) {
			this.add(new Triplet<E, EdgeSide, Pair<V>>(element.getValue0(), element.getValue1(), 
					new Pair<>(element.getValue2().getFirst(), element.getValue2().getSecond())));
		}
	}
	
	/**
	 * @return
	 * 		List of all vertices in that face in correct order
	 */
	public ArrayList<V> getAllVertices () {
		ArrayList<V> allVertices = new ArrayList<>(this.size());
		for (Triplet<E, EdgeSide, Pair<V>> e : this) {
			if (e.getValue1() == EdgeSide.LEFT) {
				allVertices.add(e.getValue2().getFirst());
			}
			else {
				allVertices.add(e.getValue2().getSecond());
			}
		}
		return allVertices;
	}
	
	public boolean addBefore(Triplet<E, EdgeSide, Pair<V>> toBeInserted, Triplet<E, EdgeSide, Pair<V>> beforeThat) {
		int insertionIndex = getIndex(beforeThat);
		if (insertionIndex < 0) {
			return false;
		}
		add(insertionIndex, toBeInserted);
		return true;
	}
	
	public boolean addBehind(Triplet<E, EdgeSide, Pair<V>> toBeInserted, Triplet<E, EdgeSide, Pair<V>> behindThat) {
		int insertionIndex = getIndex(behindThat) + 1;
		if (insertionIndex <= 0) {
			return false;
		}
		add(insertionIndex, toBeInserted);
		return true;
	}
	
	public boolean removeEdge(E edge, EdgeSide edgeSide) {
		for (Triplet<E, EdgeSide, Pair<V>> eData : this) {
			if (edge.equals(eData.getValue0()) && eData.getValue1() == edgeSide) {
				return this.remove(eData);
			}
		}
		return false;
	}
	
	public boolean removeEdgeAllOccurences(E edge) {
		return this.removeEdge(edge, EdgeSide.LEFT) || this.removeEdge(edge, EdgeSide.RIGHT);
	}
	
	/**
	 * Returns -1 for not contained
	 * 
	 * @param edgeInclSide
	 * @return
	 */
	public int getIndex(Triplet<E, EdgeSide, Pair<V>> edgeInclSide) {
		int i = 0;
		for(Triplet<E, EdgeSide, Pair<V>> e: this) {
			if (equals(e, edgeInclSide)) {
				return i;
			}
			++i;
		}
		return -1;
	}
	
	@SuppressWarnings("unchecked") //ClassCastException is catched and "false" returned then
	@Override
	public boolean equals(Object o) {
		/*
		 * Must check if same graph and the same elements in the same order - maybe shifted.
		 * Every of the .size() elements can be the first element if the order (taken as a ring)
		 * of the elements is still the same.
		 */
		if (o == this) {
			return true;
		}
		if (o instanceof Face) {
			try {
				//special case: no elements
				if (this.size() == 0 && ((LinkedList<E>) o).size() == 0) {
					return true;
				}
				//must have same size
				else if (this.size() != ((Face<?, E>) o).size()) {
					return false;
				}
				
				//find same first element
				Triplet<E, EdgeSide, Pair<V>> firstElement = this.get(0);
				Iterator<Triplet<E, EdgeSide, Pair<V>>> oIterator = ((Face<V, E>) o).iterator();
				Triplet<E, EdgeSide, Pair<V>> current = oIterator.next();
				while (oIterator.hasNext() && !equals(current, firstElement)) {
					current = oIterator.next();
				}
				
				if (!equals(current, firstElement)) {
					return false;
				}
				
				//check same elements and same order
				Iterator<Triplet<E, EdgeSide, Pair<V>>> thisIterator = this.iterator();
				while (thisIterator.hasNext()) {
					//if not the same -> fail
					if (!equals(thisIterator.next(), current)) {
						return false;
					}
					if (!oIterator.hasNext()) {
						oIterator = ((Face<V, E>) o).iterator();
					}
					current = oIterator.next();
				}
				//no error found -> o equals this
				return true;
			}
			catch (ClassCastException e) {
				return false;
			}
		}
		return false;
	}
	
	private static <V, E> boolean equals (Triplet<E, EdgeSide, Pair<V>> pair0, Triplet<E, EdgeSide, Pair<V>> pair1) {
		return pair0.getValue0().equals(pair1.getValue0()) && pair0.getValue1().equals(pair1.getValue1())
				&& pair0.getValue2().getFirst().equals(pair1.getValue2().getFirst()) 
				&& pair0.getValue2().getSecond().equals(pair1.getValue2().getSecond());
	}
	
	/**
	 * Edges are checked with their .equals(..)-method.
	 * If a face contains an edge twice (this is possible because both sides of an edge can be in the same face!),
	 * then the index of its first appearance is returned
	 * 
	 * @param edge
	 * @return
	 * 		-1 if not contained
	 */
	public int getIndexOfEdge(E edge) {
		int index = 0;
		for (Triplet<E, EdgeSide, Pair<V>> triplet : this) {
			if (triplet.getValue0().equals(edge)) {
				return index;
			}
			++index;
		}
		return -1;
	}

	/**
	 * Edges are checked with their .equals(..)-method.
	 * 
	 * @param edge
	 * @return
	 */
	public boolean containsEdge(E edge) {
		return getIndexOfEdge(edge) != -1;
	}

	/**
	 * Vertices are checked with their .equals(..)-method.
	 * 
	 * @param vertex
	 * @return
	 */
	public boolean containsVertex(V vertex) {
		for (Triplet<E, EdgeSide, Pair<V>> triplet : this) {
			for (V v : triplet.getValue2()) {
				if (vertex.equals(v)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean containsBothSidesOfThatEdge(E edge) {
		int containmentCount = 0;
		for (Triplet<E, EdgeSide, Pair<V>> triplet : this) {
			if (triplet.getValue0().equals(edge)) {
				++containmentCount;
			}
		}
		assert containmentCount <= 2;
		return containmentCount == 2;
	}

}
