package de.uniwue.informatik.algorithms.layout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;

import org.apache.commons.collections15.map.HashedMap;
import org.javatuples.Triplet;

import com.google.common.base.Function;

import edu.uci.ics.jung.algorithms.cluster.BicomponentClusterer;
import edu.uci.ics.jung.graph.util.Pair;
import de.uniwue.informatik.graph.embedding.EdgeSide;
import de.uniwue.informatik.graph.embedding.EmbeddedUndirectedGraph;
import de.uniwue.informatik.graph.embedding.Face;
import de.uniwue.informatik.main.DrawGraphs;
import de.uniwue.informatik.util.EmbeddedGraphOperations;

/**
 * This class follows "An Algorithm for Straight-Line Drawing of Planar Graphs" by Harel and Sardas (1995).
 * Naming of the Arrays (here realized as maps and not arrays) is like in their paper.
 * 
 * @author Johannes
 *
 * @param <V>
 * @param <E>
 */
public class BiconnectedCanonicalOrderer<V, E> implements Function<EmbeddedUndirectedGraph<V,E>, ArrayList<V>> {
	
	/**
	 * A(f) is the number of edges from the face f that are in G_{k-1}
	 */
	private Map<Face<V, E>, Integer> A = new HashedMap<>();
	/**
	 * N(v) is the number of neighbors of vertex v in G_{k-1}
	 */
	private Map<V, Integer> N = new HashedMap<>();
	/**
	 * F(v) is the number of "ready" faces that have vertex v as their only vertex outside G_{k-1}
	 */
	private Map<V, Integer> F = new HashedMap<>();
	/**
	 * All vertices not added to the biconnected cannonical up to the current iteration.
	 * It starts with all vertices of the graph and is decreased by one at each step.
	 * The number of the current step can be get by queried the number of all vertices of the graph minus the size of that set
	 */
	private HashSet<V> allVerticesNotAddedYet = new LinkedHashSet<V>();
	
	/**
	 * See {@link BiconnectedCanonicalOrderer#registerCollectionOfRemovedCrossingEdges(Collection)}
	 * 
	 * If null it is not used (no insertion of edges and dummy vertices during computation)
	 */
	private Collection<Pair<Pair<V>>> removedEdges = null;
	private Map<V, LinkedList<Pair<Pair<V>>>> vertex2removedEdges;
	private Map<E, Triplet<E, V, E>> originalEdgesReplacedByASplitEdge;
	
	public boolean case1 = false;
	public boolean case2 = false;
	public boolean case3 = false;
	
	
	private ArrayList<V> biconnectedCannonicalOrdering;
	
	public BiconnectedCanonicalOrderer() {
		
	}
	
	private void reset() {
		A.clear();
		N.clear();
		F.clear();
	}
	
	/**
	 * According to the algorithm for RAC crossing in NIC-planar graphs on quadratic area
	 * the computing of the biconnected canonical ordering can be changed a bit:
	 * For each removed pair of crossing edges with a remaining empty quadrangle there can be exactaly one
	 * of those edges be re-inserted (the first that is encountered)
	 * and in a special case one edge of the empty/divided quadrangle is split.
	 * 
	 * That all is done (that special algorithm is applied) iff this method is called before calling 
	 * {@link BiconnectedCanonicalOrderer#apply(EmbeddedUndirectedGraph)}.
	 * 
	 * If you want to make a biconnected canonical ordering without any modification during the computation
	 * simply do not call this method (or pass null as parameter-value).
	 * 
	 * Works only if type of <V, E> is <VData<String>, String> because vertices (bend points) and dummy edges are inserted. 
	 * 
	 * @param removedEdges
	 * 		Existing collection of previously crossed edges (represented by their vertex-endpoints). These edges were removed in the graph.
	 * @param emptyMapForEdgesReplacedByASplitEdge
	 * 		Empty data structure (map) in which the edges are noted that were split during the execution of 
	 * 		{@link BiconnectedCanonicalOrderer#apply(EmbeddedUndirectedGraph)}. The key is the then removed edges and the
	 * 		target value is the edge-vertex-edge objects that are then there instead of that one original edge.
	 * @return this
	 */
	public BiconnectedCanonicalOrderer<V, E> registerCollectionOfRemovedCrossingEdges(
			Collection<Pair<Pair<V>>> removedEdges, Map<E, Triplet<E, V, E>> emptyMapForEdgesReplacedByASplitEdge) {
		if (removedEdges != null) {		
			this.removedEdges = removedEdges;
			this.originalEdgesReplacedByASplitEdge = emptyMapForEdgesReplacedByASplitEdge;
			this.vertex2removedEdges = new LinkedHashMap<>();
			for (Pair<Pair<V>> removedCrossing : removedEdges) {
				for (Pair<V> removedEdge : removedCrossing) {
					for (V vertex : removedEdge) {
						if (!vertex2removedEdges.containsKey(vertex)) {
							vertex2removedEdges.put(vertex, new LinkedList<>());
						}
						vertex2removedEdges.get(vertex).add(removedCrossing);
					}
				}
			}
		}
		return this;
	}
	
