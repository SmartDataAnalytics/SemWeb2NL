package org.sparql2nl.demo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementOptional;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementTriplesBlock;
import org.apache.jena.sparql.syntax.ElementUnion;
import org.apache.jena.sparql.syntax.ElementVisitorBase;
import org.apache.jena.sparql.util.VarUtils;

public class TriplePatternExtractor extends ElementVisitorBase {
	
	private Set<Triple> allTriplePattern;
	private Set<Triple> unionTriplePattern;
	private Set<Triple> candidates;
	
	private boolean inOptionalClause = false;
	
	private boolean inUnionClause = false;
	
	public Set<Triple> extractTriplePattern(Query query){
		return extractTriplePattern(query, false);
	}
	
	public Set<Triple> extractTriplePattern(Query query, boolean ignoreOptionals){
		allTriplePattern = new HashSet<Triple>();
		unionTriplePattern = new HashSet<Triple>();
		candidates = new HashSet<Triple>();
		
		query.getQueryPattern().visit(this);
		
		//postprocessing: triplepattern in OPTIONAL clause
		if(!ignoreOptionals){
			if(query.isSelectType()){
				for(Triple t : candidates){
					if(org.apache.commons.collections.ListUtils.intersection(new ArrayList<Var>(VarUtils.getVars(t)), query.getProjectVars()).size() >= 2){
						allTriplePattern.add(t);
					}
				}
			}
		}
		
		return allTriplePattern;
	}
	
	@Override
	public void visit(ElementGroup el) {
		for (Iterator<Element> iterator = el.getElements().iterator(); iterator.hasNext();) {
			Element e = iterator.next();
			e.visit(this);
		}
	}

	@Override
	public void visit(ElementOptional el) {
		inOptionalClause = true;
		el.getOptionalElement().visit(this);
		inOptionalClause = false;
	}

	@Override
	public void visit(ElementTriplesBlock el) {
		for (Iterator<Triple> iterator = el.patternElts(); iterator.hasNext();) {
			Triple t = iterator.next();
			if(inOptionalClause){
				candidates.add(t);
			} else {
				allTriplePattern.add(t);
			}
		}
	}

	@Override
	public void visit(ElementPathBlock el) {
		for (Iterator<TriplePath> iterator = el.patternElts(); iterator.hasNext();) {
			TriplePath tp = iterator.next();
			if(inOptionalClause){
				candidates.add(tp.asTriple());
			} else {
				allTriplePattern.add(tp.asTriple());
			}
		}
	}

	@Override
	public void visit(ElementUnion el) {
		for (Iterator<Element> iterator = el.getElements().iterator(); iterator.hasNext();) {
			Element e = iterator.next();
			e.visit(this);
		}
	}
}
