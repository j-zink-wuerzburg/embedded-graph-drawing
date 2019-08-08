package de.uniwue.informatik.main;

import de.uniwue.informatik.algorithms.layout.GridPoint;
import de.uniwue.informatik.algorithms.layout.PseudoComplexLayout;
import de.uniwue.informatik.algorithms.layout.VData;
import de.uniwue.informatik.algorithms.layout.VData.VType;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import de.uniwue.informatik.util.ExtendedEuclideanAlgorithm;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import agape.algos.MIS;

import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.map.HashedMap;

/**
 * 
 * @author Johannes
 *
 */
public class StackedTrianglesMain {
	
	private static double COMPARISON_TOLERANCE = 0.001;
	
	public static void main(String[] args) throws IOException {
//		findIndependentSetOnSpecialLines(Quadrants.NORTH_EAST, true);
		
		DrawGraphs.visualizeDrawing(drawStackedTriangleMultiGraph(15).transformToFloatingPointLayout());
	}
	
	enum Quadrants {
		NORTH_EAST,
		NORTH,
		EAST,
		ALL_DIRECTIONS;
	}
	
	public static PseudoComplexLayout<String, String> drawStackedTriangleMultiGraph(int n) {
		int xMin = 0;
		int xMax = 2;
		int yMin = -1;
		int yMax = 1;
		

		Set<GridPoint> maximalIndependentSet = null;
		
		while (maximalIndependentSet == null || maximalIndependentSet.size() < n) {
			UndirectedGraph<GridPoint, String> conflictGraph = computeConflictGraph(xMin, xMax, yMin, yMax, true);
			maximalIndependentSet = getGreedyMaximalIndependentSet(conflictGraph);
			
			xMax += 2;
			yMin -= 1;
			yMax += 1;
		}
		System.out.println("Found fitting independent set, it is:");
		System.out.println(maximalIndependentSet);
		LinkedList<GridPoint> crossingPoints = new LinkedList<>(maximalIndependentSet);
		Collections.sort(crossingPoints, new Comparator<GridPoint>() {
			@Override
			public int compare(GridPoint o1, GridPoint o2) {
				//compare slopes
				double o1Slope = 0;
				if (o1.getY() > 0 && o1.getX() == 0) {
					o1Slope = Double.POSITIVE_INFINITY;
				}
				else if (o1.getY() < 0 && o1.getX() == 0) {
					o1Slope = Double.NEGATIVE_INFINITY;
				}
				else if(o1.getX() != 0) {
					o1Slope = (double) o1.getY() / (double) o1.getX();
				}
				double o2Slope = 0;
				if (o2.getY() > 0 && o2.getX() == 0) {
					o2Slope = Double.POSITIVE_INFINITY;
				}
				else if (o2.getY() < 0 && o2.getX() == 0) {
					o2Slope = Double.NEGATIVE_INFINITY;
				}
				else if(o2.getX() != 0) {
					o2Slope = (double) o2.getY() / (double) o2.getX();
				}
				
				return Double.compare(o2Slope, o1Slope);
			}
		});
		
		UndirectedSparseGraph<VData<String>, String> stackedTrianglesGraph = new UndirectedSparseGraph<>();
		VData<String> v1 = new VData<String>("v1");
		VData<String> v2 = new VData<String>("v2");
		PseudoComplexLayout<String, String> gridLayout = new PseudoComplexLayout<>(stackedTrianglesGraph);
		
		stackedTrianglesGraph.addVertex(v1);
		gridLayout.setLocation(v1, 0, 0);
		stackedTrianglesGraph.addVertex(v2); //location of that will be fixed later
		Map<GridPoint, VData<String>> crossingPoint2Vertex = new HashedMap<>();
		
		int vertexCounter = 2;
		int edgeCounter = 0;
		Iterator<GridPoint> crossingPointIterator = crossingPoints.iterator();
		VData<String> prevBendPoint = null;
		for (int i = 0; i < n; ++i) {
			GridPoint crossingPoint = crossingPointIterator.next();
			VData<String> crossingVertex = new VData<>(VType.CROSSING_POINT);
			stackedTrianglesGraph.addVertex(crossingVertex);
			gridLayout.setLocation(crossingVertex, crossingPoint);
			crossingPoint2Vertex.put(crossingPoint, crossingVertex);
			int x = crossingPoint.getX();
			int y = crossingPoint.getY();
			int gcd = ExtendedEuclideanAlgorithm.gcd(x, y);
			int xLeft = x - y / gcd;
			int yLeft = y + x / gcd;
			VData<String> innerVertex = new VData<String>("v"+(vertexCounter++));
			stackedTrianglesGraph.addVertex(innerVertex);
			gridLayout.setLocation(innerVertex, xLeft, yLeft);
			int xRight = x + y / gcd;
			int yRight = y - x / gcd;
			VData<String> innerBendPoint = i < n-1 ? new VData<String>(VType.BEND_POINT) : new VData<String>("v"+(vertexCounter++));
			stackedTrianglesGraph.addVertex(innerBendPoint);
			gridLayout.setLocation(innerBendPoint, xRight, yRight);
			
			stackedTrianglesGraph.addEdge("e"+(edgeCounter++), v1, crossingVertex);
			stackedTrianglesGraph.addEdge("e"+(edgeCounter++), crossingVertex, innerVertex);
			stackedTrianglesGraph.addEdge("e"+(edgeCounter++), crossingVertex, innerBendPoint);
			if (prevBendPoint != null) {
				stackedTrianglesGraph.addEdge("e"+(edgeCounter++), prevBendPoint, innerVertex);
			}
			prevBendPoint = innerBendPoint;
		}
		//add top points so that they see the end point. For that finde the crossing points in the mid (close to the (1,0)-line)
		//for each top point further apart from the mid it must lie at oder behind the vertical line of the neighboring crossing point
		//closer to the mid
		int firstDownIndex = 0;
		crossingPointIterator = crossingPoints.iterator();
		while (crossingPointIterator.hasNext() && crossingPointIterator.next().getY() >= 0) {
			++firstDownIndex;
		}
		GridPoint lastUpperBendPoint = new GridPoint(xMax, 0);
		for (int i = firstDownIndex - 1; i >= 0; --i) {
			int x = crossingPoints.get(i).getX();
			int y = crossingPoints.get(i).getY();
			int gcd = ExtendedEuclideanAlgorithm.gcd(x, y);
			int xTop = x + x / gcd;
			int yTop = y + y / gcd;
			while ((double) yTop <= 0.5 * ((double) lastUpperBendPoint.getX() - (double) xTop) + (double) lastUpperBendPoint.getY()) {
				xTop += x / gcd;
				yTop += y / gcd;
			}
			lastUpperBendPoint = new GridPoint(xTop, yTop);
			VData<String> topBendPoint = new VData<String>(VType.BEND_POINT);
			stackedTrianglesGraph.addVertex(topBendPoint);
			gridLayout.setLocation(topBendPoint, xTop, yTop);
			stackedTrianglesGraph.addEdge("e"+(edgeCounter++), v2, topBendPoint);
			stackedTrianglesGraph.addEdge("e"+(edgeCounter++), crossingPoint2Vertex.get(crossingPoints.get(i)), topBendPoint);
		}
		GridPoint lastLowerBendPoint = new GridPoint(xMax, 0);
		for (int i = firstDownIndex; i < n; ++i) {
			int x = crossingPoints.get(i).getX();
			int y = crossingPoints.get(i).getY();
			int gcd = ExtendedEuclideanAlgorithm.gcd(x, y);
			int xTop = x + x / gcd;
			int yTop = y + y / gcd;
			while ((double) yTop >= 0.5 * ((double) xTop - (double) lastLowerBendPoint.getX()) + (double) lastLowerBendPoint.getY()) {
				xTop += x / gcd;
				yTop += y / gcd;
			}
			lastLowerBendPoint = new GridPoint(xTop, yTop);
			VData<String> topBendPoint = new VData<String>(VType.BEND_POINT);
			stackedTrianglesGraph.addVertex(topBendPoint);
			gridLayout.setLocation(topBendPoint, xTop, yTop);
			stackedTrianglesGraph.addEdge("e"+(edgeCounter++), v2, topBendPoint);
			stackedTrianglesGraph.addEdge("e"+(edgeCounter++), crossingPoint2Vertex.get(crossingPoints.get(i)), topBendPoint);
		}
		
		gridLayout.setLocation(v2, Math.max(lastUpperBendPoint.getX() + 2 * lastUpperBendPoint.getY(), 
				lastLowerBendPoint.getX() - 2 * lastLowerBendPoint.getY()), 0);
		
		return gridLayout;
	}
	
