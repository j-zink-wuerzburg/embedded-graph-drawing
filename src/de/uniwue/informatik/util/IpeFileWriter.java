package de.uniwue.informatik.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.graph.util.Pair;


public class IpeFileWriter {
	
	/** 
	 * @param pathWithoutFilename
	 * @param fileName (endingless)
	 * Ohne .ipe Ending is added automatically
	 * @param layout
	 */
	public static <V, E> void writeFile(String pathWithoutFilename, String fileName, AbstractLayout<V, E> layout){
		writeFile(pathWithoutFilename, fileName, layout, null);
	}
	
	/** 
	 * @param pathWithoutFilename
	 * @param fileName (endingless)
	 * Ohne .ipe Ending is added automatically
	 * @param layout
	 * @param doNotDrawVertex these vertices are not drawn, may be null then every vertex is drawn
	 */
	public static <V, E> void writeFile(String pathWithoutFilename, String fileName, AbstractLayout<V, E> layout, 
			Collection<V> doNotDrawVertex){
		writeFile(pathWithoutFilename, fileName, layout, doNotDrawVertex, false);
	}
		
	/** 
	 * @param pathWithoutFilename
	 * @param fileName (endingless)
	 * Ohne .ipe Ending is added automatically
	 * @param layout
	 * @param doNotDrawVertex these vertices are not drawn, may be null then every vertex is drawn
	 */
	public static <V, E> void writeFile(String pathWithoutFilename, String fileName, AbstractLayout<V, E> layout, 
			Collection<V> doNotDrawVertex, boolean addGrid){
		try {
			String filePath = pathWithoutFilename+File.separator+fileName+".ipe";
			File targetFile = new File(filePath);
			if (!targetFile.exists()) {
				targetFile.getParentFile().mkdirs();
				targetFile.createNewFile();
			}
			FileWriter fw = new FileWriter(filePath, false);
			
			fw.append(IpeDraw.getIpePreamble());
			fw.append(IpeDraw.getIpeConf());
			
			//Values for Scaling from the unknown Dimension of layout to 1000
			double xSkal = 1000/layout.getSize().getWidth();
			double ySkal = 1000/layout.getSize().getHeight();
			
			if (addGrid) {
				//find extreme values
				double xMin = Double.POSITIVE_INFINITY, xMax = Double.NEGATIVE_INFINITY;
				double yMin = Double.POSITIVE_INFINITY, yMax = Double.NEGATIVE_INFINITY;
				for(V v: layout.getGraph().getVertices()){
					xMin = Math.min(xMin, layout.getX(v));
					xMax = Math.max(xMax, layout.getX(v));
					yMin = Math.min(yMin, layout.getY(v));
					yMax = Math.max(yMax, layout.getY(v));
				}
				//add grid lines
				for (int x = (int) Math.floor(xMin); x <= Math.ceil(xMax); x++) {
					fw.append(drawIpePath(new double[]{x*xSkal, x*xSkal}, 
							new double[]{(Math.floor(yMin)-0.5)*ySkal, (Math.ceil(yMax)+0.5)*ySkal}, "lightgray", "normal", "normal"));
				}
				for (int y = (int) Math.floor(yMin); y <= Math.ceil(yMax); y++) {
					fw.append(drawIpePath(new double[]{(Math.floor(xMin)-0.5)*xSkal, (Math.ceil(xMax)+0.5)*xSkal}, 
							new double[]{y*ySkal, y*ySkal}, "lightgray", "normal", "normal"));
				}
			}
			
			for(V v: layout.getGraph().getVertices()){
				if (doNotDrawVertex == null || !doNotDrawVertex.contains(v)) {
					fw.append(drawIpeMark(layout.getX(v)*xSkal, layout.getY(v)*ySkal));
				}
			}
			for(E e: layout.getGraph().getEdges()){
				Pair<V> endpoints = layout.getGraph().getEndpoints(e);
				V v1 = endpoints.getFirst();
				V v2 = endpoints.getSecond();
				
				fw.append(drawIpeEdge(layout.getX(v1)*xSkal, layout.getY(v1)*ySkal,
						layout.getX(v2)*xSkal, layout.getY(v2)*ySkal));
			}
			
			fw.append(IpeDraw.getIpeEnd());
			fw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
	
	
	
	/*
	 * The following methods are edits from IpeDraw:
	 * 
	 * There it is only possible to pass an Int-Value for the locations of the vertices and edges.
	 * This was changed so that double-values can be passed and used.
	 */
	
	
	/**
	 * Draws a mark of shape "disk" with color "black" and size "normal".
	 * 
	 * @param x
	 *            x-coordinate
	 * @param y
	 *            y-coordinate
	 * @return
	 */
	public static String drawIpeMark(double x, double y) {
		return drawIpeMark(x, y, "disk", "black", "normal");
	}
	/**
	 * Draws a mark.
	 * 
	 * @param x
	 *            x-coordinate
	 * @param y
	 *            y-coordinate
	 * @param shape
	 * 			  shape: disk, fdisk, circle, box, square, fsquare, cross
	 * @param color
	 *            color
	 * @param size
	 *            size: tiny, small, normal, large
	 * @return
	 */
	public static String drawIpeMark(double x, double y, String shape, String color, String size) {
		return "<use name=\"mark/" + shape + "(sx)\" pos=\"" + x + " " + y
				+ "\" size=\"" + size + "\" stroke=\"" + color + "\"/>\n";
	}
	
	/**
	 * Draws an undashed edge between two points with pen width "normal" and
	 * color "black".
	 * 
	 * @param x1
	 *            x-coordinate of point 1
	 * @param y1
	 *            y-coordinate of point 1
	 * @param x2
	 *            x-coordinate of point 2
	 * @param y2
	 *            y-coordinate of point 2
	 * @return
	 */
	public static String drawIpeEdge(double x1, double y1, double x2, double y2) {
		return drawIpePath(new double[] { x1, x2 }, new double[] { y1, y2 });
	}
	
	/**
	 * Draws an undashed path between points with pen width "normal" and color
	 * "black".
	 * 
	 * @param x
	 *            x-coordinates of the points
	 * @param y
	 *            y-coordinates of the points
	 * @return
	 */
	public static String drawIpePath(double[] x, double[] y) {
		return drawIpePath(x, y, "black", "normal", "normal");
	}
	/**
	 * Draws a path between points.
	 * 
	 * @param x
	 *            x-coordinates of the points
	 * @param y
	 *            y-coordinates of the points
	 * @param color
	 *            color
	 * @param pen
	 *            pen width: normal, heavier, fat, ultrafat
	 * @param dash
	 *            dash style: normal, dashed, dotted, dash dotted, dash dot
	 *            dotted
	 * @return
	 */
	public static String drawIpePath(double[] x, double[] y, String color,
			String pen, String dash) {
		String s = "<path stroke=\"" + color + "\" pen=\"" + pen + "\" dash=\""
				+ dash + "\">\n " + x[0] + " " + y[0] + " m\n ";
		for (int i = 1; i < x.length; i++) {
			s += x[i] + " " + y[i] + " l\n ";
		}
		s += "</path>\n";
		return s;
	}
}
