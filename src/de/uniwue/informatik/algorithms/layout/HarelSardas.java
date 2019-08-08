package de.uniwue.informatik.algorithms.layout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.javatuples.Pair;
import org.javatuples.Triplet;

import com.google.common.base.Function;

import de.uniwue.informatik.algorithms.layout.VData.VType;
import edu.uci.ics.jung.algorithms.util.IterativeContext;
import de.uniwue.informatik.graph.embedding.EmbeddedUndirectedGraph;

/**
 * 
 * Implementation of "An Algorithm for Straight-Line Drawing of Planar Graphs" by Harel and Sardas (1995).
 * 
 * This class primary contains the placement step (which is almost the same in the earlier paper by
 * Fraysseix, de, Pach and Pollack and later in one by Chrobak and Payne).
 * The first step (canonical order) is handled in the class {@link BiconnectedCanonicalOrderer}.
 * 
 * TODO The algorithm can be implemented in linear time but here it is implemented in a simplified way and is slower.
 * No traversal of the tree at the end but immediately repositioning each vertex in the tree under the current vertex.
 * That can be changed later to achieve better running times.
 * 
 * @author Johannes
 *
 * @param <V>
 * @param <E>
 */
public class HarelSardas<V, E> extends AbstractGridLayout<V, E> implements IterativeContext {
	
	protected int iteration = 0;
	
	protected ArrayList<V> biconnectedCanonicalOrdering;
	protected Map<V, Integer> biconnectedCanonicalOrderingIndices;
	
	protected LinkedList<V> currentContour;
	
	protected EmbeddedUndirectedGraph<V, E> graph;
	
	/**
	 * Data structure filled via execution of {@link BiconnectedCanonicalOrderer#apply(EmbeddedUndirectedGraph)}
	 * if {@link BiconnectedCanonicalOrderer#registerCollectionOfRemovedCrossingEdges(Collection, Map)} was called,
	 * thus if in one of the constructors of that class an
	 * Collection<edu.uci.ics.jung.graph.util.Pair<edu.uci.ics.jung.graph.util.Pair<V>>> removedEdges was passed.
	 * 
	 * This class variable was created to see which edges were split.
	 * E.g. if some of them were only dummy edges the replacement must become dummy objects then and may be removed later.
	 */
	public Map<E, Triplet<E, V, E>> originalEdgesReplacedByASplitEdge;
	
	/**
	 * This is the set of vertices assigned to each vertex.
	 * Same naming as in the paper. See there for more information
	 */
	protected Map<V, Set<V>> L;
	
	public HarelSardas(EmbeddedUndirectedGraph<V, E> graph, Function<V, GridPoint> initializer, 
			Collection<edu.uci.ics.jung.graph.util.Pair<edu.uci.ics.jung.graph.util.Pair<V>>> removedEdges) {
		super(graph, initializer);
		this.graph = graph;
		initialize(removedEdges);
	}
	
	public HarelSardas(EmbeddedUndirectedGraph<V, E> graph) {
		this(graph, null);
	}

	public HarelSardas(EmbeddedUndirectedGraph<V, E> graph, 
			Collection<edu.uci.ics.jung.graph.util.Pair<edu.uci.ics.jung.graph.util.Pair<V>>> removedEdges) {
		super(graph);
		this.graph = graph;
		initialize(removedEdges);
	}

	@Override
	public void initialize() {
		initialize(null);
	}
	
	public void initialize(Collection<edu.uci.ics.jung.graph.util.Pair<edu.uci.ics.jung.graph.util.Pair<V>>> removedEdges) {
		reset();
		originalEdgesReplacedByASplitEdge = new LinkedHashMap<>();
		biconnectedCanonicalOrdering = new BiconnectedCanonicalOrderer<V, E>()
				.registerCollectionOfRemovedCrossingEdges(removedEdges, originalEdgesReplacedByASplitEdge).apply(this.graph);
		biconnectedCanonicalOrderingIndices = new HashMap<>(biconnectedCanonicalOrdering.size());
		for (int i = 0; i < biconnectedCanonicalOrdering.size(); ++i) {
			biconnectedCanonicalOrderingIndices.put(biconnectedCanonicalOrdering.get(i), i);
		}
		currentContour = new LinkedList<>();
		L = new HashMap<>();
		resetVisibility();
	}

	@Override
	public void reset() {
		iteration = 0;
		biconnectedCanonicalOrdering = null;
		biconnectedCanonicalOrderingIndices = null;
		currentContour = null;
		originalEdgesReplacedByASplitEdge = null;
		L = null;
		
		resetVisibility();
	}
	