	/**
	 * Find Greedy Maximal Independent Set
	 * @param lengthenMainSegment 
	 */
	public static void findMaximalIndependentSetInConflictGraph(Quadrants gridRelativeToBasePoint, 
			boolean lengthenMainSegment) throws IOException {
				
		String csvFileName = gridRelativeToBasePoint+"-maximal_independent_sets_in_conflict_graph.csv";
		
		File csvFile = new File(csvFileName);
		if (!csvFile.exists()) {
			csvFile.createNewFile();
		}
		
		FileWriter csvFileWriter = new FileWriter(csvFileName, false);
		csvFileWriter.append("n;SizeOfGreedyMaximalIS;GreedyMaximalIS");
		csvFileWriter.close();

		
		for (int i = 1; i <= 1000; ++i) {
		
			int n = i; //number of neighbors of the central vertex
			int c = 1; //some constant
			int xMin;
			int xMax;
			int yMin;
			int yMax;
			
			if (gridRelativeToBasePoint == Quadrants.ALL_DIRECTIONS || gridRelativeToBasePoint == Quadrants.NORTH) {
				xMin = (-n * c) / 2;
				xMax = (n * c - 1) / 2;
			}
			else{
				xMin = 0;
				xMax = n * c - 1;
			}
			if (gridRelativeToBasePoint == Quadrants.ALL_DIRECTIONS || gridRelativeToBasePoint == Quadrants.EAST) {
				yMin = (-n * c) / 2;
				yMax = (n * c - 1) / 2;
			}
			else {
				yMin = 0;
				yMax = n * c - 1;
			}
			
			System.out.println(new Date()+": Construct conflict graph for grid with size "+(1 + xMax - xMin)+" x "+(1 + yMax - yMin));
			
			UndirectedGraph<GridPoint, String> conflictGraph = computeConflictGraph(xMin, xMax, yMin, yMax, lengthenMainSegment);
			
			Set<GridPoint> maximalIndependentSet = getGreedyMaximalIndependentSet(conflictGraph);
			
			System.out.println(new Date()+": Found greedy maximal Independent Set of conflict graph. It has size "+maximalIndependentSet.size()+" and is:");
			System.out.println(maximalIndependentSet);
			
			csvFileWriter = new FileWriter(csvFileName, true);
			csvFileWriter.append(System.lineSeparator() + (xMax-xMin+1) + ";" + maximalIndependentSet.size() + ";" + maximalIndependentSet);
			csvFileWriter.close();
		}
	}
	
	
	/**
	 * Find Maximum Independent Set
	 * @param lengthenMainSegment 
	 */
	public static void findMaximumIndependentSetInConflictGraph(Quadrants gridRelativeToBasePoint, 
			boolean lengthenMainSegment) throws IOException {
		
		String csvFileName = gridRelativeToBasePoint+"-maximum_independent_sets_in_conflict_graph.csv";
		
		File csvFile = new File(csvFileName);
		if (!csvFile.exists()) {
			csvFile.createNewFile();
		}
		
		FileWriter csvFileWriter = new FileWriter(csvFileName, false);
		csvFileWriter.append("n;SizeOfMaximumIS;MaximumIS");
		csvFileWriter.close();

		
		for (int i = 1; i <= 1000; ++i) {
		
			int n = i; //number of neighbors of the central vertex
			int c = 1; //some constant
			int xMin;
			int xMax;
			int yMin;
			int yMax;
			
			if (gridRelativeToBasePoint == Quadrants.ALL_DIRECTIONS || gridRelativeToBasePoint == Quadrants.NORTH) {
				xMin = (-n * c) / 2;
				xMax = (n * c - 1) / 2;
			}
			else{
				xMin = 0;
				xMax = n * c - 1;
			}
			if (gridRelativeToBasePoint == Quadrants.ALL_DIRECTIONS || gridRelativeToBasePoint == Quadrants.EAST) {
				yMin = (-n * c) / 2;
				yMax = (n * c - 1) / 2;
			}
			else {
				yMin = 0;
				yMax = n * c - 1;
			}
			
			System.out.println(new Date()+": Construct conflict graph for grid with size "+(1 + xMax - xMin)+" x "+(1 + yMax - yMin));
			
			UndirectedGraph<GridPoint, String> conflictGraph = computeConflictGraph(xMin, xMax, yMin, yMax, lengthenMainSegment);
			
			Set<GridPoint> maximumIndependentSet = getMaximumIndependentSet(conflictGraph);
			
			System.out.println(new Date()+": Found Maximum Independent Set of conflict graph. It has size "+maximumIndependentSet.size()+" and is:");
			System.out.println(maximumIndependentSet);
			
			csvFileWriter = new FileWriter(csvFileName, true);
			csvFileWriter.append(System.lineSeparator() + (xMax-xMin+1) + ";" + maximumIndependentSet.size() + ";" + maximumIndependentSet);
			csvFileWriter.close();
		}
	}
	
