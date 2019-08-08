package de.uniwue.informatik.main;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.apache.commons.collections15.map.HashedMap;
import org.javatuples.Pair;

import com.google.common.base.Function;

import de.uniwue.informatik.algorithms.layout.HarelSardas;
import de.uniwue.informatik.algorithms.layout.VData;
import de.uniwue.informatik.algorithms.layout.VData.VType;
import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.LayoutScalingControl;
import edu.uci.ics.jung.visualization.decorators.AbstractVertexShapeTransformer;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;
import de.uniwue.informatik.graph.embedding.EmbeddedUndirectedGraph;
import de.uniwue.informatik.util.CrossingRemovalFor1PlanarGraphs;
import de.uniwue.informatik.util.DefaultVisualizationModelWithoutReiterating;
import de.uniwue.informatik.util.DummyEdgeInsertion;
import de.uniwue.informatik.util.IpeFileWriter;

public class DrawGraphs {
	
	private static HarelSardas<VData<String>, String> hs;
	private static Dimension drawingArea = new Dimension(40, 40);
	private static Dimension drawingPaneSize = new Dimension(1400, 800);
	
	private static Collection<String> dummyEdges;
	private static Collection<VData<String>> dummyVertices;
	private static Collection<edu.uci.ics.jung.graph.util.Pair<edu.uci.ics.jung.graph.util.Pair<VData<String>>>> removedEdges;
	private static boolean dummyObjectsRemoved = false;
	private static boolean edgesAreReinsertedAsRACInNICPlanarCase = false;
	private static boolean gridRefined = false;
	private static int gridRefinementFactor = 2;
	
	public static boolean allCasesAppear = false;

	public static void main(String[] args) throws IOException {
		/*
		 * Draw a NIC-planar graph on quadratic area as in the algorithm
		 */
		edgesAreReinsertedAsRACInNICPlanarCase = true;
		//select graph
		Pair<EmbeddedUndirectedGraph<VData<String>, String>, LinkedHashSet<VData<String>>> graphData = getNICPlanarGraphFromSketch2();
		EmbeddedUndirectedGraph<VData<String>, String> embeddedGraph = graphData.getValue0();
		for (String e : new ArrayList<>(embeddedGraph.getEdges())) {
			for (int i=0; i<2;i++) {
				graphData = getNICPlanarGraphFromSketch2();
				embeddedGraph = graphData.getValue0();
				allCasesAppear = false;
				System.out.println("-------------------------------");
				if (i == 0) {
					embeddedGraph.setOuterFace(embeddedGraph.getLeftIncidentFace(e));
					System.out.println("outer face at left side of "+e+"-"+embeddedGraph.getEndpoints(e));
				}
				else {
					embeddedGraph.setOuterFace(embeddedGraph.getRightIncidentFace(e));
					System.out.println("outer face at right side of "+e+"-"+embeddedGraph.getEndpoints(e));
				}
				try {
					//adjust graph
					dummyEdges = DummyEdgeInsertion.insertEmptyKites(embeddedGraph, graphData.getValue1()); //dummyKiteEdges
					dummyVertices = new ArrayList<>(); //DummyEdgeInsertion.starTriangulateGraph(embeddedGraph);
					removedEdges = CrossingRemovalFor1PlanarGraphs.removeCrossings(embeddedGraph, graphData.getValue1());
					//TODO: make biconnected
					
					//draw it
					hs = new HarelSardas<>(embeddedGraph, removedEdges);
					for (String dummyEdge : new ArrayList<>(dummyEdges)) { //replace replaced dummy edges by their replacement
						if (hs.originalEdgesReplacedByASplitEdge.containsKey(dummyEdge)) {
							dummyEdges.remove(dummyEdge);
							dummyEdges.add(hs.originalEdgesReplacedByASplitEdge.get(dummyEdge).getValue0());
							dummyVertices.add(hs.originalEdgesReplacedByASplitEdge.get(dummyEdge).getValue1());
							dummyEdges.add(hs.originalEdgesReplacedByASplitEdge.get(dummyEdge).getValue2());
						}
					}
					//next line is hard coded stop condition for a "good" graph drawing in our example
					if (allCasesAppear && i == 1 && !e.equals("e39") && !e.equals("e43")) { // e.equals("e1") && i == 1) {
						break;
					}
				}
				catch (Exception ex) {
					//do nothing
				}
				catch (AssertionError a) {
					
				}
			}
			//next line is hard coded stop condition for a "good" graph drawing in our example
			if (allCasesAppear && !e.equals("e39") && !e.equals("e43")) { // && e.equals("e1")) {
				break;
			}
		}
		while (!hs.done()) {
			hs.step();
		}
		visualizeDrawing(hs.transformToFloatingPointLayout(drawingArea));
	}
	
	
	
