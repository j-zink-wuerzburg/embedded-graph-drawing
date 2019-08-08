package de.uniwue.informatik.main;

import de.uniwue.informatik.algorithms.layout.HarelSardas;
import de.uniwue.informatik.algorithms.layout.VData;
import de.uniwue.informatik.graph.embedding.EmbeddedUndirectedGraph;

public class HarelSardasMain {
	
	public static void main(String[] args) {
		
		drawCircle(8);
	}
	
	public static void drawCircle(int numberOfVertices) {
		EmbeddedUndirectedGraph<VData<String>, String> graph = new EmbeddedUndirectedGraph<>();
		VData<String> firstV = null;
		VData<String> prevV = null;
		for (int i = 0; i < numberOfVertices; ++i) {
			VData<String> v = new VData<String>("v"+i);
			if (i == 0) {
				firstV = v;
			}
			graph.addVertex(v);
			if (i != 0) {
				graph.addEdge("e"+i, prevV, 0, v, 0);
			}
			prevV = v;
		}
		graph.addEdge("e"+0, firstV, 0, prevV, 0);
		
		HarelSardas<VData<String>, String> hs = new HarelSardas<>(graph);
		while (!hs.done()) {
			hs.step();
		}
		DrawGraphs.visualizeDrawing(hs.transformToFloatingPointLayout());
	}
}