	protected void resetVisibility() {
		for (V vertex : graph.getVertices()) {
			visibilityVertices.put(vertex, false);
		}
		for (E edge : graph.getEdges()) {
			visibilityEdges.put(edge, false);
		}
	}

	@Override
	public void step() {
		int k = iteration;
		if (iteration == 0) {
			setLocation(v(0), 0, 0);
			L.put(v(0), new LinkedHashSet<>(Collections.singletonList(v(0))));
			currentContour.add(v(0));
		}
		else if (iteration == 1) {
			setLocation(v(1), 2, 0);
			L.put(v(1), new LinkedHashSet<>(Collections.singletonList(v(1))));
			currentContour.add(v(1));
		}
		else if (iteration == 2) {
			setLocation(v(2), 1, 1);
			L.put(v(2), new LinkedHashSet<>(Collections.singletonList(v(2))));
			currentContour.add(1, v(2));
		}
		else if (iteration < biconnectedCanonicalOrdering.size()) {
			V v_k = v(k);
			LinkedList<V> neighborsOfVInPrevCanonicalOrder = getNeighborsOfVInPrevCanonicalOrderFromLeftToRight(v_k);
			HashSet<V> verticesOfV_kInL = new LinkedHashSet<>();
			boolean firstNeighborReached = false;
			boolean lastNeighborReached = false;
			int insertionIndex = 0;
			V w_p = neighborsOfVInPrevCanonicalOrder.getFirst(); //leftmost neighbor of v_k in G_{k-1}
			V w_q = neighborsOfVInPrevCanonicalOrder.getLast(); //rightmost neighbor of v_k in G_{k-1}
			if (neighborsOfVInPrevCanonicalOrder.size() == 1) {
				//special case: v_k has support
				V onlyNeighbor = neighborsOfVInPrevCanonicalOrder.getFirst();
				int indexOfVAtOnlyNeighbor = -1;
				for (int i = 0; i < graph.getNeighborsInEmbeddingOrder(onlyNeighbor).size(); ++i) {
					if (graph.getNeighborsInEmbeddingOrder(onlyNeighbor).get(i).getValue0().equals(v_k)) {
						indexOfVAtOnlyNeighbor = i;
					}
				}
				int neighborOfNeighborCount = graph.getNeighborsInEmbeddingOrder(onlyNeighbor).size();
				int indexOfOnlyNeighborAtContour = currentContour.indexOf(onlyNeighbor);
				if (indexOfOnlyNeighborAtContour != 0 &&
						biconnectedCanonicalOrderingIndices.get(graph.getNeighborsInEmbeddingOrder(onlyNeighbor).
						get((indexOfVAtOnlyNeighbor + 1) % neighborOfNeighborCount).getValue0()) < k) {
					//has left support
					w_p = currentContour.get(indexOfOnlyNeighborAtContour - 1);
					w_q = onlyNeighbor;
				}
				else {
					//has right support
					w_p = onlyNeighbor;
					w_q = currentContour.get(indexOfOnlyNeighborAtContour + 1);
				}
			}
			//go through all vertices of the contour and handle them accordingly in three phases (controlled by 2 boolean vars)
			for (Iterator<V> iter = currentContour.iterator(); iter.hasNext();) {
				V contourV = iter.next();
				
				if (contourV.equals(w_q)) {
					lastNeighborReached = true;
				}
				
				if (lastNeighborReached) {
					//vertices that must be moved by two
					for (V rightV : L.get(contourV)) {
						this.setLocation(rightV, this.apply(rightV).getX() + 2, this.apply(rightV).getY());
					}
				}
				else if (firstNeighborReached) {
					//vertices that must be moved by one
					for (V midV : L.get(contourV)) {
						this.setLocation(midV, this.apply(midV).getX() + 1, this.apply(midV).getY());
					}
					verticesOfV_kInL.addAll(L.get(contourV));
					iter.remove(); //this inner vertex is no more part of the contour -> remove it
				}
				else {
					++insertionIndex;
				}
				
				if (contourV.equals(w_p)) {
					firstNeighborReached = true;
				}
			}
			//position v_k
			int x1 = this.apply(w_p).getX();
			int y1 = this.apply(w_p).getY();
			int x2 = this.apply(w_q).getX();
			int y2 = this.apply(w_q).getY();
			this.setLocation(v_k, (x1 - y1 + x2 + y2) / 2, (-x1 + y1 + x2 + y2) / 2);
			//update structures
			currentContour.add(insertionIndex, v_k);
			verticesOfV_kInL.add(v_k); 
			L.put(v_k, verticesOfV_kInL);
		}
		//update visibility
		this.visibilityVertices.put(v(k), true);
		for (V neighbor : graph.getNeighbors(v(k))) {
			//make edges to previously drawn edges visible
			if (biconnectedCanonicalOrderingIndices.get(neighbor) < k) {
				visibilityEdges.put(graph.findEdge(v(k), neighbor), true);
			}
		}
		++iteration;
	}

