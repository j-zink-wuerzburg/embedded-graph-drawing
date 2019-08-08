package de.uniwue.informatik.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;

import org.javatuples.Pair;
import org.javatuples.Triplet;

import de.uniwue.informatik.algorithms.layout.VData;
import de.uniwue.informatik.algorithms.layout.VData.VType;
import de.uniwue.informatik.graph.embedding.EdgeSide;
import de.uniwue.informatik.graph.embedding.EmbeddedUndirectedGraph;
import de.uniwue.informatik.graph.embedding.Face;

public class DummyEdgeInsertion {
	
	/**
	 * Adds dummy edges for each pair of crossing edges to form an empty kite.
	 * An empty kite is the pair of crossing edges plus edges between each pair of not via the crossing edges adjacent vertices
	 * that are the endpoints of the two crossing edges.
	 * In the planarized embedding these edges must be direct neighbors of the crossing edges (nothing in between - covers the crossing
	 * edges).
	 * If the graph already contains one of those four covering kite-edges, then only the ones are inserted that are missing.
	 * If that would lead to multi edges (because the original edge is there but no direct neighbor of the crossing edges
	 * in the planarized embedding), then the original edge is split via an dummy vertex (can become a bend point of that edge
	 * in a drawing)
	 * 
	 * @param embeddedNICPlanarGraph
	 * 		Embedded graph that is NIC-planar (NIC = nearly independent crossing).
	 * 		For each combination of two pairs of crossing edges there is at most one endpoint (vertex) that have both pairs of edges
	 * 		in common.
	 * @param crossingVertices
	 * 		Set of crossing vertices. These must be vertices of the passed graph. Only crossings of two edges are allowed, so
	 * 		each crossing vertex must have exactly four incident edges. Moreover in that case they must satisfy the NIC-condition.
	 * @return
	 * 		Inserted dummy edges, but not the split edges (original edges that were split by an dummy vertex)
	 */
	public static Collection<String> insertEmptyKites(EmbeddedUndirectedGraph<VData<String>, String> embeddedNICPlanarGraph, 
			Collection<VData<String>> crossingVertices) {

		LinkedHashSet<String> insertedDummyEdges = new LinkedHashSet<>();
		
		for (VData<String> crossingVertex : crossingVertices) {
			assert embeddedNICPlanarGraph.degree(crossingVertex) == 4 : "Crossing vertices must have degree 4. "
					+ "(but "+crossingVertex+" has degree "+embeddedNICPlanarGraph.degree(crossingVertex)+")";
			
			
			VData[] v = new VData[4];
			v[0] = embeddedNICPlanarGraph.getNeighborsInEmbeddingOrder(crossingVertex).get(0).getValue0();
			v[1] = embeddedNICPlanarGraph.getNeighborsInEmbeddingOrder(crossingVertex).get(1).getValue0();
			v[2] = embeddedNICPlanarGraph.getNeighborsInEmbeddingOrder(crossingVertex).get(2).getValue0();
			v[3] = embeddedNICPlanarGraph.getNeighborsInEmbeddingOrder(crossingVertex).get(3).getValue0();
			for (int i = 0; i < 4; ++i) {
				int j = (i+1)%4;
				
				boolean insertDummyKiteEdge = true;
				if (embeddedNICPlanarGraph.isNeighbor(v[i], v[j])) {
					//edge is already there
					if ((embeddedNICPlanarGraph.getVertexIndexInEmbeddingList(v[j], v[i]) + 1) % embeddedNICPlanarGraph.degree(v[i]) 
							== embeddedNICPlanarGraph.getVertexIndexInEmbeddingList(crossingVertex, v[i])
							&& embeddedNICPlanarGraph.getVertexIndexInEmbeddingList(v[i], v[j])
							== (embeddedNICPlanarGraph.getVertexIndexInEmbeddingList(crossingVertex, v[j]) + 1) 
							% embeddedNICPlanarGraph.degree(v[j])) {
						//case 1: it is on the correct spot (direct neighbor to the crossing edges)
						insertDummyKiteEdge = false;
					}
					else {
						//case 2: it is on the wrong spot -> split the original one and insert the dummy one later, too
						String edgeToBeSplit = embeddedNICPlanarGraph.findEdge(v[i], v[j]);
						VData<String> dummySplitVertex = new VData<String>(VType.BEND_POINT);
						EmbeddedGraphOperations.splitEdgeViaAVertex(embeddedNICPlanarGraph, edgeToBeSplit, dummySplitVertex, 
								edgeToBeSplit+"-part_0", edgeToBeSplit+"-part_1");
					}
				}
				//else no edge -> just insert it
				
				
				if (insertDummyKiteEdge) {
					//insert edge
					String dummyEdge = "kite-dummy-edge-"+v[i]+"-"+v[j];
					embeddedNICPlanarGraph.addEdge(dummyEdge, v[i], embeddedNICPlanarGraph.getVertexIndexInEmbeddingList(crossingVertex, v[i]),
							v[j], embeddedNICPlanarGraph.getVertexIndexInEmbeddingList(crossingVertex, v[j]) + 1);
					insertedDummyEdges.add(dummyEdge);
					
					//if the crossing was on the outer face one of the two new faces is the new outer face
					//an face of the empty kite must not be the outer face. If so -> neighbor will be the outer face
					if (embeddedNICPlanarGraph.getOuterFace().containsVertex(crossingVertex)) {
						Face<VData<String>, String> oldOuterFace = embeddedNICPlanarGraph.getOuterFace();
						Face<VData<String>, String> newOuterFace = embeddedNICPlanarGraph.getLeftIncidentFace(dummyEdge).equals(oldOuterFace) ?
								embeddedNICPlanarGraph.getRightIncidentFace(dummyEdge) : embeddedNICPlanarGraph.getLeftIncidentFace(dummyEdge);
						embeddedNICPlanarGraph.setOuterFace(newOuterFace);
					}
				}
			}
		}
		return insertedDummyEdges;
	}
	