	@Override
	public ArrayList<V> apply(EmbeddedUndirectedGraph<V, E> biconnectedGraph) {
		assert new BicomponentClusterer<V, E>().apply(biconnectedGraph).size() == 1
				: "Input graph ("+biconnectedGraph+") is not biconnected. Can not find biconnected canonical ordering.";
		assert biconnectedGraph.getFaces() != null : "Input graph ("+biconnectedGraph+") has an inconsistent or no embedding.";
		
		initialize(biconnectedGraph);
		
		for (int k = 1; k <= biconnectedGraph.getVertexCount(); ++k) {
			V v_k = null;
			if (k < 3) {
				allVerticesNotAddedYet.remove(biconnectedCannonicalOrdering.get(k-1));
				v_k = biconnectedCannonicalOrdering.get(k-1);
			}
			else {
				boolean thereIsOneGoodVertex = false;
				for (V v : allVerticesNotAddedYet) {
					if (N.get(v) >= 2 && N.get(v) == F.get(v) + 1) {
						thereIsOneGoodVertex = true;
						v_k = v;
						break;
					}
				}
				//if there is no "good" next vertex with more than 2 neighbors in G_{k-1} then find a vertex with
				//legal support and N.get(v) == 1
				if (!thereIsOneGoodVertex) {
					for (V v : allVerticesNotAddedYet) {
						if (N.get(v) == 1) {
							for (V neighborOfV : biconnectedGraph.getNeighbors(v)) {
								//find the one vertex that is already in the biconnected canonical order
								if (!allVerticesNotAddedYet.contains(neighborOfV)) {
									//check if v has legal support at neighborOfV
									if ((hasLeftSupport(biconnectedGraph, v, neighborOfV) 
													&& neighborOfV != biconnectedCannonicalOrdering.get(0))
											|| (hasRightSupport(biconnectedGraph, v, neighborOfV) 
													&& neighborOfV != biconnectedCannonicalOrdering.get(1))) {
										//has legal support
										v_k = v;
										break;
									}
								}
							}
							if (v_k != null) {
								break;
							}
						}
					}
				}
				
				//insert new vertex to the biconnected canonical ordering
				assert v_k != null : "Did not find a suitable vertex for the biconnected canonical ordering in step "+k;
				addVertexToBiconnectedCannonicalOrderList(v_k);
				updateNeighbors(biconnectedGraph, v_k);
				updateFaces(biconnectedGraph, v_k);
			}
			
			//special handling for the algorithm drawing NIC-planar graphs RAC onto a quadratic sized grid
			if (removedEdges != null) {
				//may increment k by one if an additional vertex was inserted (when an edge was split)
				k = handleEmptyAndDividedQuadrangles(biconnectedGraph, k, v_k);
			}
		}
		if (case1 && case2 && case3) {
			DrawGraphs.allCasesAppear = true;
		}
		return biconnectedCannonicalOrdering;
	}

