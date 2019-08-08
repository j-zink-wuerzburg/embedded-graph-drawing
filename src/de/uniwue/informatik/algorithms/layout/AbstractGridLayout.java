package de.uniwue.informatik.algorithms.layout;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;
import de.uniwue.informatik.graph.embedding.EmbeddedUndirectedGraph;
import de.uniwue.informatik.util.SerialUtils;

/**
 * Adaption of {@link Layout}. Only changes:
 * <ul>
 *   <li> {@link GridPoint} instead of (floating-point-value based) {@link Point2D}
 *   <li> Removal of all {@link Dimension} (size) related things (no size-variable maintained)
 *   <li> {@link GridLayout} instead of {@link Layout}.
 * </ul>
 * <br>
 * 
 * @param <V> the vertex type
 * @param <E> the edge type
 */
abstract public class AbstractGridLayout<V, E> implements GridLayout<V,E> {

    /**
     * A set of vertices that are fixed in place and not affected by the layout algorithm
     */
	private Set<V> dontmove = new HashSet<V>();

	protected Graph<V, E> graph;
	protected boolean initialized;

    protected LoadingCache<V, GridPoint> locations =
    	CacheBuilder.newBuilder().build(new CacheLoader<V, GridPoint>() {
	    	public GridPoint load(V vertex) {
	    		return new GridPoint(0, 0);
	    	}
    });
    
    protected LoadingCache<V, Boolean> visibilityVertices =
    	CacheBuilder.newBuilder().build(new CacheLoader<V, Boolean>() {
	    	public Boolean load(V vertex) {
	    		return true;
	    	}
    });

    protected LoadingCache<E, Boolean> visibilityEdges =
    	CacheBuilder.newBuilder().build(new CacheLoader<E, Boolean>() {
	    	public Boolean load(E edge) {
	    		return true;
	    	}
    });
    
	/**
	 * Creates an instance for {@code graph} which does not initialize the vertex locations.
	 * 
	 * @param graph the graph on which the layout algorithm is to operate
	 */
	protected AbstractGridLayout(Graph<V, E> graph) {
	    if (graph == null) 
	    {
	        throw new IllegalArgumentException("Graph must be non-null");
	    }
		this.graph = graph;
	}
	
	/**
	 * Creates an instance for {@code graph} which initializes the vertex locations
	 * using {@code initializer}.
	 * 
	 * @param graph the graph on which the layout algorithm is to operate
	 * @param initializer specifies the starting positions of the vertices
	 */
    protected AbstractGridLayout(Graph<V,E> graph, Function<V,GridPoint> initializer) {
		this.graph = graph;
		Function<V, GridPoint> chain = 
			Functions.<V,GridPoint,GridPoint>compose(
					new Function<GridPoint,GridPoint>(){
						public GridPoint apply(GridPoint p) {
							return (GridPoint)p.clone();
						}}, 
					initializer
					);
		this.locations = CacheBuilder.newBuilder().build(CacheLoader.from(chain)); 
		initialized = true;
	}
    
    public void setGraph(Graph<V,E> graph) {
        this.graph = graph;
        if(graph != null) {
        	initialize();
        }
    }
    
    public boolean isLocked(V v) {
        return dontmove.contains(v);
    }
    
    public void setInitializer(Function<V,GridPoint> initializer) {
    	if(this.equals(initializer)) {
    		throw new IllegalArgumentException("Layout cannot be initialized with itself");
    	}
		Function<V, GridPoint> chain = 
			Functions.<V,GridPoint,GridPoint>compose(
					new Function<GridPoint,GridPoint>(){
						public GridPoint apply(GridPoint p) {
							return (GridPoint)p.clone();
						}}, 
					initializer
					);
		this.locations = CacheBuilder.newBuilder().build(CacheLoader.from(chain)); 
    	initialized = true;
    }

	/**
	 * Returns the Coordinates object that stores the vertex' x and y location.
	 * 
	 * @param v
	 *            A Vertex that is a part of the Graph being visualized.
	 * @return A Coordinates object with x and y locations.
	 */
	private GridPoint getCoordinates(V v) {
        return locations.getUnchecked(v);
	}
	
	public GridPoint apply(V v) {
		return getCoordinates(v);
	}
	
