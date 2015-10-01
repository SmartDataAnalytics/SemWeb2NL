/**
 * 
 */
package org.aksw.triple2nl.util;

import java.util.Comparator;

import com.google.common.collect.ComparisonChain;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.util.NodeComparator;

/**
 * Comparator to sort a list of triples by subject, predicate, and object to
 * ensure a consistent order for human-readable output
 * 
 * @author Lorenz Buehmann
 *
 */
public class TripleComparator implements Comparator<Triple>{
	
	private final NodeComparator nodeComparator = new NodeComparator();

	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(Triple t1, Triple t2) {
		return ComparisonChain.start()
		.compare(t1.getSubject(), t2.getSubject(), nodeComparator)
		.compare(t1.getPredicate(), t2.getPredicate(), nodeComparator)
		.compare(t1.getObject(), t2.getObject(), nodeComparator)
		.result();
	}

}