	/**
	 * Special construction scheme:
	 * Use only the first quadrant and there start with using only the vertical and horizontal line and then
	 * recursively the lines in between
	 * @param lengthenMainSegment 
	 */
	public static void findIndependentSetOnSpecialLines(Quadrants gridRelativeToBasePoint, 
			boolean lengthenMainSegment) throws IOException {
		
		String csvFileName = gridRelativeToBasePoint+"-maximum_independent_sets_on_special_lines.csv";
		
		File csvFile = new File(csvFileName);
		if (!csvFile.exists()) {
			csvFile.createNewFile();
		}
		
		FileWriter csvFileWriter = new FileWriter(csvFileName, false);
		csvFileWriter.append("n;SizeOfMaximumIS;MaximumIS");
		csvFileWriter.close();
		
		Set<GridPoint> maximumIndependentSet = null;
		
		int n = 1; //basic size of grid
		
		for (int i = 1; i <= 1000; ++i) { //i number of vertices in independent set
			
			while (maximumIndependentSet == null || maximumIndependentSet.size() < i) {
				
				int c = 1; //some constant
				int xMin;
				int xMax;
				int yMin;
				int yMax;
				
				if (gridRelativeToBasePoint == Quadrants.ALL_DIRECTIONS || gridRelativeToBasePoint == Quadrants.NORTH) {
					xMin = (-n * c) / 2;
					xMax = (n * c - 1) / 2;
				}
				else{
					xMin = 0;
					xMax = n * c - 1;
				}
				if (gridRelativeToBasePoint == Quadrants.ALL_DIRECTIONS || gridRelativeToBasePoint == Quadrants.EAST) {
					yMin = (-n * c) / 2;
					yMax = (n * c - 1) / 2;
				}
				else {
					yMin = 0;
					yMax = n * c - 1;
				}
				
				System.out.println(new Date()+": Construct conflict graph for grid with size "+(1 + xMax - xMin)+" x "+(1 + yMax - yMin));
				
				UndirectedGraph<GridPoint, String> conflictGraph = computeConflictGraph(xMin, xMax, yMin, yMax, lengthenMainSegment);
				
				LinkedList<GridPoint> validGridPointBases = new LinkedList<>();
				validGridPointBases.add(new GridPoint(0, 1));
				validGridPointBases.add(new GridPoint(1, 0));
				
				while(validGridPointBases.size() < i) {
					Iterator<GridPoint> iter = new ArrayList<GridPoint>(validGridPointBases).iterator();
					GridPoint prev = null;
					GridPoint curr = iter.next();
					for (int j = 1; iter.hasNext(); ++j) {
						prev = curr;
						curr = iter.next();
						GridPoint newPoint = new GridPoint(prev.getX() + curr.getX(), prev.getY() + curr.getY());
						validGridPointBases.add(j, newPoint);
						++j;
					}
				}
				
				//Remove vertices that are no multiples of the points in validGridPointBases
				int removeCounter = 0;
				for (GridPoint vertex : new ArrayList<>(conflictGraph.getVertices())) {
					int gcd = ExtendedEuclideanAlgorithm.gcd(vertex.getX(), vertex.getY());
					GridPoint pointReduced = new GridPoint(vertex.getX() / gcd, vertex.getY() / gcd);
					if (!validGridPointBases.contains(pointReduced)){
						++removeCounter;
						conflictGraph.removeVertex(vertex);
					}
				}
				
				System.out.println(new Date()+": Removed "+removeCounter+" vertices from conflict graph");
				System.out.println("Conflict graph has now "+conflictGraph.getVertexCount()+" vertices and "+conflictGraph.getEdgeCount()+" edges");
				int degreeSum = 0;
				int minDegree = Integer.MAX_VALUE;
				int maxDegree = 0;
				for (GridPoint v : conflictGraph.getVertices()) {
					int degreeOfV = conflictGraph.degree(v);
					degreeSum += degreeOfV;
					minDegree = Math.min(minDegree, degreeOfV);
					maxDegree = Math.max(maxDegree, degreeOfV);
				}
				double avgDegree = (double)degreeSum / (double)conflictGraph.getVertexCount();
				System.out.println("Average degree of conflict graph is "+avgDegree+", min is "+minDegree+", max is "+maxDegree);
				
				maximumIndependentSet = getMaximumIndependentSet(conflictGraph);
				
				System.out.println(new Date()+": Found maximum Independent Set of conflict graph. It has size "+maximumIndependentSet.size()+" and is:");
				System.out.println(maximumIndependentSet);
				
				csvFileWriter = new FileWriter(csvFileName, true);
				csvFileWriter.append(System.lineSeparator() + (xMax-xMin+1) + ";" + maximumIndependentSet.size() + ";" + maximumIndependentSet);
				csvFileWriter.close();
				
				++n;
			}
		}
	}