	private int handleEmptyAndDividedQuadrangles(EmbeddedUndirectedGraph<V, E> biconnectedGraph, int k, V v_k) {
		if (vertex2removedEdges.containsKey(v_k)) {
			for (Pair<Pair<V>> quadrangle : vertex2removedEdges.get(v_k)) {
				boolean isFirstVertex = true;
				boolean isLastVertex = true;
				V opposite = null;
				Pair<V> neighborsInQuadrangle = null;
				int lowestIndex = k - 1;
				for (Pair<V> edge : quadrangle) {
					for (V v : edge) {
						if (!v.equals(v_k)) {
							if (allVerticesNotAddedYet.contains(v)) {
								isLastVertex = false;
							}
							else{
								isFirstVertex = false;
								lowestIndex = Math.min(lowestIndex, biconnectedCannonicalOrdering.indexOf(v));
							}
							if (edge.contains(v_k)) {
								opposite = v;
							}
							else {
								neighborsInQuadrangle = edge;
							}
						}
					}
				}
				//check three cases
				//1. v_k is the first vertex of that quadrangle in the biconnected canonical ordering
				if (isFirstVertex) {
					//insert dummy edge to make an divided quadrangle out of that empty quadrangle
					Face<V, E> emptyQuadrangleFace = getEmptyQuadrangleFace(biconnectedGraph, quadrangle);
					E dummyEdge = (E) ("dummyE_btw._"+v_k+"_"+opposite);
					biconnectedGraph.addEdge(dummyEdge, 
							v_k, biconnectedGraph.getEdgeIndexInEmbeddingList(emptyQuadrangleFace, v_k), 
							opposite, biconnectedGraph.getEdgeIndexInEmbeddingList(emptyQuadrangleFace, opposite));
					//update data structures
					A.remove(emptyQuadrangleFace);
					A.put(biconnectedGraph.getLeftIncidentFace(dummyEdge), 0);
					A.put(biconnectedGraph.getRightIncidentFace(dummyEdge), 0);
					N.replace(opposite, N.get(opposite) + 1);
				}
				//2. v_k is the last vertex of the quadrangle, the opposite in not the first and the opposite is
				//   in the underset of one of the two neighbors of v_k
				if (isLastVertex && lowestIndex < biconnectedCannonicalOrdering.indexOf(opposite) 
						&& (isDirectlyCoveredBy(opposite, neighborsInQuadrangle.getFirst(), biconnectedGraph, biconnectedCannonicalOrdering) 
						|| isDirectlyCoveredBy(opposite, neighborsInQuadrangle.getSecond(), biconnectedGraph, biconnectedCannonicalOrdering))) {
					System.out.println("Case2"); case2 = true;
					//Add a dummy shift vertex between the two neighbors of it. Inset it
					// in the biconn can ordering directly before v_k
					V leftNeighbor = biconnectedGraph.isDirectPredecessorInEmbeddingList(
							neighborsInQuadrangle.getFirst(), neighborsInQuadrangle.getSecond(), v_k) ?
							neighborsInQuadrangle.getFirst() : neighborsInQuadrangle.getSecond();
					V rightNeighbor = leftNeighbor == neighborsInQuadrangle.getFirst() ?
							neighborsInQuadrangle.getSecond() : neighborsInQuadrangle.getFirst();
					Face<V, E> faceForInsertion = biconnectedGraph.getLeftFace(biconnectedGraph.findEdge(v_k, leftNeighbor), v_k);
					V shiftVertex = (V) new VData<String>("shiftVertexOf_"+leftNeighbor+"+"+rightNeighbor);
					E dummyEdge0 = (E) ("dummyEdge0_"+shiftVertex);
					E dummyEdge1 = (E) ("dummyEdge1_"+shiftVertex);
					biconnectedGraph.addVertex(shiftVertex);
					biconnectedGraph.addEdge(dummyEdge0, shiftVertex, 0, leftNeighbor, 
							biconnectedGraph.getVertexIndexInEmbeddingList(v_k, leftNeighbor));
					biconnectedGraph.addEdge(dummyEdge1, shiftVertex, 0, rightNeighbor, 
							biconnectedGraph.getVertexIndexInEmbeddingList(v_k, rightNeighbor) + 1);
					//update data structures and current canonical ordering (we insert a new vertex!)
					A.remove(faceForInsertion);
					Face<V, E> newTriangleFace = biconnectedGraph.getRightFace(dummyEdge0, leftNeighbor);
					Face<V, E> quadrangleFace = biconnectedGraph.getLeftFace(dummyEdge0, leftNeighbor);
					A.put(newTriangleFace, 3);
					A.put(quadrangleFace,4);
					N.put(shiftVertex, 2);
					//insert new shift vertex in current cannonical order before v_k
					biconnectedCannonicalOrdering.add(biconnectedCannonicalOrdering.indexOf(v_k), shiftVertex); 
					// -> must increment k by 1; this is the only case where k is changed here
					++k;
				}
				//3. v_k is the last vertex of the quadrangle, the opposite is not the first and the opposite is
				//   not in the underset of one of the two neighbors of v_k
				else if (isLastVertex && lowestIndex < biconnectedCannonicalOrdering.indexOf(opposite) 
						&& !isDirectlyCoveredBy(opposite, neighborsInQuadrangle.getFirst(), biconnectedGraph, biconnectedCannonicalOrdering) 
						&& !isDirectlyCoveredBy(opposite, neighborsInQuadrangle.getSecond(), biconnectedGraph, biconnectedCannonicalOrdering)) {
					System.out.println("Case3"); case3 = true;
					//split edge
					V vLowest = biconnectedCannonicalOrdering.indexOf(neighborsInQuadrangle.getFirst()) 
							< biconnectedCannonicalOrdering.indexOf(neighborsInQuadrangle.getSecond()) ?
							neighborsInQuadrangle.getFirst() : neighborsInQuadrangle.getSecond();
					V vSecondHeighest = vLowest == neighborsInQuadrangle.getSecond() ?
							neighborsInQuadrangle.getFirst() : neighborsInQuadrangle.getSecond();
					E edgeToBeSplit = biconnectedGraph.findEdge(vLowest, opposite);
					Face<V, E> leftOriginalFace = biconnectedGraph.getLeftFace(edgeToBeSplit, vLowest);
					Face<V, E> rightOriginalFace = biconnectedGraph.getRightFace(edgeToBeSplit, vLowest);
					int leftOriginalFaceAValue = A.remove(leftOriginalFace);
					int rightOriginalFaceAValue = A.remove(rightOriginalFace);
					V newBendPoint = (V) new VData<String>(VData.VType.BEND_POINT);
					E firstPart = (E) ("firstPartOf_"+edgeToBeSplit);
					E secondPart = (E) ("secondPartOf_"+edgeToBeSplit);
					EmbeddedGraphOperations.splitEdgeViaAVertex(biconnectedGraph, edgeToBeSplit, newBendPoint, 
							firstPart, secondPart);
					originalEdgesReplacedByASplitEdge.put(edgeToBeSplit, new Triplet<>(firstPart, newBendPoint, secondPart));
					//update data structures and current canonical ordering (we insert a new vertex!)
					Face<V, E> leftNewFace = biconnectedGraph.getLeftFace(
							biconnectedGraph.getEndpoints(firstPart).contains(vLowest) ? firstPart : secondPart, vLowest);
					Face<V, E> rightNewFace = biconnectedGraph.getRightFace(
							biconnectedGraph.getEndpoints(firstPart).contains(vLowest) ? firstPart : secondPart, vLowest);
					A.put(leftNewFace, leftOriginalFaceAValue + 1);
					A.put(rightNewFace, rightOriginalFaceAValue + 1);
					N.put(newBendPoint, 2);
					//insert new bend point in current cannonical order at position of opposite, move everything after one spot
					biconnectedCannonicalOrdering.add(biconnectedCannonicalOrdering.indexOf(opposite), newBendPoint); 
					// -> must increment k by 1; this is the only case where k is changed here
					++k;

					//insert a dummy edge at the spot where we just have split the edge of the quadrangle
					E dummyEdge = (E) ("dummyE_btw._"+vLowest+"_"+opposite);
					int index0 = Math.max(biconnectedGraph.getVertexIndexInEmbeddingList(vSecondHeighest, vLowest),
							biconnectedGraph.getVertexIndexInEmbeddingList(newBendPoint, vLowest));
					index0 = index0 == biconnectedGraph.degree(vLowest) - 1  ? 0 : index0;
					int index1 = Math.max(biconnectedGraph.getVertexIndexInEmbeddingList(newBendPoint, opposite),
							biconnectedGraph.getVertexIndexInEmbeddingList(vSecondHeighest, opposite));
					index1 = index1 == biconnectedGraph.degree(opposite) - 1 ? 0 : index1;
					biconnectedGraph.addEdge(dummyEdge, 
							vLowest, biconnectedGraph.
							getIndexOfVertexBoundingTheCommonFaceLaterInTheCircularOrder(vSecondHeighest, newBendPoint, vLowest), 
							opposite, biconnectedGraph.
							getIndexOfVertexBoundingTheCommonFaceLaterInTheCircularOrder(vSecondHeighest, newBendPoint, opposite));
					//update data structures
					Face<V, E> faceForInsertion =
							biconnectedGraph.getLeftIncidentFace(biconnectedGraph.findEdge(vLowest, vSecondHeighest)).containsVertex(v_k) ?
							biconnectedGraph.getRightIncidentFace(biconnectedGraph.findEdge(vLowest, vSecondHeighest))
							: biconnectedGraph.getLeftIncidentFace(biconnectedGraph.findEdge(vLowest, vSecondHeighest));
					A.remove(faceForInsertion);
					A.put(biconnectedGraph.getLeftIncidentFace(dummyEdge), 3);
					A.put(biconnectedGraph.getRightIncidentFace(dummyEdge), 3);
				}
				else if (isLastVertex) {
					System.out.println("Case1"); case1 = true;
				}
			}
		}
		return k;
	}
	
