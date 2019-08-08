package de.uniwue.informatik.util;

import de.uniwue.informatik.graph.embedding.EmbeddedUndirectedGraph;

public class EmbeddedGraphOperations {
	
	public static <V, E> void splitEdgeViaAVertex(EmbeddedUndirectedGraph<V, E> embeddedGraph, E edgeToBeSplit, V viaThatVertex, 
			E newFirstPartOfSplitEdge, E newSecondPartOfSplitEdge) {
		if (!embeddedGraph.containsVertex(viaThatVertex)) {
			embeddedGraph.addVertex(viaThatVertex);
		}
		assert embeddedGraph.degree(viaThatVertex) == 0;
		assert embeddedGraph.containsEdge(edgeToBeSplit);
		
		V v0 = embeddedGraph.getEndpoints(edgeToBeSplit).getFirst();
		V v1 = embeddedGraph.getEndpoints(edgeToBeSplit).getSecond();
		int indexAtV0 = (embeddedGraph.getEdgeIndexInEmbeddingList(edgeToBeSplit, v0) + 1) % embeddedGraph.degree(v0);
		int indexAtV1 = embeddedGraph.getEdgeIndexInEmbeddingList(edgeToBeSplit, v1);
		boolean outerFaceIsLeft = embeddedGraph.getLeftFace(edgeToBeSplit, v0).equals(embeddedGraph.getOuterFace());
		boolean outerFaceIsRight = embeddedGraph.getRightFace(edgeToBeSplit, v0).equals(embeddedGraph.getOuterFace());
		embeddedGraph.addEdge(newFirstPartOfSplitEdge, v0, indexAtV0, viaThatVertex, 0);
		embeddedGraph.addEdge(newSecondPartOfSplitEdge, viaThatVertex, 1, v1, indexAtV1);
		embeddedGraph.removeEdge(edgeToBeSplit);
		
		if(outerFaceIsLeft) {
			embeddedGraph.setOuterFace(embeddedGraph.getLeftFace(embeddedGraph.getEndpoints(newFirstPartOfSplitEdge).contains(v0) ? 
					newFirstPartOfSplitEdge : newSecondPartOfSplitEdge, v0));
		}
		if(outerFaceIsRight) {
			embeddedGraph.setOuterFace(embeddedGraph.getRightFace(embeddedGraph.getEndpoints(newFirstPartOfSplitEdge).contains(v0) ? 
					newFirstPartOfSplitEdge : newSecondPartOfSplitEdge, v0));
		}
	}
}