	private static Set<GridPoint> getGreedyMaximalIndependentSet(UndirectedGraph<GridPoint, String> graph) {
		MIS<GridPoint, String> mis = new MIS<GridPoint, String>(new Factory<Graph<GridPoint, String>>() {
			@Override
			public Graph<GridPoint, String> create() {
				return new UndirectedSparseGraph<>();
			}
		}, new Factory<GridPoint>() {
			@Override
			public GridPoint create() {
				return new GridPoint(0, 0);
			}
		}, new Factory<String>() {
			@Override
			public String create() {
				return "";
			}
		});
		Set<GridPoint> maximalIndependentSet = mis.maximalIndependentSetGreedy(graph);
		return maximalIndependentSet;
	}
	
	private static Set<GridPoint> getMaximumIndependentSet(UndirectedGraph<GridPoint, String> graph) {
		MIS<GridPoint, String> mis = new MIS<GridPoint, String>(new Factory<Graph<GridPoint, String>>() {
			@Override
			public Graph<GridPoint, String> create() {
				return new UndirectedSparseGraph<>();
			}
		}, new Factory<GridPoint>() {
			@Override
			public GridPoint create() {
				return new GridPoint(0, 0);
			}
		}, new Factory<String>() {
			@Override
			public String create() {
				return "";
			}
		});
		Set<GridPoint> maximumIndependentSet = mis.maximumIndependentSetMoonMoser(graph);
		return maximumIndependentSet;
	}

