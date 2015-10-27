/**
 * 
 */
package org.aksw.assessment.util;

import org.aksw.sparqltools.util.SPARQLEndpointType;

import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.SortCondition;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.expr.E_Function;
import com.hp.hpl.jena.sparql.expr.ExprAggregator;
import com.hp.hpl.jena.sparql.expr.ExprList;
import com.hp.hpl.jena.sparql.expr.aggregate.AggCountVar;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.sparql.util.ExprUtils;

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
