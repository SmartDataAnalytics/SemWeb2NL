/**
 * 
 */
package org.aksw.sparql2nl.naturallanguagegeneration.graph;

import org.jgrapht.graph.DefaultEdge;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

/**
 * @author Lorenz Buehmann
 *
 */
public class Edge extends DefaultEdge {

	private Triple triple;
	private boolean reverted = false;;

	public Edge(Triple triple) {
		this(triple, false);
	}

	public Edge(Triple triple, boolean reverted) {
		this.triple = triple;
		this.reverted = reverted;
	}

	/**
	 * @return the triple
	 */
	public Triple asTriple() {
		return triple;
	}

	/**
	 * @return the predicateNode
	 */
	public Node getPredicateNode() {
		return triple.getPredicate();
	}
	
	/**
	 * @return the reverted
	 */
	public boolean isReverted() {
		return reverted;
	}

}