	private static UndirectedGraph<GridPoint, String> computeConflictGraph(int xMin, int xMax, int yMin, int yMax, 
			boolean lengthenMainSegment) {
		//construct conflict graph
		UndirectedGraph<GridPoint, String> conflictGraph = new UndirectedSparseGraph<>();
		
		for (int x = xMin; x <= xMax; ++x) {
			for (int y = yMin; y <= yMax; ++y) {
				if (x != 0 || y != 0) {
					conflictGraph.addVertex(new GridPoint(x, y));
				}
			}
		}
		
		//find all conflicting pairs of grid points
		int counterConflicts = 0;
		int counterNoConflicts = 0;
		for (int x0 = xMin; x0 <= xMax; ++x0) {
			for (int y0 = yMin; y0 <= yMax; ++y0) {
				if (x0 != 0 || y0 != 0) {
					GridPoint gridPoint0 = new GridPoint(x0, y0);
					for (int x1 = xMin; x1 <= xMax; ++x1) {
						for (int y1 = yMin; y1 <= yMax; ++y1) {
							if (x0 < x1 || (x0 == x1 && y0 < y1)) {
								if (x1 != 0 || y1 != 0) {
									GridPoint gridPoint1 = new GridPoint(x1, y1);
									if (hasConflict(gridPoint0, gridPoint1, Math.max(xMax-yMin+1, yMax-yMin+1), lengthenMainSegment)) {
										conflictGraph.addEdge("("+x0+","+y0+") conflicts ("+x1+","+y1+")", 
												gridPoint0, gridPoint1);
										++counterConflicts;
//										System.out.println("("+x0+", "+y0+") - ("+x1+", "+y1+") CONFLICT");
									}
									else {
										++counterNoConflicts;
//										System.out.println("("+x0+", "+y0+") - ("+x1+", "+y1+") no conflict");
									}
								}
							}
						}
					}
				}
			}
		}
		System.out.println(new Date()+": Found "+counterConflicts+" times a conflict and "+counterNoConflicts+" times no conflict");
		System.out.println("Conflict graph has "+conflictGraph.getVertexCount()+" vertices and "+conflictGraph.getEdgeCount()+" edges");
		int degreeSum = 0;
		int minDegree = Integer.MAX_VALUE;
		int maxDegree = 0;
		for (GridPoint v : conflictGraph.getVertices()) {
			int degreeOfV = conflictGraph.degree(v);
			degreeSum += degreeOfV;
			minDegree = Math.min(minDegree, degreeOfV);
			maxDegree = Math.max(maxDegree, degreeOfV);
		}
		double avgDegree = (double)degreeSum / (double)conflictGraph.getVertexCount();
		System.out.println("Average degree of conflict graph is "+avgDegree+", min is "+minDegree+", max is "+maxDegree);
		return conflictGraph;
	}
	
