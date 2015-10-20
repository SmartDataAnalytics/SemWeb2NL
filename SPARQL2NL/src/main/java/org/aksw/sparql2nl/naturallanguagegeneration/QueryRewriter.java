/**
 * 
 */
package org.aksw.sparql2nl.naturallanguagegeneration;

import java.util.Iterator;
import java.util.Set;

import org.aksw.sparql2nl.queryprocessing.TriplePatternExtractor;
import org.aksw.triple2nl.property.PropertyVerbalizer;
import org.apache.log4j.Logger;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.sparql.core.TriplePath;
import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.expr.ExprAggregator;
import com.hp.hpl.jena.sparql.expr.ExprFunction;
import com.hp.hpl.jena.sparql.expr.ExprFunction2;
import com.hp.hpl.jena.sparql.expr.ExprVar;
import com.hp.hpl.jena.sparql.expr.aggregate.Aggregator;
import com.hp.hpl.jena.sparql.syntax.Element;
import com.hp.hpl.jena.sparql.syntax.ElementFilter;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.sparql.syntax.ElementPathBlock;
import com.hp.hpl.jena.sparql.syntax.ElementTriplesBlock;

/**
 * @author Lorenz Buehmann
 *
 */
public class QueryRewriter {
	
	private static final Logger logger = Logger.getLogger(QueryRewriter.class.getName());

	private FilterExpressionConverter filterConverter;
	private PropertyVerbalizer propertyVerbalizer;

	public QueryRewriter(FilterExpressionConverter filterConverter, PropertyVerbalizer propertyVerbalizer) {
		this.filterConverter = filterConverter;
		this.propertyVerbalizer = propertyVerbalizer;
	}

	public Query rewriteAggregates(Query originalQuery) {
		if(originalQuery.hasAggregators()){
			logger.info("Rewriting aggregates in query\n" + originalQuery);
			Query copy = QueryFactory.create(originalQuery);
			ElementGroup wherePart = (ElementGroup) copy.getQueryPattern();
			
			Iterator<Expr> havingExprsIterator = copy.getHavingExprs().iterator();
			while (havingExprsIterator.hasNext()) {
				Expr expr = (Expr) havingExprsIterator.next();

				ExprFunction function = expr.getFunction();
				if (function instanceof ExprFunction2) {
					Expr left = ((ExprFunction2) function).getArg1();
					Expr right = ((ExprFunction2) function).getArg2();
					
					left = rewriteAggregateExpression(copy, left);
					right = rewriteAggregateExpression(copy, right);
					
					try {
						ExprFunction newFunction = function.getClass().getConstructor(Expr.class, Expr.class).newInstance(left, right);
						ElementFilter filter = new ElementFilter(newFunction);
						wherePart.addElementFilter(filter);
						havingExprsIterator.remove();
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					
				}
				if (copy.getHavingExprs().isEmpty()) {
					copy.getGroupBy().getVars().clear();
					copy.getGroupBy().getExprs().clear();
				}
			}
			logger.info("Rewritten query:\n" + copy);
			return copy;
		}
		return originalQuery;
	}
	
	private Expr rewriteAggregateExpression(Query query, Expr expr){
		if(expr instanceof ExprAggregator){
			// get the var which is used
			Aggregator aggregator = ((ExprAggregator) expr).getAggregator();
			ExprVar exprVar = aggregator.getExprList().get(0).getExprVar();
			logger.debug("rewriting "  + aggregator);
			logger.debug("anchor var: " + exprVar);
			
			// get predicate in triple where var is object
			TriplePatternExtractor extractor = new TriplePatternExtractor();
			Set<Triple> incomingTriplePatterns = extractor.extractIncomingTriplePatterns(query, exprVar.getAsNode());
			logger.debug("triple: " + incomingTriplePatterns);

			// build new Triple pattern
			// we assume only one triple pattern for now
			Triple triple = incomingTriplePatterns.iterator().next();
			ElementGroup wherePart = (ElementGroup) query.getQueryPattern();

			ElementTriplesBlock block = new ElementTriplesBlock();
			
			Triple newTriple = Triple.create(
					triple.getSubject(),
					getAggregatedPredicate(aggregator, triple.getPredicate()),
					triple.getObject());
			
			block.addTriple(newTriple);
			ElementGroup body = new ElementGroup();
			body.addElement(block);

			Iterator<Element> it = wherePart.getElements().iterator();
			while (it.hasNext()) {
				Element el = (Element) it.next();
				
				if (el instanceof ElementPathBlock) {
					boolean removed = false;
					Iterator<TriplePath> patternElts = ((ElementPathBlock) el).patternElts();
					while (patternElts.hasNext()) {
						TriplePath triplePath = (TriplePath) patternElts.next();
						if (triplePath.asTriple().equals(triple)) {
							patternElts.remove();
							removed = true;
						}
					}
					if(removed){
						((ElementPathBlock) el).addTriple(newTriple);
					}
				}

			}
			return exprVar;
		}
		return expr;
	}
	
	private Node getAggregatedPredicate(Aggregator aggregator, Node predicate){
		filterConverter.startVisit();
		String aggregatorText = filterConverter.convertAggregator(aggregator);
		String propertyText = propertyVerbalizer.verbalize(predicate.getURI()).getVerbalizationText();
		
		String s = aggregatorText.trim() + " " + propertyText.trim();
		s = enCamelCase(s);
		
		return NodeFactory.createURI("http://sparql2nl.aksw.org/rewrite/" + s);
	}

    private String enCamelCase(String text){
    	String s = "";
    	String[] tokens = text.split(" ");
    	//first token lowercase
    	s += tokens[0].toLowerCase();
    	//other tokens with beginning capital letter
		for (int i = 1; i < tokens.length; i++) {
			s += org.apache.commons.lang3.StringUtils.capitalize(tokens[i]);
		}
		return s;
    }

}
