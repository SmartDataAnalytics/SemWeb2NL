/*
 * #%L
 * ASSESS
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
package org.aksw.assessment.informativeness;

import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.dllearner.kb.sparql.SparqlEndpoint;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.ResultSet;

/**
 * Computes the informativeness of a given triple by executing two SPARQL
 * queries to get the number of incoming and outgoing links. Given a triple t we
 * get informativeness = log(|incomingLinks(t_s)| + |outgoingLinks(t_o)|)
 * 
 * This is based on the work of Giuseppe Pirro in REWOrD: Semantic Relatedness
 * in the Web of Data, AAAI 2012.
 * 
 * @author Lorenz Buehmann
 *
 */
public class StatisticalInformativenessGenerator implements InformativenessGenerator{
	
	private QueryExecutionFactory qef;
	
	private static final ParameterizedSparqlString incomingLinksTemplate = new ParameterizedSparqlString(
			"SELECT (COUNT(*) AS ?cnt) WHERE {?s ?p ?o}");
	private static final ParameterizedSparqlString outgoingLinksTemplate = new ParameterizedSparqlString(
			"SELECT (COUNT(*) AS ?cnt) WHERE {?s ?p ?o}");

	public StatisticalInformativenessGenerator(SparqlEndpoint endpoint) {
		qef = new QueryExecutionFactoryHttp(endpoint.getURL().toString(), endpoint.getDefaultGraphURIs());
	}

	/* (non-Javadoc)
	 * @see org.aksw.assessment.question.informativeness.InformativenessGenerator#computeInformativeness(com.hp.hpl.jena.graph.Triple)
	 */
	@Override
	public double computeInformativeness(Triple triple) {
		double informativeness = 0;
		
		//get the popularity of the subject, i.e. the incoming links
		incomingLinksTemplate.setIri("s", triple.getSubject().getURI());
		Query query = incomingLinksTemplate.asQuery();
		QueryExecution qe = qef.createQueryExecution(query);
		ResultSet rs = qe.execSelect();
		int subjectPropularity = rs.next().getLiteral("cnt").getInt();
		
		//get the popularity of the object, i.e. the incoming links
		outgoingLinksTemplate.setIri("o", triple.getObject().getURI());
		query = outgoingLinksTemplate.asQuery();
		qe = qef.createQueryExecution(query);
		rs = qe.execSelect();
		int objectPropularity = rs.next().getLiteral("cnt").getInt();
		
		informativeness = Math.log(subjectPropularity + objectPropularity);
		
		qe.close();
		
		return informativeness;
	}

}
