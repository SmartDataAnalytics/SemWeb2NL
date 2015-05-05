/**
 * 
 */
package org.aksw.triple2nl.functionality;

import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.dllearner.reasoning.SPARQLReasoner;
import org.semanticweb.owlapi.model.IRI;

import uk.ac.manchester.cs.owl.owlapi.OWLObjectPropertyImpl;

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
			return sparqlReasoner.isFunctional(new OWLObjectPropertyImpl(IRI.create(uri)));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

}