	public static EmbeddedUndirectedGraph<VData<String>, String> getStackedTriangles(int numberOfTriangles) {
		if (numberOfTriangles <= 0) {
			return null;
		}
		
		EmbeddedUndirectedGraph<VData<String>, String> graph = new EmbeddedUndirectedGraph<>();
		VData<String> v1 = new VData<String>("v1");
		graph.addVertex(v1);
		VData<String> v2 = new VData<String>("v2");
		graph.addVertex(v2);
		int edgeCounter = 0;
		graph.addEdge("e"+(edgeCounter++), v1, 0, v2, 0);
		for (int i = 0; i < numberOfTriangles; ++i) {
			VData<String> topVertex = new VData<String>("v"+(i+3));
			graph.addVertex(topVertex);
			graph.addEdge("e"+(edgeCounter++), v1, graph.degree(v1), topVertex, 0);
			graph.addEdge("e"+(edgeCounter++), v2, 0, topVertex, 1);
		}
		
		return graph;
	}
	
	
	private static Map<String, VData<String>> name2vData = new HashedMap<>(); //crossing vertices are not in this map
	/**
	 * 
	 * @return
	 * 		value0 = embedded graph, value1 = crossing vertices in that graph
	 */
	public static Pair<EmbeddedUndirectedGraph<VData<String>, String>, LinkedHashSet<VData<String>>> getNICPlanarGraphFromSketch() {
		EmbeddedUndirectedGraph<VData<String>, String> graph = new EmbeddedUndirectedGraph<>();
		LinkedHashSet<VData<String>> crossingVertices = new LinkedHashSet<>();
		//init vertices
		VData<String>[] vertex = new VData[29]; //25 vertices (v0 - v24, and then 4 crossing vertices)
		for (int i = 0; i < 25; ++i) {
			String name = "v"+i;
			vertex[i] = new VData<String>(name);
			name2vData.put(name, vertex[i]);
			graph.addVertex(vertex[i]);
		}
		for (int i = 25; i < 29; ++i) {
			vertex[i] = new VData<String>(VType.CROSSING_POINT);
			graph.addVertex(vertex[i]);
			crossingVertices.add(vertex[i]);
		}
		//init edges
		int edgeCounter = 0;
		graph.addEdge("e"+(edgeCounter++), vertex[0], 0, vertex[1], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[0], 1, vertex[3], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[0], 2, vertex[2], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[1], 1, vertex[2], 1);
		graph.addEdge("e"+(edgeCounter++), vertex[1], 2, vertex[4], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[2], 1, vertex[3], 1);
		graph.addEdge("e"+(edgeCounter++), vertex[2], 2, vertex[4], 1);
		graph.addEdge("e"+(edgeCounter++), vertex[3], 1, vertex[25], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[4], 2, vertex[5], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[4], 3, vertex[8], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[4], 4, vertex[17], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[4], 5, vertex[20], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[4], 6, vertex[19], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[5], 1, vertex[25], 1);
		graph.addEdge("e"+(edgeCounter++), vertex[5], 2, vertex[7], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[5], 3, vertex[8], 1);
		graph.addEdge("e"+(edgeCounter++), vertex[6], 0, vertex[25], 1);
		graph.addEdge("e"+(edgeCounter++), vertex[6], 1, vertex[7], 1);
		graph.addEdge("e"+(edgeCounter++), vertex[7], 1, vertex[25], 2);
		graph.addEdge("e"+(edgeCounter++), vertex[7], 3, vertex[11], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[7], 4, vertex[10], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[7], 5, vertex[26], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[8], 2, vertex[26], 1);
		graph.addEdge("e"+(edgeCounter++), vertex[9], 0, vertex[26], 1);
		graph.addEdge("e"+(edgeCounter++), vertex[9], 1, vertex[12], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[10], 1, vertex[11], 1);
		graph.addEdge("e"+(edgeCounter++), vertex[10], 2, vertex[12], 1);
		graph.addEdge("e"+(edgeCounter++), vertex[11], 1, vertex[12], 2);
		graph.addEdge("e"+(edgeCounter++), vertex[12], 3, vertex[14], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[12], 4, vertex[27], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[12], 5, vertex[13], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[13], 1, vertex[27], 1);
		graph.addEdge("e"+(edgeCounter++), vertex[13], 2, vertex[15], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[13], 3, vertex[26], 2);
		graph.addEdge("e"+(edgeCounter++), vertex[14], 1, vertex[15], 1);
		graph.addEdge("e"+(edgeCounter++), vertex[14], 2, vertex[27], 1);
		graph.addEdge("e"+(edgeCounter++), vertex[15], 1, vertex[27], 2);
		graph.addEdge("e"+(edgeCounter++), vertex[15], 3, vertex[19], 1);
		graph.addEdge("e"+(edgeCounter++), vertex[15], 4, vertex[23], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[15], 5, vertex[28], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[15], 6, vertex[16], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[16], 1, vertex[28], 1);
		graph.addEdge("e"+(edgeCounter++), vertex[16], 2, vertex[18], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[16], 3, vertex[17], 1);
		graph.addEdge("e"+(edgeCounter++), vertex[17], 2, vertex[18], 1);
		graph.addEdge("e"+(edgeCounter++), vertex[18], 1, vertex[28], 1);
		graph.addEdge("e"+(edgeCounter++), vertex[18], 2, vertex[21], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[18], 3, vertex[19], 1);
		graph.addEdge("e"+(edgeCounter++), vertex[18], 4, vertex[20], 1);
		graph.addEdge("e"+(edgeCounter++), vertex[19], 1, vertex[20], 2);
		graph.addEdge("e"+(edgeCounter++), vertex[19], 3, vertex[21], 1);
		graph.addEdge("e"+(edgeCounter++), vertex[19], 4, vertex[28], 1);
		graph.addEdge("e"+(edgeCounter++), vertex[19], 5, vertex[22], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[22], 1, vertex[23], 1);
		graph.addEdge("e"+(edgeCounter++), vertex[22], 2, vertex[24], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[23], 1, vertex[24], 1);
		//set outer face
		graph.setOuterFace(graph.getRightFace(graph.findEdge(vertex[0], vertex[3]), vertex[0]));
		//TODO: Check graph.setOuterFace(graph.getRightFace(graph.findEdge(vertex[17], vertex[4]), vertex[4])); Something goes wrong there
		
		return new Pair<>(graph, crossingVertices);
	}
	

