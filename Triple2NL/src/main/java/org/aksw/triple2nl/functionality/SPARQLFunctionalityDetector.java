/**
 * 
 */
package org.aksw.triple2nl.functionality;

import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.dllearner.core.owl.ObjectProperty;
import org.dllearner.reasoning.SPARQLReasoner;

/**
 * @author Lorenz Buehmann
 *
 */
public class SPARQLFunctionalityDetector implements FunctionalityDetector{
	
	private SPARQLReasoner sparqlReasoner;

	public SPARQLFunctionalityDetector(QueryExecutionFactory qef) {
		sparqlReasoner = new SPARQLReasoner(qef);
	}

	/* (non-Javadoc)
	 * @see org.aksw.triple2nl.functionality.FunctionalityDetector#isFunctional(java.lang.String)
	 */
	@Override
	public boolean isFunctional(String uri) {
		try {
			return sparqlReasoner.isFunctional(new ObjectProperty(uri));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

}
