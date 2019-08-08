package de.uniwue.informatik.graph.embedding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;

import org.javatuples.Triplet;

import com.google.common.base.Supplier;

import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;
import edu.uci.ics.jung.graph.AbstractTypedGraph;
import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

/**
 * Adaption of {@link UndirectedSparseGraph}, but with the aim of integrating additional information
 * regarding an embedding.
 * Here at each vertex a clock-wise or counter-clock-wise (question of interpretation) ordering
 * of the incident edges is saved.
 * This is done with an {@link ArrayList} for each vertex.
 * Furthermore for each edge its left and right incident face is stored.
 * 
 * @author Johannes
 *
 * @param <V>
 * @param <E>
 */
public class EmbeddedUndirectedGraph<V, E> extends AbstractTypedGraph<V, E>
        implements UndirectedGraph<V, E>
{

    /**
     * @param <V> the vertex type for the graph Supplier
     * @param <E> the edge type for the graph Supplier
     * @return a {@code Supplier} that creates an instance of this graph type.
     */
    public static <V,E> Supplier<UndirectedGraph<V,E>> getFactory() {
        return new Supplier<UndirectedGraph<V,E>> () {

            public UndirectedGraph<V,E> get() {
                return new EmbeddedUndirectedGraph<V,E>();
            }
        };
    }
    
    /**
     * Map of vertices to adjacency maps.
     * A vertex is mapped to a list of {@link org.javatuples.Pair}s of corresponding elements: 
     * <ul>
     *   <li> adjacent vertices (the one to the incident edge in the same triplet)
     *   <li> incident edges (the one to the adjecent vertex in the same triplet)
     * </ul>
     * 
     * This class represents an graph + its embedding -> The order of incident edges at each vertex is relevant,
     * that is why a {@link LinkedList} is maintained for every vertex.
     * The size of that list of a vertex v is the same as the degree of v in the graph.
     */
    protected Map<V, ArrayList<org.javatuples.Pair<V, E>>> vertices;
    /**
     * Map of edges to incident vertices.
     * 
     * As the graph is undirected the order of vertices in each pair is irrelevant for the graph (could be both).
     * But the order here is used to distinguish between a "left" and "right" incident face of that edge.
     * Thus a quasi-direction is defined here.
     */
    protected Map<E, Pair<V>> edges;
    /**
     * First pair element is left side (seen in quasi-direction gotten by {@link EmbeddedUndirectedGraph#edges})
     * second is right side of that edge
     */
    protected Map<E, Pair<Face<V, E>>> incidentFaces;
    protected Face<V,E> outerFace;

	/**
     * Creates an instance.
     */
    public EmbeddedUndirectedGraph() {
    	super(EdgeType.UNDIRECTED);
        vertices = new HashMap<>();
        edges = new HashMap<>();
        incidentFaces = new HashMap<>();
        outerFace = new Face<>();
    }
    
    /**
     * Copy-Constructor
     * <p>
     * Generic V- and E-objects are the same but the workaround ({@link Collection}s, {@link Map}s, {@link Face}s, ...) is new
     * 
     * @param copyThat
     */
    public EmbeddedUndirectedGraph(EmbeddedUndirectedGraph<V, E> copyThat) {
    	this();
    	for (V v : copyThat.vertices.keySet()) {
    		ArrayList<org.javatuples.Pair<V, E>> value = copyThat.vertices.get(v);
    		ArrayList<org.javatuples.Pair<V, E>> copyList = new ArrayList<>(value.size());
    		for (org.javatuples.Pair<V, E> vEPair : value) {
    			copyList.add(new org.javatuples.Pair<V, E>(vEPair.getValue0(), vEPair.getValue1()));
    		}
    		vertices.put(v, copyList);
    	}
    	for (E e : copyThat.edges.keySet()) {
    		edges.put(e, new Pair<>(copyThat.edges.get(e)));
    	}
    	for (E e : copyThat.incidentFaces.keySet()) {
    		Pair<Face<V, E>> value = copyThat.incidentFaces.get(e);
    		Face<V, E> oldFace1 = value.getFirst();
    		Face<V, E> oldFace2 = value.getSecond();
    		Face<V, E> newFace1 = new Face<V, E>(oldFace1);
    		Face<V, E> newFace2 = new Face<V, E>(oldFace2);
    		incidentFaces.put(e, new Pair<Face<V,E>>(newFace1, newFace2));
    		
    		if (oldFace1.equals(copyThat.getOuterFace())) {
    			this.outerFace = newFace1;
    		}
    		if (oldFace2.equals(copyThat.getOuterFace())) {
    			this.outerFace = newFace2;
    		}
    	}
    }

    /**
     * This appends that edge to the end of the ordered edge-lists of both endpoints.
     * Order of the incident edges (clock-wise or counter-clock-wise) around a vertex is relevant for an embedding
     * but not for a graph. <p>
     * With this method a order-position at each endpoint can not be chosen.
     * So better use {@link EmbeddedUndirectedGraph#addEdge(Object, Object, int, Object, int)}
     */
    @Deprecated
    public boolean addEdge(E edge, Pair<? extends V> endpoints, EdgeType edgeType)
    {
    	return addEdge(edge, endpoints.getFirst(), vertices.get(endpoints.getFirst()).size(), 
    			endpoints.getSecond(), vertices.get(endpoints.getSecond()).size());
    }
    
    /**
     * Adds an edge between the two passed endpoints (vertices). That edge will be inserted at the passed position
     * of the clock-wise or counter-clock-wise edge-ordering (realized as {@link ArrayList}) at each of the two
     * end points.
     * Such an ordering is decisive for an embedding (embedded graph) but not for a graph in general.
     * If the outer face is split by inserting an edge by calling that method, then the left part/half of the old
     * outer face becomes the new face, i.e. the part left seen from endPoint1 along edge to endPoint2.
     * If one wants to become the right half of the old outer face the new outer face then
     * values of endPoint1 and endPoint2 must be swapped by the caller. 
     * 
     * @param edge
     * @param endPoint1
     * @param embeddingOrderIndexAtEndPoint1
     * @param endPoint2
     * @param embeddingOrderIndexAtEndPoint2
     * @return
     */
    public boolean addEdge(E edge, V endPoint1, int embeddingOrderIndexAtEndPoint1, V endPoint2, 
    		int embeddingOrderIndexAtEndPoint2) {
//    	this.validateEdgeType(edgeType);
        Pair<V> new_endpoints = getValidatedEndpoints(edge, new Pair<V>(endPoint1, endPoint2));
        if (new_endpoints == null) {
            return false;
        }
       
        V v1 = new_endpoints.getFirst();
        V v2 = new_endpoints.getSecond();
        
        int oldDegreeV1 = vertices.get(v1).size();
        int oldDegreeV2 = vertices.get(v2).size();
        
        if (embeddingOrderIndexAtEndPoint1 < 0 || embeddingOrderIndexAtEndPoint1 > oldDegreeV1
        		|| embeddingOrderIndexAtEndPoint2 < 0 || embeddingOrderIndexAtEndPoint2 > oldDegreeV2) {
        	return false;
        }
        
        if (findEdge(v1, v2) != null) {
            return false;
        }
        
        //special case: First edge in the whole graph -> base for every edge inserted later
        if (this.getEdgeCount() == 0) {
            edges.put(edge, new_endpoints);
            outerFace.add(new Triplet<E, EdgeSide, Pair<V>>(edge, getAbsoluteSideOfLeftFace(edge, v1), this.getEndpoints(edge)));
            outerFace.add(new Triplet<E, EdgeSide, Pair<V>>(edge, getAbsoluteSideOfLeftFace(edge, v2), this.getEndpoints(edge)));
    		incidentFaces.put(edge, new Pair<>(outerFace, outerFace));
    		vertices.get(v1).add(new org.javatuples.Pair<>(v2, edge));
    		vertices.get(v2).add(new org.javatuples.Pair<>(v1, edge));
    		return true;
        }
        
        //must not connect to two vertices that both have degree 0 (all edges currently must be in a single component)
        if ((!vertices.containsKey(v1) || this.degree(v1) == 0) && (!vertices.containsKey(v2) || this.degree(v2) == 0)) {
        	return false;
        }
        
        if (!vertices.containsKey(v1)) {
            this.addVertex(v1);
        }
        
        if (!vertices.containsKey(v2)) {
            this.addVertex(v2);
        }
        
        //Case 1: One end point has degree 0 -> inserted completely within the face of the other end point (always consistent)
        if (this.degree(v1) == 0) {
            edges.put(edge, new_endpoints);
        	insertEdgeWithOneEndPointHavingDegree0(edge, embeddingOrderIndexAtEndPoint2, v1, v2, oldDegreeV2);
        }
        else if (this.degree(v2) == 0) {
            edges.put(edge, new_endpoints);
        	insertEdgeWithOneEndPointHavingDegree0(edge, embeddingOrderIndexAtEndPoint1, v2, v1, oldDegreeV1);
        }
        //Case 2: Both end points have degree > 0 -> New edge must lie within only one face.
        //        That face is be split now by the new edge. From one face, two faces must be created/adapted.
        else {
        	org.javatuples.Pair<V, E> prevV1 = 
        			vertices.get(v1).get((embeddingOrderIndexAtEndPoint1 - 1 + oldDegreeV1) % oldDegreeV1);
        	org.javatuples.Pair<V, E> prevV2 = 
        			vertices.get(v2).get((embeddingOrderIndexAtEndPoint2 - 1 + oldDegreeV2) % oldDegreeV2);
	        /*
	         * Must be consistent in the current embedding. That means the complete inserted edge must be in the same face
	         * of the total graph.
	         * So at both end points at both embeddingOrderIndices (following the prev incident edge)
	         * there must be the same face (face1 equals face2).
	         */
	        if (!getLeftFace(prevV1.getValue1(), v1).equals(getLeftFace(prevV2.getValue1(), v2))) {
	        	System.out.println(getLeftFace(prevV1.getValue1(), v1)+" - gegen - "+getLeftFace(prevV2.getValue1(), v2));
	        	return false;
	        }
			Face<V, E> face = getLeftFace(prevV1.getValue1(), v1);
	        
	        edges.put(edge, new_endpoints);
	        
	        splitFace(face, edge, v1, prevV1, prevV2);
	        
	        // map v1 to <v2, edge> and vice versa
	        vertices.get(v1).add(embeddingOrderIndexAtEndPoint1, new org.javatuples.Pair<>(v2, edge));
	        vertices.get(v2).add(embeddingOrderIndexAtEndPoint2, new org.javatuples.Pair<>(v1, edge));
        }
        
        return true;
    }

	private void splitFace(Face<V, E> face, E edge, V v1, org.javatuples.Pair<V, E> prevV1,
			org.javatuples.Pair<V, E> prevV2) {
		int indexPrevV1 = face.getIndex(new Triplet<>(prevV1.getValue1(), 
				getAbsoluteSideOfRightFace(prevV1.getValue1(), prevV1.getValue0()), this.getEndpoints(prevV1.getValue1())));
		int indexPrevV2 = face.getIndex(new Triplet<>(prevV2.getValue1(), 
				getAbsoluteSideOfRightFace(prevV2.getValue1(), prevV2.getValue0()), this.getEndpoints(prevV2.getValue1())));
		
		Face<V, E> faceAtPrevV1 = new Face<>();
		Face<V, E> faceAtPrevV2 = new Face<>();
		
		Face<V, E> currentHalfFace;
		assert indexPrevV1 != indexPrevV2 : "Unexpected same edge order index in the face (same edge? "+prevV1+", "+prevV2+")";
		if (indexPrevV1 < indexPrevV2) {
			currentHalfFace = faceAtPrevV2;
		}
		else {
			currentHalfFace = faceAtPrevV1;
		}
		int i = 0;
		for (Triplet<E, EdgeSide, Pair<V>> edgeOfFace : face) {
			if (i == indexPrevV1) {
				currentHalfFace.add(new Triplet<>(edge, getAbsoluteSideOfLeftFace(edge, v1), this.getEndpoints(edge)));
				currentHalfFace = faceAtPrevV1;
			}
			if (i == indexPrevV2) {
				currentHalfFace.add(new Triplet<>(edge, getAbsoluteSideOfRightFace(edge, v1), this.getEndpoints(edge)));
				currentHalfFace = faceAtPrevV2;
			}
			
			currentHalfFace.add(edgeOfFace);
			//adjust reference form old face to new face at each edge
			if (edgeOfFace.getValue1() == EdgeSide.LEFT) {
				incidentFaces.replace(edgeOfFace.getValue0(), new Pair<>(currentHalfFace, 
						incidentFaces.get(edgeOfFace.getValue0()).getSecond()));
			}
			else { //if (edgeOfFace.getValue1() == EdgeSide.RIGHT) {
				incidentFaces.replace(edgeOfFace.getValue0(), new Pair<>( 
						incidentFaces.get(edgeOfFace.getValue0()).getFirst(), currentHalfFace));
			}
			
			++i;
		}
		//adjust reference form old face to new face at the new edge
		if (getAbsoluteSideOfLeftFace(edge, v1) == EdgeSide.LEFT) {
			incidentFaces.put(edge, new Pair<>(faceAtPrevV2, faceAtPrevV1));
		}
		else { //if (getAbsoluteSideOfLeftFace(edge, v1) == EdgeSide.RIGHT) {
			incidentFaces.put(edge, new Pair<>(faceAtPrevV1, faceAtPrevV2));
		}
		
		//special case: face was the outer face -> left half (i.e. faceAtPrevV2) becomes new outer face
		if (face.equals(this.getOuterFace())) {
			this.setOuterFace(faceAtPrevV2);
		}
	}
	

	private Face<V, E> uniteFace(Face<V, E> face0, Face<V, E> face1, E alongThatEdge) {
		assert face0.containsEdge(alongThatEdge);
		assert face1.containsEdge(alongThatEdge);
		assert !face0.containsBothSidesOfThatEdge(alongThatEdge);
		assert !face1.containsBothSidesOfThatEdge(alongThatEdge);
		
		Face<V, E> unitedFace = new Face<>();
		
		int indexAtFace0 = face0.getIndexOfEdge(alongThatEdge);
		int indexAtFace1 = face1.getIndexOfEdge(alongThatEdge);
		
		//fill new face with edges
		for (int i = 0; i < indexAtFace0; ++i) {
			unitedFace.add(face0.get(i));
			replaceIncidentFace(face0.get(i).getValue0(), face0, unitedFace);
		}
		for (int i = indexAtFace1 + 1; i < face1.size(); ++i) {
			unitedFace.add(face1.get(i));
			replaceIncidentFace(face1.get(i).getValue0(), face1, unitedFace);
		}
		for (int i = 0; i < indexAtFace1; ++i) {
			unitedFace.add(face1.get(i));
			replaceIncidentFace(face1.get(i).getValue0(), face1, unitedFace);
		}
		for (int i = indexAtFace0 + 1; i < face0.size(); ++i) {
			unitedFace.add(face0.get(i));
			replaceIncidentFace(face0.get(i).getValue0(), face0, unitedFace);
		}
		
		//special case: one face was the outer face -> new face becomes outer face
		if (face0.equals(this.getOuterFace())) {
			this.setOuterFace(unitedFace);
		}
		if (face1.equals(this.getOuterFace())) {
			this.setOuterFace(unitedFace);
		}
		

    	//remove it from the data structures
    	incidentFaces.remove(alongThatEdge);
    	removeFromVerticesMap(alongThatEdge);
		
		return unitedFace;
	}
	
	private void replaceIncidentFace(E edge, Face<V, E> oldFace, Face<V, E> newFace) {
		assert incidentFaces.containsKey(edge);
		
		Pair<Face<V, E>> currentPair = incidentFaces.get(edge);
		boolean replaceFirst = false;
		boolean replaceSecond = false;
		if (currentPair.getFirst().equals(oldFace)) {
			replaceFirst = true;
		}
		if (currentPair.getSecond().equals(oldFace)) {
			replaceSecond = true;
		}
		
		incidentFaces.replace(edge, new Pair<Face<V, E>>(
				replaceFirst ? newFace : currentPair.getFirst(), replaceSecond ? newFace : currentPair.getSecond()));
	}

	private void insertEdgeWithOneEndPointHavingDegree0(E edge, int embeddingOrderIndexAtOtherEndPoint, 
			V vWithDegree0, V vOtherEndPoint, int oldDegreeOtherEndPoint) {
		//adjust face (insert these new edge twice (both sides))
		org.javatuples.Pair<V, E> prevAtOtherEndPoint = vertices.get(vOtherEndPoint).get(
				(embeddingOrderIndexAtOtherEndPoint - 1 + oldDegreeOtherEndPoint) % oldDegreeOtherEndPoint);
		Face<V, E> face = getLeftFace(prevAtOtherEndPoint.getValue1(), vOtherEndPoint);
		face.addBefore(new Triplet<>(edge, getAbsoluteSideOfRightFace(edge, vOtherEndPoint), this.getEndpoints(edge)), 
				new Triplet<>(prevAtOtherEndPoint.getValue1(), 
						getAbsoluteSideOfLeftFace(prevAtOtherEndPoint.getValue1(), vOtherEndPoint), 
						this.getEndpoints(prevAtOtherEndPoint.getValue1())));
		face.addBefore(new Triplet<>(edge, getAbsoluteSideOfLeftFace(edge, vOtherEndPoint), this.getEndpoints(edge)), 
				new Triplet<>(edge, getAbsoluteSideOfRightFace(edge, vOtherEndPoint), this.getEndpoints(edge)));
		
		//add new edge in vertex-neighbor-lists and edge-incidence-list
		incidentFaces.put(edge, new Pair<>(face, face));
		//adjust all incident faces
		for (Triplet<E, EdgeSide, Pair<V>> eData : face) {
			E edgeOfFace = eData.getValue0();
			Pair<Face<V, E>> currentValue = incidentFaces.get(edgeOfFace);
			incidentFaces.replace(edgeOfFace, new Pair<>(eData.getValue1() == EdgeSide.LEFT ? face : currentValue.getFirst(),
					eData.getValue1() == EdgeSide.RIGHT ? face : currentValue.getSecond()));
		}
		//add edges in circular-order adjacency-list at both vertices
		vertices.get(vWithDegree0).add(new org.javatuples.Pair<>(vOtherEndPoint, edge));
		vertices.get(vOtherEndPoint).add(embeddingOrderIndexAtOtherEndPoint, new org.javatuples.Pair<>(vWithDegree0, edge));
	}

    public Collection<E> getInEdges(V vertex)
    {
        return this.getIncidentEdges(vertex);
    }

    public Collection<E> getOutEdges(V vertex)
    {
        return this.getIncidentEdges(vertex);
    }

    public Collection<V> getPredecessors(V vertex)
    {
        return this.getNeighbors(vertex);
    }

    public Collection<V> getSuccessors(V vertex)
    {
        return this.getNeighbors(vertex);
    }

    @Override
    public E findEdge(V v1, V v2)
    {
        if (!containsVertex(v1) || !containsVertex(v2))
            return null;
        
        for (org.javatuples.Pair<V, E> neighbor : vertices.get(v1)) {
        	if (neighbor.getValue0().equals(v2)) {
        		return neighbor.getValue1();
        	}
        }
        
        return null;
    }
    
    @Override
    public Collection<E> findEdgeSet(V v1, V v2)
    {
        if (!containsVertex(v1) || !containsVertex(v2))
            return null;
        ArrayList<E> edge_collection = new ArrayList<E>(1);
//        if (!containsVertex(v1) || !containsVertex(v2))
//            return edge_collection;
        E e = findEdge(v1, v2);
        if (e == null)
            return edge_collection;
        edge_collection.add(e);
        return edge_collection;
    }
    
    public Pair<V> getEndpoints(E edge)
    {
        return edges.get(edge);
    }

    public V getSource(E directed_edge)
    {
        return null;
    }

    public V getDest(E directed_edge)
    {
        return null;
    }

    public boolean isSource(V vertex, E edge)
    {
        return false;
    }

    public boolean isDest(V vertex, E edge)
    {
        return false;
    }

    public Collection<E> getEdges()
    {
        return Collections.unmodifiableCollection(edges.keySet());
    }

    public Collection<V> getVertices()
    {
        return Collections.unmodifiableCollection(vertices.keySet());
    }

    public boolean containsVertex(V vertex)
    {
        return vertices.containsKey(vertex);
    }

    public boolean containsEdge(E edge)
    {
        return edges.containsKey(edge);
    }

    public int getEdgeCount()
    {
        return edges.size();
    }

    public int getVertexCount()
    {
        return vertices.size();
    }

    public ArrayList<org.javatuples.Pair<V, E>> getNeighborsInEmbeddingOrder(V vertex)
    {
    	if(!containsVertex(vertex))
    		return null;
    	return (ArrayList<org.javatuples.Pair<V, E>>) vertices.get(vertex).clone();
    }
    
    public Collection<V> getNeighbors(V vertex)
    {
        if (!containsVertex(vertex))
            return null;
        
        ArrayList<V> neighborVertices = new ArrayList<V>(vertices.get(vertex).size());
        for (org.javatuples.Pair<V, E> neighbor : vertices.get(vertex)) {
        	neighborVertices.add(neighbor.getValue0());
        }
        return neighborVertices;
    }

    public Collection<E> getIncidentEdges(V vertex)
    {
        if (!containsVertex(vertex))
            return null;

        ArrayList<E> neighborEdges = new ArrayList<E>(vertices.get(vertex).size());
        for (org.javatuples.Pair<V, E> neighbor : vertices.get(vertex)) {
        	neighborEdges.add(neighbor.getValue1());
        }
        return neighborEdges;
    }

    public boolean addVertex(V vertex)
    {
        if(vertex == null) {
            throw new IllegalArgumentException("vertex may not be null");
        }
        if (!containsVertex(vertex)) {
            vertices.put(vertex, new ArrayList<org.javatuples.Pair<V,E>>());
            return true;
        } else {
            return false;
        }
    }

    public boolean removeVertex(V vertex)
    {
        if (!containsVertex(vertex))
            return false;

        // iterate over copy of incident edge collection
        ArrayList<org.javatuples.Pair<V, E>> neighborCopyList = new ArrayList<>(vertices.get(vertex));
        for (int i = neighborCopyList.size() - 1; i >= 0; --i) {
            removeEdge(neighborCopyList.get(i).getValue1());
        }
        
        vertices.remove(vertex);
        return true;
    }

    public boolean removeEdge(E edge)
    {
        if (!containsEdge(edge))
            return false;
        
        Pair<V> endpoints = getEndpoints(edge);
        V v1 = endpoints.getFirst();
        V v2 = endpoints.getSecond();

        
        //unite face if necessary
        if (!incidentFaces.get(edge).getFirst().equals(incidentFaces.get(edge).getSecond())) {
        	uniteFace(incidentFaces.get(edge).getFirst(), incidentFaces.get(edge).getSecond(), edge);
        }
        else {
        	//if it is the same edge remove it from the face
        	incidentFaces.get(edge).getFirst().removeEdgeAllOccurences(edge);
        	incidentFaces.get(edge).getSecond().removeEdgeAllOccurences(edge);
        	//remove it from the data structures
        	incidentFaces.remove(edge);
        	removeFromVerticesMap(edge);
        }
        
        edges.remove(edge);
        
        return true;
    }
    
    private void removeFromVerticesMap(E edge) {
    	for (V v : this.getEndpoints(edge)) {
    		for (org.javatuples.Pair<V, E> adj : new ArrayList<>(vertices.get(v))) {
    			if (adj.getValue1().equals(edge)) {
    				vertices.get(v).remove(adj);
    			}
    		}
    	}
    }

    public Face<V, E> getOuterFace() {
		return outerFace;
	}

	public void setOuterFace(Face<V, E> outerFace) {
		this.outerFace = outerFace;
	}
	
	/**
	 * 
	 * @return
	 */
	public Collection<Face<V, E>> getFaces() {
		HashSet<Face<V, E>> allFaces = new LinkedHashSet<>();
		for (Pair<Face<V, E>> pairOfIncidentFaces : incidentFaces.values()) {
			allFaces.add(pairOfIncidentFaces.getFirst());
			allFaces.add(pairOfIncidentFaces.getSecond());
		}
		return allFaces;
	}
	
	public Face<V, E> getLeftIncidentFace(E edge) {
		return incidentFaces.get(edge).getFirst();
	}

	public Face<V, E> getRightIncidentFace(E edge) {
		return incidentFaces.get(edge).getSecond();
	}
	
	public Pair<Face<V, E>> getIncidentFaces(E edge) {
		return new Pair<>(this.getLeftIncidentFace(edge), this.getRightIncidentFace(edge));
	}
	
	/**
	 * -1 for not contained
	 * 
	 * @param edge
	 * 		incident edge of which the index in the list of the vertex (2nd parameter) is returned
	 * @param atThisVertex
	 * 		In the adjacency-list of this vertex the index of the edge (1st parameter) is returned
	 * @return
	 */
	public int getEdgeIndexInEmbeddingList(E edge, V atThisVertex) {
		return getVertexIndexInEmbeddingList(this.getOpposite(atThisVertex, edge), atThisVertex);
	}
	
	/**
	 * -1 for not contained.
	 * The returned number is the edge index of an edge bounding the passed face with higher index.
	 * E.g. if in the embedding list at atThisVertex the passed face is bounded by the
	 * 5th and 6th edge, that method returns 6.
	 * If the same face appears more than once, then only the first occurence is considered.
	 * E.g. if it appears between the 5th and 6th edge and again between the 9th an 10th edge,
	 * then 6 is returned.
	 * 
	 * @param face
	 * @param atThisVertex
	 * @return
	 */
	public int getEdgeIndexInEmbeddingList(Face<V, E> face, V atThisVertex) {
		int index = 0;
		for (org.javatuples.Pair<V, E> neighbor : this.getNeighborsInEmbeddingOrder(atThisVertex)) {
			if (face.equals(this.getRightFace(neighbor.getValue1(), atThisVertex))) {
				return index; 
			}
			++index;
		}
		return -1;
	}
	
	/**
	 * -1 for not contained
	 * 
	 * @param vertex
	 * 		adjacent vertex of which the index in the list of the other vertex is returned
	 * @param atThisVertex
	 * 		In the adjacency-list of this vertex the index of the other vertex is returned
	 * @return
	 */
	public int getVertexIndexInEmbeddingList(V vertex, V atThisVertex) {
		ArrayList<org.javatuples.Pair<V, E>> embeddedNeighbors = this.getNeighborsInEmbeddingOrder(atThisVertex);
		for (int i = 0; i < embeddedNeighbors.size(); ++i) {
			if (embeddedNeighbors.get(i).getValue0().equals(vertex)) {
				return i;
			}
		}
		return -1;	
	}
	
	public boolean isDirectPredecessorInEmbeddingList(V predecessor, V successor, V atThisVertex) {
		int indexPre = this.getVertexIndexInEmbeddingList(predecessor, atThisVertex);
		int indexSuc = this.getVertexIndexInEmbeddingList(successor, atThisVertex);
		if (indexSuc - indexPre == 1) {
			return true;
		}
		else if (indexPre == this.degree(atThisVertex) - 1 && indexSuc == 0) {
			return true;
		}
		
		return false;
	}
	
	private EdgeSide getAbsoluteSideOfRightFace(E edge, V seenFromThatVertex) {
		assert edges.containsKey(edge) : "Edge "+edge+" does not exist in graph "+this;
		Pair<V> endPoints = edges.get(edge);
		assert endPoints.contains(seenFromThatVertex) : "Edge "+edge+" does not have "+seenFromThatVertex+" as an endpoint.";
		
		if (endPoints.getFirst().equals(seenFromThatVertex)) {
			return EdgeSide.RIGHT;
		}
		return EdgeSide.LEFT;
	}
	
	private EdgeSide getAbsoluteSideOfLeftFace(E edge, V seenFromThatVertex) {
		assert edges.containsKey(edge) : "Edge "+edge+" does not exist in graph "+this;
		Pair<V> endPoints = edges.get(edge);
		assert endPoints.contains(seenFromThatVertex) : "Edge "+edge+" does not have "+seenFromThatVertex+" as an endpoint.";
		
		if (endPoints.getFirst().equals(seenFromThatVertex)) {
			return EdgeSide.LEFT;
		}
		return EdgeSide.RIGHT;
	}
	
	public Face<V, E> getLeftFace(E edge, V seenFromThatVertex) {
		if (getAbsoluteSideOfLeftFace(edge, seenFromThatVertex) == EdgeSide.LEFT) {
			return incidentFaces.get(edge).getFirst();
		}
		return incidentFaces.get(edge).getSecond();
	}
	
	public Face<V, E> getRightFace(E edge, V seenFromThatVertex) {
		if (getAbsoluteSideOfRightFace(edge, seenFromThatVertex) == EdgeSide.RIGHT) {
			return incidentFaces.get(edge).getSecond();
		}
		return incidentFaces.get(edge).getFirst();
	}
	
	
	public int getIndexOfVertexBoundingTheCommonFaceLaterInTheCircularOrder(V neighborOfV0, V neighborOfV1, V atThisVertex) {
		int indexNeighbor0 = getVertexIndexInEmbeddingList(neighborOfV0, atThisVertex);
		int indexNeighbor1 = getVertexIndexInEmbeddingList(neighborOfV1, atThisVertex);
		if (indexNeighbor0 - indexNeighbor1 == 1 || indexNeighbor0 - indexNeighbor1 == -(degree(atThisVertex) - 1)) {
			return getVertexIndexInEmbeddingList(neighborOfV0, atThisVertex);
		}
		else if (indexNeighbor0 - indexNeighbor1 == -1 || indexNeighbor0 - indexNeighbor1 == degree(atThisVertex) - 1) {
			return getVertexIndexInEmbeddingList(neighborOfV1, atThisVertex);
		}
		return -1;
	}
	
	/**
	 * Consider the face at one vertex.
	 * Two adjacent vertices of this vertex that are neighbors in the circular order around this vertex
	 * are incident to a face at this vertex.
	 * For a vertex of degree 2 this might be two different faces then one of them is returned.
	 * Note that this will throw a exception
	 * if the two specified neighbors are not neighboring in the circular order around atThisVertex.
	 * 
	 * @param neighborOfV0
	 * 		adjacent to atThisVertex
	 * @param neighborOfV1
	 * 		adjacent to atThisVertex
	 * @param atThisVertex
	 * @return
	 */
	public Face<V, E> getFaceInBetween(V neighborOfV0, V neighborOfV1, V atThisVertex) {
		return getRightFace(findEdge(atThisVertex, getNeighborsInEmbeddingOrder(atThisVertex).get(
				getIndexOfVertexBoundingTheCommonFaceLaterInTheCircularOrder(neighborOfV0, neighborOfV1, atThisVertex)).getValue0()),
				atThisVertex);
	}
}