	/**
	 * Like getNICPlanarGraphFromSketch2 but with some more edges
	 * 
	 * @return
	 * 		value0 = embedded graph, value1 = crossing vertices in that graph
	 */
	public static Pair<EmbeddedUndirectedGraph<VData<String>, String>, LinkedHashSet<VData<String>>> getNICPlanarGraphFromSketch2() {
		Pair<EmbeddedUndirectedGraph<VData<String>,String>,LinkedHashSet<VData<String>>> pair = getNICPlanarGraphFromSketch();
		EmbeddedUndirectedGraph<VData<String>, String> graph = pair.getValue0();
		int edgeCounter = graph.getEdgeCount();
		graph.addEdge("e"+(edgeCounter++), name2vData.get("v6"), 1, name2vData.get("v11"), 1);
		graph.addEdge("e"+(edgeCounter++), name2vData.get("v11"), 2, name2vData.get("v14"), 1);	
		return pair;
	}
	
	public static EmbeddedUndirectedGraph<VData<String>, String> getEmbeddedGraphFromPaper() {
		EmbeddedUndirectedGraph<VData<String>, String> graph = new EmbeddedUndirectedGraph<>();
		//init vertices
		VData<String>[] vertex = new VData[29]; //my mistake: started counting at 1, so forget about index 0-object (28 objects after are relevant)
		for (int i = 1; i < vertex.length; ++i) {
			vertex[i] = new VData<String>("v"+i);
			graph.addVertex(vertex[i]);
		}
		//init edges
		int edgeCounter = 0;
		graph.addEdge("e"+(edgeCounter++), vertex[1], 0, vertex[2], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[1], 1, vertex[6], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[1], 2, vertex[28], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[1], 3, vertex[26], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[1], 4, vertex[24], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[2], 1, vertex[4], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[3], 0, vertex[4], 1);
		graph.addEdge("e"+(edgeCounter++), vertex[3], 1, vertex[6], 1);
		graph.addEdge("e"+(edgeCounter++), vertex[4], 1, vertex[5], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[4], 2, vertex[6], 2);
		graph.addEdge("e"+(edgeCounter++), vertex[5], 1, vertex[8], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[6], 3, vertex[7], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[6], 4, vertex[14], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[7], 1, vertex[8], 1);
		graph.addEdge("e"+(edgeCounter++), vertex[7], 2, vertex[10], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[8], 1, vertex[9], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[9], 1, vertex[10], 1);
		graph.addEdge("e"+(edgeCounter++), vertex[10], 2, vertex[11], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[10], 3, vertex[12], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[10], 4, vertex[13], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[10], 5, vertex[14], 1);
		graph.addEdge("e"+(edgeCounter++), vertex[11], 1, vertex[13], 1);
		graph.addEdge("e"+(edgeCounter++), vertex[11], 2, vertex[12], 1);
		graph.addEdge("e"+(edgeCounter++), vertex[13], 2, vertex[18], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[13], 3, vertex[17], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[13], 4, vertex[14], 2);
		graph.addEdge("e"+(edgeCounter++), vertex[14], 3, vertex[16], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[14], 4, vertex[15], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[15], 1, vertex[16], 1);
		graph.addEdge("e"+(edgeCounter++), vertex[16], 1, vertex[18], 1);
		graph.addEdge("e"+(edgeCounter++), vertex[16], 2, vertex[23], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[17], 1, vertex[18], 2);
		graph.addEdge("e"+(edgeCounter++), vertex[18], 1, vertex[21], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[18], 2, vertex[19], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[19], 1, vertex[20], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[19], 2, vertex[22], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[19], 3, vertex[23], 1);
		graph.addEdge("e"+(edgeCounter++), vertex[20], 1, vertex[21], 1);
		graph.addEdge("e"+(edgeCounter++), vertex[20], 2, vertex[22], 1);
		graph.addEdge("e"+(edgeCounter++), vertex[22], 2, vertex[23], 2);
		graph.addEdge("e"+(edgeCounter++), vertex[23], 3, vertex[24], 1);
		graph.addEdge("e"+(edgeCounter++), vertex[23], 4, vertex[25], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[23], 5, vertex[28], 1);
		graph.addEdge("e"+(edgeCounter++), vertex[25], 1, vertex[26], 1);
		graph.addEdge("e"+(edgeCounter++), vertex[26], 1, vertex[27], 0);
		graph.addEdge("e"+(edgeCounter++), vertex[27], 1, vertex[28], 2);
		//set outer face
		graph.setOuterFace(graph.getRightFace(graph.findEdge(vertex[15], vertex[19]), vertex[19]));
		
		return graph;
	}
	
