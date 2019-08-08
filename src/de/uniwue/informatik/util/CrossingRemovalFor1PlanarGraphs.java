package de.uniwue.informatik.util;

import java.util.Collection;
import java.util.LinkedHashSet;

import de.uniwue.informatik.algorithms.layout.VData;
import edu.uci.ics.jung.graph.util.Pair;
import de.uniwue.informatik.graph.embedding.EmbeddedUndirectedGraph;

public class CrossingRemovalFor1PlanarGraphs {
	
	/**
	 * 
	 * @param embedded1planarGraph
	 * @param crossingVertices
	 * @return
	 * 		A collection where each element stands for a removed crossing vertex.
	 * 		Such an element consists of two pairs of vertices. These stand for the 4 vertices that were adjacent to the
	 * 		removed crossing-vertex. They are paired to the two removed edges.
	 * 		So one such element consists of a pair that is the two edges and each edge has two endpoints, thus a pair of pairs of vertices.
	 */
	public static Collection<Pair<Pair<VData<String>>>> removeCrossings(EmbeddedUndirectedGraph<VData<String>, String> embedded1planarGraph, 
			Collection<VData<String>> crossingVertices) {
		LinkedHashSet<Pair<Pair<VData<String>>>> removedEdges = new LinkedHashSet<>();
		for (VData<String> crossingVertex : crossingVertices) {
			assert embedded1planarGraph.degree(crossingVertex) == 4 : "Vertex >"+crossingVertex+"< is no valid crossing vertex because"
					+ " it has degree "+embedded1planarGraph.degree(crossingVertex)+", but should have degree 4.";
			//the two edges are defined by the embedding: The two edges that are not neighbors in the rotation system around
			//the crossing vertex are adjacent via the removed edge
			Pair<VData<String>> edge0 = new Pair<>(embedded1planarGraph.getNeighborsInEmbeddingOrder(crossingVertex).get(0).getValue0(), 
					embedded1planarGraph.getNeighborsInEmbeddingOrder(crossingVertex).get(2).getValue0());
			Pair<VData<String>> edge1 = new Pair<>(embedded1planarGraph.getNeighborsInEmbeddingOrder(crossingVertex).get(1).getValue0(),
					embedded1planarGraph.getNeighborsInEmbeddingOrder(crossingVertex).get(3).getValue0());
			removedEdges.add(new Pair<Pair<VData<String>>>(edge0, edge1));
			//remove vertex with incident edges
			embedded1planarGraph.removeVertex(crossingVertex);
		}
		return removedEdges;
	}
}
