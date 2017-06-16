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
package org.aksw.assessment.util;

import org.aksw.sparqltools.util.SPARQLEndpointType;

import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.SortCondition;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.E_Function;
import org.apache.jena.sparql.expr.ExprAggregator;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.aggregate.AggCountVar;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.util.ExprUtils;

/**
 * @author Lorenz Buehmann
 *
 */
public class SPARQLQueryUtils {
	
	/**
	 * Defines the default SPARQL prefixes for rdf, rdfs and owl mostly used in SPARQL queries.
	 */
	public static final String DEFAULT_PREFIXES = 
			"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
			+ "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
			+ "PREFIX owl:<http://www.w3.org/2002/07/owl#>\n";
	
	private static final String INCOMING_LINK_VAR_NAME = "?incoming";
	
	/**
     * For some {@link SPARQLEndpointType SPARQL endpoint types} we need to add an additional constraint to
     * determine the ranking information based on incoming relations of the entities.
     * <p>
     * Currently this done for {@link SPARQLEndpointType#ARQ ARQ} and {@link SPARQLEndpointType#LARQ LARQ}.
     * 
     * @param endpointType
     *            the endpoint type
     * @param query
     *            the SPARQL query
     * @param rootVar
     *            the variable used to select entities
     */
    public static void addRanking(SPARQLEndpointType endpointType,
                                              final Query query,
                                              final Var rootVar) {
    	addRankingConstraints(endpointType, query, rootVar);
    	addRankingOrder(endpointType, query, rootVar);
    }
	
	/**
     * For some {@link SPARQLEndpointType SPARQL endpoint types} we need to add an additional constraint to
     * determine the ranking information based on incoming relations to the Entities.
     * <p>
     * Currently this done for {@link SPARQLEndpointType#ARQ ARQ} and {@link SPARQLEndpointType#LARQ LARQ}.
     * 
     * @param endpointType
     *            the endpoint type
     * @param query
     *            the SPARQL query
     * @param rootVar
     *            the variable used to select entities
     */
    public static void addRankingConstraints(SPARQLEndpointType endpointType,
                                              final Query query,
                                              final Var rootVar) {
    	// for Virtuoso we do not need to count incoming links, because it
        // has a page rank like feature that we can use to rank entities!
        // all others do not support sorting
    	if(endpointType == SPARQLEndpointType.Virtuoso){
    		
    	} else {
    		ElementGroup whereClause = (ElementGroup) query.getQueryPattern();
    		whereClause.addTriplePattern(Triple.create(
    				NodeFactory.createVariable(INCOMING_LINK_VAR_NAME),
    				NodeFactory.createVariable("p_in"),
    				rootVar));
    	}
    }

    /**
     * @param endpointType the SPARQL endpoint type
     * @param query the SPARQL query
     */
    public static void addRankingOrder(SPARQLEndpointType endpointType,
    		final Query query,
            final Var rootVar) {
        if (endpointType == SPARQLEndpointType.Virtuoso) {
        	query.addOrderBy(
        			new SortCondition(
        					new E_Function("LONG::IRI_RANK", new ExprList(ExprUtils.nodeToExpr(rootVar))),
        					Query.ORDER_DESCENDING));
        } else {
            // TODO: COUNT is not part of the SPARQL 1.0 specification!
            // see http://www.w3.org/2009/sparql/wiki/Feature:AggregateFunctions
        	query.addOrderBy(
        			new SortCondition(
        					new ExprAggregator(
        							rootVar, 
        							new AggCountVar(ExprUtils.nodeToExpr(rootVar))), 
        					Query.ORDER_DESCENDING));
        } 
    }

}