	/**
	 * 
	 * @param gridPoint0
	 * @param gridPoint1
	 * @param gridLength
	 * @param lengthenMainSegment
	 * 		if false, then main segment is made as short as possible that means the top point is moved as close as possible to the
	 * 		passed central point (the crossing point).
	 * 		if true, then main segment is made long enough so that every other segment passing/touching the half line defined by 
	 * 		the passed point will cause a conflict
	 * @return
	 */
	private static boolean hasConflict(GridPoint gridPoint0, GridPoint gridPoint1, int gridLength, boolean lengthenMainSegment) {
		int x0 = gridPoint0.getX();
		int y0 = gridPoint0.getY();
		int x1 = gridPoint1.getX();
		int y1 = gridPoint1.getY();

		int gcdX = ExtendedEuclideanAlgorithm.gcd(x0, x1);
		int gcdY = ExtendedEuclideanAlgorithm.gcd(y0, y1);
		
		//Check 1: gridPoint0 and gridPoint1 on the same half-line from the origin (multiples of each other)
		if (Math.signum(x0) == Math.signum(x1) && Math.signum(y0) == Math.signum(y1) 
				&& (gcdX == 0 || gcdY == 0 || (Math.abs(x0) / gcdX == Math.abs(y0) / gcdY && Math.abs(x1) / gcdX == Math.abs(y1) / gcdY))) {
			return true;
		}
		
		/*
		 * relative to the point (x, y) [crossing point / mid-point] 
		 * we can assign 3 other grid points belonging to that (x, y).
		 * It is:
		 *  - (xTop, yTop) [top (bend) point]
		 *  - (xLeft, yLeft) [left (bend) point]
		 *  - (xRight, yRight) [right (bend) point]
		 */
		int gcd0 = ExtendedEuclideanAlgorithm.gcd(x0, y0);
		int gcd1 = ExtendedEuclideanAlgorithm.gcd(x1, y1);
		
		int x0Top = lengthenMainSegment ? x0 + (int) ((double) x0 / (double)(Math.abs(x0) + Math.abs(y0)) * 4.0 * (double) gridLength) 
				: x0 + x0 / gcd0;
		int y0Top = lengthenMainSegment ? y0 + (int) ((double) y0 / (double)(Math.abs(x0) + Math.abs(y0)) * 4.0 * (double) gridLength) 
				: y0 + y0 / gcd0;
		int x0Left = x0 - y0 / gcd0;
		int y0Left = y0 + x0 / gcd0;
		int x0Right = x0 + y0 / gcd0;
		int y0Right = y0 - x0 / gcd0;

		int x1Top = lengthenMainSegment ? x1 + (int) ((double) x1 / (double)(Math.abs(x1) + Math.abs(y1)) * 4.0 * (double) gridLength) 
				: x1 + x1 / gcd1;
		int y1Top = lengthenMainSegment ? y1 + (int) ((double) y1 / (double)(Math.abs(x1) + Math.abs(y1)) * 4.0 * (double) gridLength) 
				: y1 + y1 / gcd1;
		int x1Left = x1 - y1 / gcd1;
		int y1Left = y1 + x1 / gcd1;
		int x1Right = x1 + y1 / gcd1;
		int y1Right = y1 - x1 / gcd1;
		
		//Check 2: Primary line segment (origin to top point) of gp0 touches secondary line segment (left to right) of gp1
		if (touches(0, 0, x0Top, y0Top, x1Left, y1Left, x1Right, y1Right)) {
			return true;
		}
		
		//Check 3: Primary line segment (origin to top point) of gp1 touches secondary line segment (left to right) of gp0
		if (touches(x0Left, y0Left, x0Right, y0Right, 0, 0, x1Top, y1Top)) {
			return true;
		}
		
		//Check 4: Both secondary line segments touch each other
		if (touches(x0Left, y0Left, x0Right, y0Right, x1Left, y1Left, x1Right, y1Right)) {
			return true;
		}
		
		return false;
	}