	/**
	 * The possibly covered vertex and the possibly covering vertex must be adjacent (direct neighbors).
	 * Lower hierarchy depths are currently not checked
	 * 
	 * @param covered
	 * @param covering
	 * @return
	 */
	public static <V,E> boolean isDirectlyCoveredBy(V covered, V covering, EmbeddedUndirectedGraph<V, E> biconnectedGraph, 
			ArrayList<V> biconnectedCannonicalOrdering) {
		assert biconnectedGraph.isNeighbor(covered, covering);
		
		int indexOfCovering = biconnectedCannonicalOrdering.indexOf(covering);
		indexOfCovering = indexOfCovering == -1 ? Integer.MAX_VALUE : indexOfCovering;
		int indexOfCovered = biconnectedCannonicalOrdering.indexOf(covered);
		indexOfCovered = indexOfCovered == -1 ? Integer.MAX_VALUE : indexOfCovered;
		
		if (indexOfCovered >= indexOfCovering) {
			return false;
		}
		
		int embeddingListIndexOfCoveredAtCovering = biconnectedGraph.getVertexIndexInEmbeddingList(covered, covering);
		V leftNeighborInEmbeddingList = biconnectedGraph.getNeighborsInEmbeddingOrder(covering).get(
				(embeddingListIndexOfCoveredAtCovering - 1 + biconnectedGraph.getNeighborsInEmbeddingOrder(covering).size()) 
				% biconnectedGraph.getNeighborsInEmbeddingOrder(covering).size()).getValue0();
		V rightNeighborInEmbeddingList = biconnectedGraph.getNeighborsInEmbeddingOrder(covering).get(
				(embeddingListIndexOfCoveredAtCovering + 1) % biconnectedGraph.getNeighborsInEmbeddingOrder(covering).size()).getValue0();
		if (biconnectedCannonicalOrdering.indexOf(leftNeighborInEmbeddingList) != -1 
				&& biconnectedCannonicalOrdering.indexOf(rightNeighborInEmbeddingList) != -1
				&& biconnectedCannonicalOrdering.indexOf(leftNeighborInEmbeddingList) < indexOfCovering 
				&& biconnectedCannonicalOrdering.indexOf(rightNeighborInEmbeddingList) < indexOfCovering) {
			return true;
		}
		return false;
	}