	@Override
	public boolean done() {
		if (iteration >= biconnectedCanonicalOrdering.size()) {
			return true;
		}
		return false;
	}
	
	private V v(int indexInBiconnectedCanonicalOrder) {
		return biconnectedCanonicalOrdering.get(indexInBiconnectedCanonicalOrder);
	}
	
	public int index(V v) {
		return biconnectedCanonicalOrdering.indexOf(v);
	}
	
	/**
	 * 
	 * @param v
	 * @return
	 */
	private LinkedList<V> getNeighborsOfVInPrevCanonicalOrderFromLeftToRight(V v) {
		int indexOfV = biconnectedCanonicalOrderingIndices.get(v);
		LinkedList<V> neighborsOfVInPrevCanonicalOrder = new LinkedList<>();
		for (Pair<V, E> neighborOfVTuple : graph.getNeighborsInEmbeddingOrder(v)) {
			V neighborOfV = neighborOfVTuple.getValue0();
			if (biconnectedCanonicalOrderingIndices.get(neighborOfV) < indexOfV) {
				neighborsOfVInPrevCanonicalOrder.add(neighborOfV);
			}
		}
		
		Collections.sort(neighborsOfVInPrevCanonicalOrder, new Comparator<V>() {
			@Override
			public int compare(V o1, V o2) {
				return Integer.compare(currentContour.indexOf(o1), currentContour.indexOf(o2));
			}
		});
		
		//check for correctness (can be removed or commented out)
		V prevNeighbor = null;
		for (V neighbor : neighborsOfVInPrevCanonicalOrder) {
			assert prevNeighbor == null || this.apply(prevNeighbor).getX() < this.apply(neighbor).getX() :
					"unexpected order of neighbor vertices of "+v+", Vertex "+prevNeighbor+" unexpectedly came after "+neighbor;
			prevNeighbor = neighbor;
		}
		return neighborsOfVInPrevCanonicalOrder;
	}
	
	
	/**
	 * Does only work if this of type <VData<String>, String>
	 * 
	 * @param removedEdges
	 */
	public void reinsertCrossingEdgesInNICplanarCase(
			Collection<edu.uci.ics.jung.graph.util.Pair<edu.uci.ics.jung.graph.util.Pair<V>>> removedEdges) {
		
		//remove all shiftVertices
		for (V v : new ArrayList<>(graph.getVertices())) {
			if (((VData<String>) v).getV() != null && ((VData<String>) v).getV().startsWith("shiftVertex")) {
				graph.removeVertex(v);
			}
		}
		
		//insert all crossing edges
		for (edu.uci.ics.jung.graph.util.Pair<edu.uci.ics.jung.graph.util.Pair<V>> quadrangle : removedEdges) {
			//assign vertices
			V vA, vB, vC, vD;
			if (graph.isNeighbor(quadrangle.getFirst().getFirst(), quadrangle.getFirst().getSecond())) {
				vA = index(quadrangle.getFirst().getFirst()) < index(quadrangle.getFirst().getSecond()) ? 
						quadrangle.getFirst().getFirst() : quadrangle.getFirst().getSecond();
				vC = vA == quadrangle.getFirst().getFirst() ? quadrangle.getFirst().getSecond() : quadrangle.getFirst().getFirst();
			}
			else {
				vA = index(quadrangle.getSecond().getFirst()) < index(quadrangle.getSecond().getSecond()) ? 
						quadrangle.getSecond().getFirst() : quadrangle.getSecond().getSecond();
				vC = vA == quadrangle.getSecond().getFirst() ? quadrangle.getSecond().getSecond() : quadrangle.getSecond().getFirst();
			}
			int indexOfAAtC = graph.getVertexIndexInEmbeddingList(vA, vC);
			vD = graph.getNeighborsInEmbeddingOrder(vC).get((indexOfAAtC - 1 + graph.degree(vC)) % graph.degree(vC)).getValue0();
			vB = graph.getNeighborsInEmbeddingOrder(vC).get((indexOfAAtC + 1) % graph.degree(vC)).getValue0();
						
			//remove dummy edge, insert bend points and the crossing point. Edges between them follow in the following case distinction
			V vLower = apply(vB).getY() < apply(vD).getY() ? vB : vD;
			boolean vCCoversVLower = BiconnectedCanonicalOrderer.isDirectlyCoveredBy(vLower, vC, graph, biconnectedCanonicalOrdering);
			graph.removeEdge(graph.findEdge(vA, vC));
			V bendAC = (V) new VData<String>(VType.BEND_POINT);
			V bendBD = (V) new VData<String>(VType.BEND_POINT);
			V crossingPoint = (V) new VData<String>(VType.CROSSING_POINT);
			graph.addVertex(bendAC);
			graph.addVertex(bendBD);
			graph.addVertex(crossingPoint);
			int embeddingOrderIndexA = graph.isNeighbor(vA, vB) ? graph.getVertexIndexInEmbeddingList(vB, vA) + 1 
					: graph.getVertexIndexInEmbeddingList(vD, vA);
			int embeddingOrderIndexB = graph.getVertexIndexInEmbeddingList(vC, vB) + 1;
			int embeddingOrderIndexC = graph.getVertexIndexInEmbeddingList(vB, vC);
			int embeddingOrderIndexD = graph.getVertexIndexInEmbeddingList(vC, vD);
			String eAC = "e_"+vA+"_"+vC;
			String eBD = "e_"+vB+"_"+vD;
			
			//find positions for bend points and crossing points and insert edges
			if (index(vC) > index(vB) && index(vC) > index(vD)) {
				//Case 1
				setLocation(crossingPoint, getX(vA), getY(vLower));
				setLocation(bendAC, getX(vA), getY(vLower) + 1);
				setLocation(bendBD, vLower == vB ? getX(vA) - 1 : getX(vA) + 1, getY(vLower));
				graph.addEdge((E) ("firstPartOf_"+eAC), vA, embeddingOrderIndexA, crossingPoint, 0);
				graph.addEdge((E) ("secondPartOf_"+eAC), crossingPoint, 1, bendAC, 0);
				graph.addEdge((E) ("thirdPartOf_"+eAC), bendAC, 1, vC, embeddingOrderIndexC);
			}
			else {
				//Case 2 or 3
				if (vCCoversVLower) {
					//Case 2
					int xCross = vLower == vB ? (getX(vC) - getY(vC) + getX(vLower) + getY(vLower)) / 2 
							: (getX(vC) + getY(vC) + getX(vLower) - getY(vLower)) / 2;
					int yCross = vLower == vB ? (-getX(vC) + getY(vC) + getX(vLower) + getY(vLower)) / 2 
							: (getX(vC) + getY(vC) - getX(vLower) + getY(vLower)) / 2;
					setLocation(crossingPoint, xCross, yCross);
					setLocation(bendAC, vLower == vB ? xCross - 1 : xCross + 1, yCross - 1);
					setLocation(bendBD, vLower == vB ? xCross - 1 : xCross + 1, yCross + 1);
				}
				else {
					//Case 3
					setLocation(crossingPoint, getX(vC), getY(vLower));
					setLocation(bendAC, getX(vC), getY(vLower) - 1);
					setLocation(bendBD, vLower == vB ? getX(vC) - (getY(vC) - getY(vB)) : getX(vC) + (getY(vC) - getY(vD)), getY(vLower));
				}
				graph.addEdge((E) ("firstPartOf_"+eAC), vA, embeddingOrderIndexA, bendAC, 0);
				graph.addEdge((E) ("secondPartOf_"+eAC), bendAC, 1, crossingPoint, 0);
				graph.addEdge((E) ("thirdPartOf_"+eAC), crossingPoint, 1, vC, embeddingOrderIndexC);
			}
			//in all cases
			if (vLower == vB) {
				graph.addEdge((E) ("firstPartOf_"+eBD), vB, embeddingOrderIndexB, crossingPoint, 1);
				graph.addEdge((E) ("secondPartOf_"+eBD), crossingPoint, 3, bendBD, 0);
				graph.addEdge((E) ("thirdPartOf_"+eBD), bendBD, 1, vD, embeddingOrderIndexD);
			}
			else {
				graph.addEdge((E) ("firstPartOf_"+eBD), vB, embeddingOrderIndexB, bendBD, 0);
				graph.addEdge((E) ("secondPartOf_"+eBD), bendBD, 1, crossingPoint, 1);
				graph.addEdge((E) ("thirdPartOf_"+eBD), crossingPoint, 3, vD, embeddingOrderIndexD);
			}
			//in Case 3 we additionally have to remove the dummy edge (a, v_lower)
			if (!(index(vC) > index(vB) && index(vC) > index(vD)) && !vCCoversVLower) {
				graph.removeEdge(graph.findEdge(vA, vLower));
			}
		}
	}
}