	public static void visualizeDrawing(Layout<VData<String>, String> layout) {
		//Visualize created drawings
		//first layout
		final VisualizationViewer<VData<String>, String> vv1 = new VisualizationViewer<>(
				new DefaultVisualizationModelWithoutReiterating<>(layout, drawingArea), drawingPaneSize);
		vv1.scaleToLayout(new LayoutScalingControl());
		vv1.getRenderContext().setEdgeShapeTransformer(new EdgeShape<VData<String>,String>(layout.getGraph()).new Line());
		vv1.getRenderContext().setVertexShapeTransformer(new AbstractVertexShapeTransformer<VData<String>>() {
			@Override
			public Shape apply(VData<String> input) {
				return factory.getEllipse(input);
			}
		});
		vv1.getRenderContext().setVertexLabelTransformer(new Function<VData<String>, String>(){
			@Override
			public String apply(VData<String> input) {
				return input.toString()+"_("+((int) hs.apply(input).getX())+","+((int) hs.apply(input).getY())+")";
			}
		});
		vv1.getRenderer().getVertexLabelRenderer().setPosition(Position.N);
		GraphZoomScrollPane scrollPane1 = new GraphZoomScrollPane(vv1);
	    vv1.setGraphMouse(new DefaultModalGraphMouse<String,Number>());
		JFrame frame = new JFrame("Graph Drawing");
		JButton reset1 = new JButton("Reset position and zoom");
	    reset1.addActionListener(new ActionListener() {
	    	public void actionPerformed(ActionEvent e) {
				vv1.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT).setToIdentity();
				vv1.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.VIEW).setToIdentity();
			}
	    });
	    JButton stepButton = new JButton("Step");
	    stepButton.addActionListener(new ActionListener() {
	    	public void actionPerformed(ActionEvent e) {
	    		if (!hs.done()) {
	    			hs.step();
	    			AbstractLayout<VData<String>, String> layoutInThisStep = hs.transformToFloatingPointLayout(drawingArea);
	    			vv1.setModel(new DefaultVisualizationModelWithoutReiterating<>(layoutInThisStep, drawingArea));
	    			vv1.getRenderContext().setEdgeShapeTransformer(
	    					new EdgeShape<VData<String>,String>(layoutInThisStep.getGraph()).new Line());
					vv1.repaint();
	    		}
	    		else if (edgesAreReinsertedAsRACInNICPlanarCase) {
	    			if (!gridRefined) {
	    				//refine grid
	    				for (VData<String> v : hs.getGraph().getVertices()) {
	    					hs.setLocation(v, hs.getX(v) * gridRefinementFactor, hs.getY(v) * gridRefinementFactor);
	    				}
	    				gridRefined = true;
		    			AbstractLayout<VData<String>, String> layoutInThisStep = hs.transformToFloatingPointLayout(drawingArea);
		    			vv1.setModel(new DefaultVisualizationModelWithoutReiterating<>(layoutInThisStep, drawingArea));
		    			vv1.getRenderContext().setEdgeShapeTransformer(
		    					new EdgeShape<VData<String>,String>(layoutInThisStep.getGraph()).new Line());
						vv1.repaint();
	    				IpeFileWriter.writeFile("target/drawings", "drawingBeforeEdgeInsertion", 
	    						(AbstractLayout<VData<String>, String>) layoutInThisStep, null, true);
	    			}
	    			else {
	    				hs.reinsertCrossingEdgesInNICplanarCase(removedEdges);
	    				edgesAreReinsertedAsRACInNICPlanarCase = false;
		    			AbstractLayout<VData<String>, String> layoutInThisStep = hs.transformToFloatingPointLayout(drawingArea);
		    			vv1.setModel(new DefaultVisualizationModelWithoutReiterating<>(layoutInThisStep, drawingArea));
		    			vv1.getRenderContext().setEdgeShapeTransformer(
		    					new EdgeShape<VData<String>,String>(layoutInThisStep.getGraph()).new Line());
						vv1.repaint();
	    				IpeFileWriter.writeFile("target/drawings", "drawingAfterEdgeInsertion", 
	    						(AbstractLayout<VData<String>, String>) layoutInThisStep, null, true);
	    			}
	    		}
	    		else if (!dummyObjectsRemoved) {
	    			for (String edge : dummyEdges) {
	    				hs.getGraph().removeEdge(edge);
	    			}
	    			for (VData<String> vertex : dummyVertices) {
	    				hs.getGraph().removeVertex(vertex);
	    			}
	    			AbstractLayout<VData<String>, String> layoutInThisStep = hs.transformToFloatingPointLayout(drawingArea);
	    			vv1.setModel(new DefaultVisualizationModelWithoutReiterating<>(layoutInThisStep, drawingArea));
	    			vv1.getRenderContext().setEdgeShapeTransformer(
	    					new EdgeShape<VData<String>,String>(layoutInThisStep.getGraph()).new Line());
					vv1.repaint();
	    			dummyObjectsRemoved = true;
	    			//mark the crossings and bends - they shall not be drawn
	    			Set<VData<String>> doNotDraw = new HashSet<>();
	    			for (VData<String> v : layoutInThisStep.getGraph().getVertices()) {
	    				if (v.getVType() != VType.REGULAR) {
	    					doNotDraw.add(v);
	    				}
	    			}
    				IpeFileWriter.writeFile("target/drawings", "drawingAfterDummyRemoval", 
    						(AbstractLayout<VData<String>, String>) layoutInThisStep, doNotDraw, true);
	    		}
			}
	    });
		JPanel pane1 = new JPanel();
		pane1.setLayout(new BoxLayout(pane1, BoxLayout.Y_AXIS));
		pane1.add(reset1);
		pane1.add(stepButton);
		pane1.add(scrollPane1);
		frame.setLayout(new FlowLayout());
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		scrollPane1.setBorder(new TitledBorder(null, "Layout 1", TitledBorder.CENTER, TitledBorder.TOP));
		frame.getContentPane().add(pane1);
		frame.pack();
		frame.setVisible(true);
	}

}
