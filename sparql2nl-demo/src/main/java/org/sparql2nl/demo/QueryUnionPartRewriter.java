package org.sparql2nl.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.sparql.core.TriplePath;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.expr.E_Equals;
import com.hp.hpl.jena.sparql.expr.E_IRI;
import com.hp.hpl.jena.sparql.expr.ExprVar;
import com.hp.hpl.jena.sparql.expr.NodeValue;
import com.hp.hpl.jena.sparql.syntax.Element;
import com.hp.hpl.jena.sparql.syntax.ElementFilter;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.sparql.syntax.ElementOptional;
import com.hp.hpl.jena.sparql.syntax.ElementPathBlock;
import com.hp.hpl.jena.sparql.syntax.ElementTriplesBlock;
import com.hp.hpl.jena.sparql.syntax.ElementUnion;
import com.hp.hpl.jena.sparql.syntax.ElementVisitorBase;

public class QueryUnionPartRewriter extends ElementVisitorBase {
	
	private boolean inUnion;
	
	private Element element;
	
	public Query rewriteUnionParts(Query query){
		inUnion = false;
		Query copy = QueryFactory.create(query);
		copy.getQueryPattern().visit(this);
		return copy;
	}
	
	public Query rewriteUnionParts(String queryString){
		return rewriteUnionParts(QueryFactory.create(queryString, Syntax.syntaxARQ));
	}
	
	private Element rewriteElement(Element el){
		el.visit(this);
		return element;
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
		el.getOptionalElement().visit(this);
	}

	@Override
	public void visit(ElementTriplesBlock el) {
		for (Iterator<Triple> iterator = el.patternElts(); iterator.hasNext();) {
			Triple t = iterator.next();
		}
	}

	@Override
	public void visit(ElementPathBlock el) {
		for (Iterator<TriplePath> iterator = el.patternElts(); iterator.hasNext();) {
			TriplePath tp = iterator.next();
		}
	}

	@Override
	public void visit(ElementUnion el) {
		ElementRewriter er = new ElementRewriter();
		List<Element> rewrittenElements = new ArrayList<Element>();
		for (Iterator<Element> iterator = el.getElements().iterator(); iterator.hasNext();) {
			Element e = iterator.next();
			inUnion = true;
			rewrittenElements.add(er.rewriteElement(e));
			inUnion = false;
		}
//		el.getElements().clear();
//		for (Element element : rewrittenElements) {
//			el.addElement(element);
//		}
	}
	
	class ElementRewriter  extends ElementVisitorBase{
		private Map<Node, Var> node2Var = new HashMap<Node, Var>();
		private Element element;
		private int cnt = 1;
		
		private Set<ElementFilter> filtersToAdd = new HashSet<ElementFilter>(); 
		
		public Element rewriteElement(Element el){
			el.visit(this);
			return element;
		}
		
		private Node getVar(Node node){
			if(node.isVariable()){
				return node;
			} 
			Var var = node2Var.get(node);
			if(var == null){
				var = Var.alloc("var" + cnt++);
				node2Var.put(node, var);
				filtersToAdd.add(new ElementFilter(new E_Equals(new ExprVar(var), NodeValue.makeNode(node))));
			}
			return var;
		}
		
		@Override
		public void visit(ElementGroup el) {
			for (Iterator<Element> iterator = new ArrayList<Element>(el.getElements()).iterator(); iterator.hasNext();) {
				Element e = iterator.next();
				e.visit(this);
				for (Iterator<ElementFilter> iterator2 = filtersToAdd.iterator(); iterator2.hasNext();) {
					ElementFilter filter = iterator2.next();
					el.addElementFilter(filter);
					iterator2.remove();
				}
			}
			
			
		}

		@Override
		public void visit(ElementOptional el) {
			el.getOptionalElement().visit(this);
		}

		@Override
		public void visit(ElementTriplesBlock el) {
			List<Triple> triples = new ArrayList<Triple>();
			for (Iterator<Triple> iterator = el.patternElts(); iterator.hasNext();) {
				Triple t = iterator.next();
				triples.add(Triple.create(t.getSubject(), t.getPredicate(), getVar(t.getObject())));
				iterator.remove();
			}
			for (Triple triple : triples) {
				el.addTriple(triple);
			}
		}

		@Override
		public void visit(ElementPathBlock el) {
			List<TriplePath> triples = new ArrayList<TriplePath>();
			for (Iterator<TriplePath> iterator = el.patternElts(); iterator.hasNext();) {
				TriplePath tp = iterator.next();
				triples.add(new TriplePath(tp.getSubject(), tp.getPath(), getVar(tp.getObject())));
				iterator.remove();
			}
			for (TriplePath tp : triples) {
				el.addTriple(tp);
			}
		}

		@Override
		public void visit(ElementUnion el) {
			List<Element> rewrittenElements = new ArrayList<Element>();
			for (Iterator<Element> iterator = el.getElements().iterator(); iterator.hasNext();) {
				Element e = iterator.next();
				e.visit(this);
			}
//			el.getElements().clear();
//			for (Element element : rewrittenElements) {
//				el.addElement(element);
//			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		String query = "PREFIX  rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
				"PREFIX  res:  <http://dbpedia.org/resource/> " +
				"PREFIX  dbo:  <http://dbpedia.org/ontology/> " +
				"PREFIX  xsd:  <http://www.w3.org/2001/XMLSchema#> " +
				"PREFIX  rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
				"SELECT DISTINCT  ?person WHERE  " +
				"{ ?person rdf:type dbo:Person      " +
				"	   { ?person dbo:occupation res:Writer {?person dbo:instrument res:Singing} UNION {?person dbo:instrument res:Guitar} }    " +
				"UNION" +
				"      { ?person dbo:occupation res:Surfing }    " +
				"?person dbo:birthDate ?date    " +
				"FILTER ( ?date > \"1950\"^^xsd:date )  }"	;
		
		Query rewrittenQuery = new QueryUnionPartRewriter().rewriteUnionParts(query);
		System.out.println(rewrittenQuery);
	}
	
}