	private Face<V, E> getEmptyQuadrangleFace(EmbeddedUndirectedGraph<V, E> biconnectedGraph, Pair<Pair<V>> quadrangle) {
		Map<Face<V, E>, Integer> incidentFaces2NumberOfOccurences = new HashMap<>();
		V prevV = quadrangle.getSecond().getSecond();
		for (int i = 0; i < 2; ++i) {
			for (Pair<V> edge : quadrangle) {
				V v = i == 0 ? edge.getFirst() : edge.getSecond();
				for (Face<V, E> incidentFace : biconnectedGraph.getIncidentFaces(biconnectedGraph.findEdge(prevV, v))) {
					if (incidentFaces2NumberOfOccurences.containsKey(incidentFace)) {
						incidentFaces2NumberOfOccurences.replace(incidentFace, incidentFaces2NumberOfOccurences.get(incidentFace) + 1);
					}
					else {
						incidentFaces2NumberOfOccurences.put(incidentFace, 1);
					}
				}
				prevV = v;
			}
		}
		
		for (Face<V, E> incidentFace : incidentFaces2NumberOfOccurences.keySet()) {
			if (incidentFace.size() == 4 && incidentFaces2NumberOfOccurences.get(incidentFace) == 4 
					&& biconnectedGraph.getOuterFace() != incidentFace) {
				return incidentFace;
			}
		}
		
		return null;
	}