	/*
	 * with type double
	 */
	
	private static boolean touches(double xLine0Point0, double yLine0Point0, double xLine0Point1, double yLine0Point1, 
			double xLine1Point0, double yLine1Point0, double xLine1Point1, double yLine1Point1) {
		//Uses Cramer's rule to find the intersection point of the 2 lines defined by those 4 points
		//see: https://de.wikipedia.org/wiki/Schnittpunkt
		//Note: Parallelness is currently not checked
		double xIntersection = ((xLine1Point1 - xLine1Point0) * (xLine0Point1 * yLine0Point0 - xLine0Point0 * yLine0Point1) 
				- (xLine0Point1 - xLine0Point0) * (xLine1Point1 * yLine1Point0 - xLine1Point0 * yLine1Point1))
				/ ((yLine1Point1 - yLine1Point0) * (xLine0Point1 - xLine0Point0) 
						- (yLine0Point1 - yLine0Point0) * (xLine1Point1 - xLine1Point0));
		double yIntersection = ((yLine0Point0 - yLine0Point1) * (xLine1Point1 * yLine1Point0 - xLine1Point0 * yLine1Point1) 
				- (yLine1Point0 - yLine1Point1) * (xLine0Point1 * yLine0Point0 - xLine0Point0 * yLine0Point1))
				/ ((yLine1Point1 - yLine1Point0) * (xLine0Point1 - xLine0Point0) 
						- (yLine0Point1 - yLine0Point0) * (xLine1Point1 - xLine1Point0));
		
		//it touches only if the intersection point is in between the bounding points of the line segments
		if (isSmallerOrEqual(Math.min(xLine0Point0, xLine0Point1), xIntersection) 
				&& isSmallerOrEqual(xIntersection, Math.max(xLine0Point0, xLine0Point1))
				&& isSmallerOrEqual(Math.min(yLine0Point0, yLine0Point1), yIntersection) 
				&& isSmallerOrEqual(yIntersection, Math.max(yLine0Point0, yLine0Point1))
				&& isSmallerOrEqual(Math.min(xLine1Point0, xLine1Point1), xIntersection) 
				&& isSmallerOrEqual(xIntersection, Math.max(xLine1Point0, xLine1Point1))
				&& isSmallerOrEqual(Math.min(yLine1Point0, yLine1Point1), yIntersection)
				&& isSmallerOrEqual(yIntersection, Math.max(yLine1Point0, yLine1Point1))) {
			return true;
		}
		return false;
	}
	

	private static boolean isSmallerOrEqual(double x0, double x1) {
		if (equals(x0, x1)) {
			return true;
		}
		if (x0 < x1) {
			return true;
		}
		return false;
	}
	
	private static boolean equals(double x0, double x1) {
		if (Math.abs(x0-x1) <= COMPARISON_TOLERANCE) {
			return true;
		}
		return false;
	}
	
