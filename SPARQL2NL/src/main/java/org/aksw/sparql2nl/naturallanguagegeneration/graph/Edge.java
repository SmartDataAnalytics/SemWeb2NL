/*
 * #%L
 * SPARQL2NL
 * %%
 * Copyright (C) 2015 Agile Knowledge Engineering and Semantic Web (AKSW)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
/**
 * 
 */
package org.aksw.sparql2nl.naturallanguagegeneration.graph;

import org.jgrapht.graph.DefaultEdge;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

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
