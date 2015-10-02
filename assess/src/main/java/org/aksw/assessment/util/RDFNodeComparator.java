/**
 * 
 */
package org.aksw.assessment.util;

import java.util.Comparator;

import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.util.NodeUtils;

/**
 * A comparator for RDFNode objects.
 * @author Lorenz Buehmann
 *
 */
public class RDFNodeComparator implements Comparator<RDFNode> {

	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(RDFNode o1, RDFNode o2) {
		return NodeUtils.compareRDFTerms(o1.asNode(), o2.asNode());
	}

}
