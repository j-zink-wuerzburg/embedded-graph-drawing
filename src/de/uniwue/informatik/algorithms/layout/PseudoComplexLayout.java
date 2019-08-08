package de.uniwue.informatik.algorithms.layout;

import com.google.common.base.Function;

import edu.uci.ics.jung.graph.Graph;

/**
 * Use this to create layouts consisting of regular vertices, bend points and crossing points.
 * In practice all these 3 types are modeled by normal vertices and its special function
 * (crossing point / bend point) is currently not checked at all
 * 
 * @author Johannes
 *
 * @param <V>
 * @param <E>
 */
public class PseudoComplexLayout<V, E> extends AbstractGridLayout<VData<V>, E> {

	public PseudoComplexLayout(Graph<VData<V>, E> graph) {
		super(graph);
	}

    public PseudoComplexLayout(Graph<VData<V>,E> graph, Function<VData<V>,GridPoint> initializer) {
		super(graph);
    }

	@Override
	public void initialize() {
		// TODO Auto-generated method stub	
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
	}

}
