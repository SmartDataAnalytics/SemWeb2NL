/*
 * #%L
 * AVATAR
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
package org.aksw.avatar;

import java.util.Set;
import java.util.TreeSet;

import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.dllearner.kb.sparql.SparqlEndpoint;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

/**
 * @author Lorenz Buehmann
 *
 */
public class DBpediaPropertyAnalyzer {
	
	public static void main(String[] args) throws Exception {
		SparqlEndpoint endpoint = SparqlEndpoint.getEndpointDBpedia();
		QueryExecutionFactory qef = new QueryExecutionFactoryHttp(endpoint.getURL().toString(), endpoint.getDefaultGraphURIs());
		//get all object properties
		Set<String> properties = new TreeSet<>();
		String query = "SELECT ?p WHERE {?p a <http://www.w3.org/2002/07/owl#ObjectProperty>}";
		QueryExecution qe = qef.createQueryExecution(query);
		ResultSet rs = qe.execSelect();
		QuerySolution qs;
		while(rs.hasNext()){
			qs = rs.next();
			properties.add(qs.getResource("p").getURI());
		}
		qe.close();
		//get a value for each property
		for (String property : properties) {
			query = "SELECT ?o WHERE {?s <" + property + "> ?o.} LIMIT 1";
			qe = qef.createQueryExecution(query);
			rs = qe.execSelect();
			if(rs.hasNext()){
				qs = rs.next();
				if(qs.get("o").isURIResource()){
					String objectURI = qs.getResource("o").getURI();
					if(objectURI.contains("__")){
						System.out.println(property);
					}
				}
			}
			qe.close();
		}
		
	}

}