	private void initialize(EmbeddedUndirectedGraph<V, E> biconnectedGraph) {
		reset();
		biconnectedCannonicalOrdering = new ArrayList<>(biconnectedGraph.getVertexCount());
		allVerticesNotAddedYet = new LinkedHashSet<>(biconnectedGraph.getVertices());
		
		for (Face<V, E> face : biconnectedGraph.getFaces()) {
			A.put(face, 0);
		}
		for (V v : biconnectedGraph.getVertices()) {
			N.put(v, 0);
			F.put(v, 0);
		}
		Triplet<E,EdgeSide,Pair<V>> firstEdgeOfOuterFace = biconnectedGraph.getOuterFace().get(0);
		V v1;
		V v2;
		Face<V, E> innerFaceOfFirstEdge;
		if (firstEdgeOfOuterFace.getValue1() == EdgeSide.RIGHT) {
			v1 = biconnectedGraph.getEndpoints(firstEdgeOfOuterFace.getValue0()).getFirst();
			v2 = biconnectedGraph.getEndpoints(firstEdgeOfOuterFace.getValue0()).getSecond();
			innerFaceOfFirstEdge = biconnectedGraph.getLeftIncidentFace(firstEdgeOfOuterFace.getValue0());
		}
		else {
			v2 = biconnectedGraph.getEndpoints(firstEdgeOfOuterFace.getValue0()).getFirst();
			v1 = biconnectedGraph.getEndpoints(firstEdgeOfOuterFace.getValue0()).getSecond();
			innerFaceOfFirstEdge = biconnectedGraph.getRightIncidentFace(firstEdgeOfOuterFace.getValue0());
		}
		biconnectedCannonicalOrdering.add(v1);
		biconnectedCannonicalOrdering.add(v2);
		updateNeighbors(biconnectedGraph, v1);
		updateNeighbors(biconnectedGraph, v2);
		A.replace(innerFaceOfFirstEdge, 1);
		if (innerFaceOfFirstEdge.size() == 3) { //face becomes ready if it has only three vertices
			for (V v3 : innerFaceOfFirstEdge.getAllVertices()) {
				if (v1 != v3 && v2 != v3) {
					F.replace(v3, 1);
				}
			}
		}
	}
	
	private void addVertexToBiconnectedCannonicalOrderList(V v) {
		biconnectedCannonicalOrdering.add(v);
		allVerticesNotAddedYet.remove(v);
	}
	
	/**
	 * Updates all neighbors of v_k outside G_k
	 * 
	 * @param v_k
	 * 		Vertex that has now in the k-th step become the next the step in the biconnected cannonical order.
	 * 		That means it is the k-th vertex of the cannonical order
	 */
	private void updateNeighbors(EmbeddedUndirectedGraph<V, E> biconnectedGraph, V v_k) {
		for (V v : biconnectedGraph.getNeighbors(v_k)) {
//			if (allVerticesNotAddedYet.contains(v)) {
				N.replace(v, N.get(v) + 1);
//			}
		}
	}
	
