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
package org.aksw.sparql2nl.queryprocessing;

import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.kb.sparql.SparqlQuery;

import org.apache.jena.query.ResultSet;

public class SPARQLDomainExtractor implements DomainExtractor{
	
	private SparqlEndpoint endpoint;
	
	public SPARQLDomainExtractor(SparqlEndpoint endpoint) {
		this.endpoint = endpoint;
	}

	@Override
	public String getDomain(String propertyURI) {
		String query = String.format("SELECT ?domain WHERE {<%s> <http://www.w3.org/2000/01/rdf-schema#domain> ?domain}", propertyURI);
		ResultSet rs = new SparqlQuery(query, endpoint).send(false);
		while(rs.hasNext()){
			return rs.next().get("domain").asResource().getURI();
		}
		return null;
	}

}