	/**
	 *  A star is for every face with degree greater than 3 a vertex in that face plus an edge from that vertex to each
	 *  vertex of the face.
	 *  This can lead to multi edges.
	 *  To avoid multi edges the following is done:
	 *  Whenever we encounter a vertex twice (or more often) in one face, instead of a multi edge
	 *  a split edge is inserted, that means a path of to edges connected by another dummy node.
	 *  In total for each face with degree greater than 3 there is one central dummy vertex added plus
	 *  up to degree of the face many more dummy vertices (for the split edges to avoid multi edges).
	 * 
	 * @param embeddedGraph
	 * 		Embedded graph in which to insert these stars.
	 * @return
	 * 		Collection of dummy vertices that were added (the edges adjacent to them are exactly the dummy edges)
	 */
	public static Collection<VData<String>> starTriangulateGraph(EmbeddedUndirectedGraph<VData<String>, String> embeddedGraph) {
		ArrayList<Pair<Pair<VData<String>, VData<String>>, Pair<String, String>>> dummyEdgesToBeAddedPlusSurroundingEdges = 
				new ArrayList<>(2 * embeddedGraph.getEdges().size());
		HashSet<VData<String>> dummyVertices = new LinkedHashSet<>();
		
		//find at first edges to be added
		int dummyVertexCounter = 0;
		int dummyEdgeCounter = 0;
		for (Face<VData<String>, String> face : embeddedGraph.getFaces()) {
			if (face.size() > 3) {
				VData<String> centralDummyVertex = new VData<String>("dummy_v_"+(dummyVertexCounter++));
				dummyVertices.add(centralDummyVertex);
				String prevEdge = face.get(face.size() - 1).getValue0();
				for (Triplet<String, EdgeSide, edu.uci.ics.jung.graph.util.Pair<VData<String>>> edgeData : face) {
					VData<String> targetVertex = edgeData.getValue1() == 
							EdgeSide.LEFT ? edgeData.getValue2().getFirst() : edgeData.getValue2().getSecond();
					dummyEdgesToBeAddedPlusSurroundingEdges.add(
							new Pair<>(new Pair<>(centralDummyVertex, targetVertex), new Pair<>(prevEdge, edgeData.getValue0())));
					prevEdge = edgeData.getValue0();
				}
			}
		}
		
		//add vertices
		for (VData<String> dummyVertex : dummyVertices) {
			embeddedGraph.addVertex(dummyVertex);
		}
		
		//add edges
		for (Pair<Pair<VData<String>, VData<String>>, Pair<String, String>> edgeData : dummyEdgesToBeAddedPlusSurroundingEdges) {
			VData<String> centralDummyVertex = edgeData.getValue0().getValue0();
			VData<String> targetVertex = edgeData.getValue0().getValue1();
			int indexAtCentralDummyVertex = embeddedGraph.degree(centralDummyVertex);
			int indexAtTargetVertex = (embeddedGraph.getVertexIndexInEmbeddingList(embeddedGraph.getOpposite(
					targetVertex, edgeData.getValue1().getValue1()), targetVertex) + 1) % embeddedGraph.degree(targetVertex);
			if (embeddedGraph.isNeighbor(centralDummyVertex, targetVertex)) {
				//avoid multiple edges by splitting the existing edge and then inserting that new one
				String edgeToBeSplit = embeddedGraph.findEdge(centralDummyVertex, targetVertex);
				VData<String> splittingDummyVertex = new VData<String>(VType.BEND_POINT);
				dummyVertices.add(splittingDummyVertex);
				EmbeddedGraphOperations.splitEdgeViaAVertex(embeddedGraph, edgeToBeSplit, splittingDummyVertex, 
						"firstPartOf_"+edgeToBeSplit, "secondPartOf_"+edgeToBeSplit);		
			}
			//now add the new one
			embeddedGraph.addEdge("dummy_e_"+(dummyEdgeCounter++), centralDummyVertex, indexAtCentralDummyVertex, targetVertex, indexAtTargetVertex);
		}
		
		return dummyVertices;
	}
}
