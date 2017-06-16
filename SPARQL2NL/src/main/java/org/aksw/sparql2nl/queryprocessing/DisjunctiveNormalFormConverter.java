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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.Syntax;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementOptional;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementUnion;
import org.apache.jena.sparql.syntax.ElementVisitorBase;

public class DisjunctiveNormalFormConverter extends ElementVisitorBase {
	
	private boolean inUNION = false;
	private Stack<ElementUnion> unionStack = new Stack<>();
	
	private ElementUnion toAdd = null;
	
	private Map<ElementUnion, ElementGroup> union2Group = new HashMap<>();
	
	public org.apache.jena.query.Query getDisjunctiveNormalForm(org.apache.jena.query.Query query){
		Query copy = QueryFactory.create(query);
		copy.getQueryPattern().visit(this);
		
		return copy;
	}
	
	@Override
	public void visit(ElementUnion el) {
		unionStack.push(el);
		inUNION = true;
		for(Element e : el.getElements()){
			e.visit(this);
		}
		unionStack.pop();
		if(!unionStack.isEmpty()){
			toAdd = el;
//			ElementUnion parentUnion = unionStack.peek();
//			System.out.println("Parent UNION:\n " + parentUnion);
//			for(Element e : el.getElements()){
//				parentUnion.addElement(e);
//			}
		}
		if(toAdd != null && el != toAdd){
			ElementGroup group2Remove = union2Group.get(toAdd);
			group2Remove.getElements().remove(toAdd);
			el.getElements().remove(group2Remove);
			for(Element e1 : getUnionElements(toAdd)){
				ElementGroup g = new ElementGroup();
				for(Element e2 : group2Remove.getElements()){
					g.addElement(e2);
				}
				g.addElement(e1);
				el.addElement(g);
			}
			
			
		}
		inUNION = false;
	}
	
	private List<Element> getUnionElements(ElementUnion el){
		List<Element> elements = new ArrayList<>();
		for(Element e : el.getElements()){
			if(e instanceof ElementGroup){
//				if(((ElementGroup) e).getElements().size() == 1){
//					elements.add(((ElementGroup) e).getElements().get(0));
//				}
				elements.add(((ElementGroup) e).getElements().get(0));
			} else {
				elements.add(e);
			}
		}
//		for(Element ele : elements)System.out.println(ele.getClass());
		return elements;
	}
	
	@Override
	public void visit(ElementPathBlock el) {
		
	}
	
	@Override
	public void visit(ElementOptional el) {
		// TODO Auto-generated method stub
		super.visit(el);
	}
	
	@Override
	public void visit(ElementGroup el) {
		Element e;
		for(Iterator<Element> it = el.getElements().iterator(); it.hasNext();){
			e = it.next();
			if(e instanceof ElementUnion){
				union2Group.put((ElementUnion) e, el);
			}
			e.visit(this);
		}
	}
	
	public static void main(String[] args) {
		String queryString = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
				"PREFIX dbp: <http://dbpedia.org/property/> " +
				"PREFIX res: <http://dbpedia.org/resource/> " +
				"PREFIX yago: <http://dbpedia.org/class/yago/> " +
				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
				"SELECT DISTINCT ?uri ?string WHERE {" +
				"        { ?uri rdf:type yago:ArgentineFilms . }        " +
				"UNION	{ ?uri rdf:type dbo:Film .	" +
				"{ ?uri dbo:country res:Argentina . } " +
				"UNION " +
				"{ ?uri dbp:country 'Argentina'@en . } }	" +
				"OPTIONAL { ?uri rdfs:label ?string. FILTER (lang(?string) = 'en') }" +
				"}";
		
//		queryString = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
//				"PREFIX dbp: <http://dbpedia.org/property/> " +
//				"PREFIX res: <http://dbpedia.org/resource/> " +
//				"PREFIX yago: <http://dbpedia.org/class/yago/> " +
//				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
//				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
//				"SELECT DISTINCT ?uri ?string WHERE {" +
//				"?uri rdf:type dbo:Automobile .         " +
//				" { ?uri dbp:production res:Germany . } " +
//						"UNION " +
//				"{ ?uri dbp:assembly res:Germany . }        " +
//						"UNION { ?uri dbp:manufacturer ?x .       " +
//								" { ?x dbo:locationCountry res:Germany . } " +
//									"UNION " +
//								"{ ?x rdf:type yago:AutomotiveCompaniesOfGermany . } " +
//								"}        " +
//				"OPTIONAL { ?uri rdfs:label ?string. FILTER (lang(?string) = 'en') }}";
		System.out.println(QueryFactory.create(queryString, Syntax.syntaxARQ));
		Query q = QueryFactory.create(queryString, Syntax.syntaxARQ);
		q = new DisjunctiveNormalFormConverter().getDisjunctiveNormalForm(q);
		System.out.println(q.toString());
	}
	

}