	/**
	 * Returns the x coordinate of the vertex from the Coordinates object.
	 * in most cases you will be better off calling transform(v).
	 * 
	 * @param v the vertex whose x coordinate is to be returned
	 * @return the x coordinate of {@code v}
	 */
	public int getX(V v) {
        Preconditions.checkNotNull(getCoordinates(v), "Cannot getX for an unmapped vertex "+v);
        return getCoordinates(v).getX();
	}

	/**
	 * Returns the y coordinate of the vertex from the Coordinates object.
	 * In most cases you will be better off calling transform(v).
	 * 
	 * @param v the vertex whose y coordinate is to be returned
	 * @return the y coordinate of {@code v}
	 */
	public int getY(V v) {
        Preconditions.checkNotNull(getCoordinates(v), "Cannot getY for an unmapped vertex "+v);
        return getCoordinates(v).getY();
	}
	
	/**
	 * @param v the vertex whose coordinates are to be offset
	 * @param xOffset the change to apply to this vertex's x coordinate
	 * @param yOffset the change to apply to this vertex's y coordinate
	 */
	protected void offsetVertex(V v, int xOffset, int yOffset) {
		GridPoint c = getCoordinates(v);
        c.setLocation(c.getX()+xOffset, c.getY()+yOffset);
		setLocation(v, c);
	}

	/**
	 * @return the graph that this layout operates on
	 */
	public Graph<V, E> getGraph() {
	    return graph;
	}
	
	/**
	 * Forcibly moves a vertex to the (x,y) location by setting its x and y
	 * locations to the specified location. Does not add the vertex to the
	 * "dontmove" list, and (in the default implementation) does not make any
	 * adjustments to the rest of the graph.
	 * @param picked the vertex whose location is being set
	 * @param x the x coordinate of the location to set
	 * @param y the y coordinate of the location to set
	 */
	public void setLocation(V picked, int x, int y) {
		GridPoint coord = getCoordinates(picked);
		coord.setLocation(x, y);
	}

	public void setLocation(V picked, GridPoint p) {
		GridPoint coord = getCoordinates(picked);
		coord.setLocation(p);
	}

	/**
	 * Locks {@code v} in place if {@code state} is {@code true}, otherwise unlocks it.
	 * @param v the vertex whose position is to be (un)locked
	 * @param state {@code true} if the vertex is to be locked, {@code false} if to be unlocked
	 */
	public void lock(V v, boolean state) {
		if(state == true) 
		    dontmove.add(v);
		else 
		    dontmove.remove(v);
	}
	
	/**
	 * @param lock {@code true} to lock all vertices in place, {@code false} to unlock all vertices
	 */
	public void lock(boolean lock) {
		for(V v : graph.getVertices()) {
			lock(v, lock);
		}
	}
	
	public AbstractLayout<V, E> transformToFloatingPointLayout() {
		return this.transformToFloatingPointLayout(null);
	}
		
	public AbstractLayout<V, E> transformToFloatingPointLayout(Dimension size) {
		Graph<V, E> newGraph = null;
		if (this.getGraph() instanceof EmbeddedUndirectedGraph) {
			newGraph = new EmbeddedUndirectedGraph<>((EmbeddedUndirectedGraph<V, E>) this.getGraph()); //copy the graph
		}
		else {
			newGraph = (Graph<V, E>) SerialUtils.cloneObject(this.getGraph()); //does probably not work properly
		}
		
		for (V v : this.getGraph().getVertices()) {
			try {
				if (!visibilityVertices.get(v)) {
					newGraph.removeVertex(v);
				}
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		for (E edge : this.getGraph().getEdges()) {
			try {
				if (!visibilityEdges.get(edge)) {
					newGraph.removeEdge(edge);
				}
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		
		AbstractLayout<V,E> transformedLayout = new AbstractLayout<V, E>(newGraph) {

			@Override
			public void initialize() {
				//do nothing
			}

			@Override
			public void reset() {
				//do nothing
			}
		};
		
		for (V v : newGraph.getVertices()) {
			transformedLayout.setLocation(v, this.apply(v).transform2Point2D());
		}
		
		if (size != null) {
			transformedLayout.setSize(size);
		}
		
		return transformedLayout;
	}
}