	/*
	 * with type BigDecimal
	 */
	
//	private static boolean touches(double xLine0Point0Double, double yLine0Point0Double, double xLine0Point1Double, double yLine0Point1Double, 
//			double xLine1Point0Double, double yLine1Point0Double, double xLine1Point1Double, double yLine1Point1Double) {
//		BigDecimal xLine0Point0 = new BigDecimal(xLine0Point0Double);
//		BigDecimal yLine0Point0 = new BigDecimal(yLine0Point0Double);
//		BigDecimal xLine0Point1 = new BigDecimal(xLine0Point1Double);
//		BigDecimal yLine0Point1 = new BigDecimal(yLine0Point1Double);
//		BigDecimal xLine1Point0 = new BigDecimal(xLine1Point0Double);
//		BigDecimal yLine1Point0 = new BigDecimal(yLine1Point0Double);
//		BigDecimal xLine1Point1 = new BigDecimal(xLine1Point1Double);
//		BigDecimal yLine1Point1 = new BigDecimal(yLine1Point1Double);
//		//Uses Cramer's rule to find the intersection point of the 2 lines defined by those 4 points
//		//see: https://de.wikipedia.org/wiki/Schnittpunkt
//		//Note: Parallelness is currently not checked
//		if (((yLine1Point1.subtract(yLine1Point0)).multiply((xLine0Point1.subtract(xLine0Point0)))
//				.subtract((yLine0Point1.subtract(yLine0Point0)).multiply((xLine1Point1.subtract(xLine1Point0)))))
//				.compareTo(new BigDecimal(0)) == 0 ||
//				((yLine1Point1.subtract(yLine1Point0)).multiply((xLine0Point1.subtract(xLine0Point0)))
//				.subtract((yLine0Point1.subtract(yLine0Point0)).multiply((xLine1Point1.subtract(xLine1Point0)))))
//				.compareTo(new BigDecimal(0)) == 0) {
//				/*
//				 * if one divisor equals 0, then the two lines are parallel. 
//				 * Assume here that the segments are not on the same line -> no conflict
//				 */
//			return false;
//		}
//		BigDecimal xIntersection = ((xLine1Point1.subtract(xLine1Point0)).multiply((xLine0Point1.multiply(yLine0Point0).subtract(xLine0Point0.multiply(yLine0Point1)))) 
//					.subtract((xLine0Point1.subtract(xLine0Point0)).multiply((xLine1Point1.multiply(yLine1Point0).subtract(xLine1Point0.multiply(yLine1Point1))))))
//					.divide(((yLine1Point1.subtract(yLine1Point0)).multiply((xLine0Point1.subtract(xLine0Point0))) 
//					.subtract((yLine0Point1.subtract(yLine0Point0)).multiply((xLine1Point1.subtract(xLine1Point0))))), 20, RoundingMode.HALF_EVEN);
//		BigDecimal yIntersection = ((yLine0Point0.subtract(yLine0Point1)).multiply((xLine1Point1.multiply(yLine1Point0).subtract(xLine1Point0.multiply(yLine1Point1)))) 
//					.subtract((yLine1Point0.subtract(yLine1Point1)).multiply((xLine0Point1.multiply(yLine0Point0).subtract(xLine0Point0.multiply(yLine0Point1))))))
//					.divide(((yLine1Point1.subtract(yLine1Point0)).multiply((xLine0Point1.subtract(xLine0Point0))) 
//					.subtract((yLine0Point1.subtract(yLine0Point0)).multiply((xLine1Point1.subtract(xLine1Point0))))), 20, RoundingMode.HALF_EVEN);
//		
//		//it touches only if the intersection point is in between the bounding points of the line segments
//		if (isSmallerOrEqual(xLine0Point0.min(xLine0Point1), xIntersection) 
//				&& isSmallerOrEqual(xIntersection, xLine0Point0.max(xLine0Point1))
//				&& isSmallerOrEqual(yLine0Point0.min(yLine0Point1), yIntersection) 
//				&& isSmallerOrEqual(yIntersection, yLine0Point0.max(yLine0Point1))
//				&& isSmallerOrEqual(xLine1Point0.min(xLine1Point1), xIntersection) 
//				&& isSmallerOrEqual(xIntersection, xLine1Point0.max(xLine1Point1))
//				&& isSmallerOrEqual(yLine1Point0.min(yLine1Point1), yIntersection)
//				&& isSmallerOrEqual(yIntersection, yLine1Point0.max(yLine1Point1))) {
//			return true;
//		}
//		return false;
//	}
//	
//
//	private static boolean isSmallerOrEqual(BigDecimal x0, BigDecimal x1) {
//		if (equals(x0, x1)) {
//			return true;
//		}
//		if (x0.compareTo(x1) < 0) {
//			return true;
//		}
//		return false;
//	}
//	
//	private static boolean equals(BigDecimal x0, BigDecimal x1) {
//		if (x0.subtract(x1).abs().compareTo(new BigDecimal(COMPARISON_TOLERANCE)) <= 0) {
//			return true;
//		}
//		return false;
//	}
	
}