	/**
	 * Difference to that method in the paper:
	 * Here we update all incident faces not only the ones at (w_i_1, v_k) and (w_i_p, v_k).
	 * This should not make anything wrong and it was done because it is not that easy to determine
	 * w_i_1 andd w_i_p
	 * 
	 * @param biconnectedGraph
	 * @param v_k
	 * 		k-th vertex of the biconnected cannonical order (the vertex that was added to G_{k-1} to get G_k)
	 */
	private void updateFaces(EmbeddedUndirectedGraph<V, E> biconnectedGraph, V v_k) {
		HashSet<Face<V, E>> affectedFaces = new LinkedHashSet<>();
		for (E incidentEdge : biconnectedGraph.getIncidentEdges(v_k)) {
			V neighborOfV_k = biconnectedGraph.getOpposite(v_k, incidentEdge);
			if (!allVerticesNotAddedYet.contains(neighborOfV_k)) {
				Face<V, E> fL = biconnectedGraph.getLeftIncidentFace(incidentEdge);
				Face<V, E> fR = biconnectedGraph.getRightIncidentFace(incidentEdge);
				A.replace(fL, A.get(fL) + 1);
				A.replace(fR, A.get(fR) + 1);
				affectedFaces.add(fL);
				affectedFaces.add(fR);
			}
		}
		
		//check if we now have ready faces
		for (Face<V, E> face : affectedFaces) {
			if (A.get(face) + 2 == face.size() && !face.equals(biconnectedGraph.getOuterFace())) {
				updateReadyFaces(biconnectedGraph, face);
			}
		}
	}
	
	/**
	 * When an face becomes "ready" that is only one vertex v is outside G_k after calling
	 * {@link BiconnectedCanonicalOrderer#updateFaces(EmbeddedUndirectedGraph, Object, Object, Object)}
	 * then this should be called in it with the face now being "ready" as parameter.
	 * 
	 * @param biconnectedGraph
	 * @param f
	 * 		newly "ready" face
	 */
	private void updateReadyFaces(EmbeddedUndirectedGraph<V, E> biconnectedGraph, Face<V, E> f) {
		V onlyVOutside = null; //Only vertex of f that is outside G_k
		for (Triplet<E,EdgeSide,Pair<V>> e : f) {
			for (V v : biconnectedGraph.getEndpoints(e.getValue0())) {
				if (allVerticesNotAddedYet.contains(v)) {
					assert onlyVOutside == null || onlyVOutside == v : "Function call was made for Face "+f+", but that was no ready face. "
							+ "(More than one vertex of that face lies outside G_k)";
					onlyVOutside = v;
				}
			}
		}
		assert onlyVOutside != null : "Function call was made for Face "+f+", but that was no ready face. "
				+ "(No vertex of that face lies outside G_k, face was already completely inside)";
		
		F.replace(onlyVOutside, F.get(onlyVOutside) + 1);
	}
	
	private boolean hasLeftSupport(EmbeddedUndirectedGraph<V, E> biconnectedGraph, V v, V neighborOfVInContour) {
		int indexOfV = indexOfVAtUInCircularOrder(biconnectedGraph, v, neighborOfVInContour);
		int neighborCount = biconnectedGraph.getNeighborsInEmbeddingOrder(neighborOfVInContour).size();
		return !allVerticesNotAddedYet.contains(biconnectedGraph.getNeighborsInEmbeddingOrder(neighborOfVInContour)
				.get((indexOfV+1)%neighborCount).getValue0());
	}
	
	private boolean hasRightSupport(EmbeddedUndirectedGraph<V, E> biconnectedGraph, V v, V neighborOfVInContour) {
		int indexOfV = indexOfVAtUInCircularOrder(biconnectedGraph, v, neighborOfVInContour);
		int neighborCount = biconnectedGraph.getNeighborsInEmbeddingOrder(neighborOfVInContour).size();
		return !allVerticesNotAddedYet.contains(biconnectedGraph.getNeighborsInEmbeddingOrder(neighborOfVInContour)
				.get((indexOfV-1+neighborCount)%neighborCount).getValue0());
	}
	
	private int indexOfVAtUInCircularOrder(EmbeddedUndirectedGraph<V, E> biconnectedGraph, V v, V u) {
		int index = -1;
		for (int i = 0; i < biconnectedGraph.getNeighborsInEmbeddingOrder(u).size(); ++i) {
			if (biconnectedGraph.getNeighborsInEmbeddingOrder(u).get(i).getValue0().equals(v)) {
				index = i;
				break;
			}
		}
		return index;
	}
}
