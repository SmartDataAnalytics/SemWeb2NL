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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.Syntax;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.E_Bound;
import org.apache.jena.sparql.expr.E_LogicalNot;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementOptional;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementTriplesBlock;
import org.apache.jena.sparql.syntax.ElementUnion;
import org.apache.jena.sparql.syntax.ElementVisitorBase;
import org.apache.jena.sparql.util.VarUtils;

public class NegatedElementsExtractor extends ElementVisitorBase{
	
	ElementGroup currentGroup;
	
	Map<ElementGroup, Var> elementGroup2Var = new HashMap<>();
	
	public void getNegatedElements(Query query){
		query.getQueryPattern().visit(this);
		
		TriplePatternExtractor tpExtractor = new TriplePatternExtractor();
		
		for(Entry<ElementGroup, Var> entry : elementGroup2Var.entrySet()){
			Var var = entry.getValue();
			Set<Triple> triples = tpExtractor.extractTriplePattern(entry.getKey());
			for(Triple t : triples){
				if(VarUtils.getVars(t).contains(var)){
					System.out.println(t);
				}
			}
		}
		
	}
	
	@Override
	public void visit(ElementGroup el) {
		currentGroup = el;
		for (Iterator<Element> iterator = el.getElements().iterator(); iterator.hasNext();) {
			Element e = iterator.next();
			e.visit(this);
		}
			
	}

	@Override
	public void visit(ElementTriplesBlock el) {
		for (Iterator<Triple> iter = el.patternElts(); iter.hasNext();) {
			Triple t = iter.next();
			
		}
	}

	@Override
	public void visit(ElementPathBlock el) {
		for (Iterator<TriplePath> iter = el.patternElts(); iter.hasNext();) {
			TriplePath tp = iter.next();
			
		}
	}
	
	@Override
	public void visit(ElementUnion el) {
		for (Iterator<Element> iterator = el.getElements().iterator(); iterator.hasNext();) {
			Element e = iterator.next();
			e.visit(this);
			
		}
	}
	
	@Override
	public void visit(ElementOptional el) {
		el.getOptionalElement().visit(this);
	}
	
	@Override
	public void visit(ElementFilter el) {
		Expr expr = el.getExpr();
		if(expr instanceof E_LogicalNot){
			Expr not = ((E_LogicalNot) expr).getArg(1);
			if( not instanceof E_Bound){
				elementGroup2Var.put(currentGroup, ((E_Bound) not).getArg().asVar());
			}
		}
		
	}
	
	public static void main(String[] args) {
		String querya = "PREFIX  rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
                + "PREFIX  res:  <http://dbpedia.org/resource/> "
                + "PREFIX  dbo:  <http://dbpedia.org/ontology/> "
                + "PREFIX  dbp:  <http://dbpedia.org/property/> "
                + "PREFIX  rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
                + " "
                + "ASK "
                + "WHERE "
                + "  {   { res:Batman_Begins dbo:starring res:Christian_Bale. FILTER(!BOUND(?b)) } "
                + "    UNION "
                + "      { res:Batman_Begins dbp:starring ?b. res:Batman_Begins dbo:starring ?c. FILTER(!BOUND(?b)) } "
                + "  }";
		
		querya = "PREFIX  rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
                + "PREFIX  res:  <http://dbpedia.org/resource/> "
                + "PREFIX  dbo:  <http://dbpedia.org/ontology/> "
                + "PREFIX  dbp:  <http://dbpedia.org/property/> "
                + "PREFIX  rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
                + " "
                + "ASK "
                + "WHERE "
                + "  {  res:Batman_Begins dbo:starring res:Christian_Bale. FILTER(!BOUND(?b)) } "
                + "  }";
		
		Query q = QueryFactory.create(querya, Syntax.syntaxARQ);
		new NegatedElementsExtractor().getNegatedElements(q);
	}
	
	

}
