package org.sparql2nl.demo;

import org.apache.jena.graph.Triple;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.expr.E_Equals;
import org.apache.jena.sparql.expr.E_Str;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.Template;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.apache.log4j.Logger;
import org.dllearner.kb.sparql.SparqlEndpoint;

import java.io.StringWriter;
import java.util.Iterator;
import java.util.Set;

public class SPARQLExplain {
	
	private static final Logger logger = Logger.getLogger(SPARQLExplain.class);

	private org.aksw.jena_sparql_api.core.QueryExecutionFactory qef;

	public SPARQLExplain(org.aksw.jena_sparql_api.core.QueryExecutionFactory qef) {
		this.qef = qef;
	}
	
	public void getExplanation(String query, String resource) {
		// parse the query
		Query q = QueryFactory.create(query, Syntax.syntaxARQ);

		// create a CONSTRUCT query
		Query constructQuery = new Query();
		constructQuery.setQueryConstructType();
		// create the template part
		Set<Triple> triples = new TriplePatternExtractor()
				.extractTriplePattern(q);
		BasicPattern bgp = new BasicPattern();
		for (Triple triple : triples) {
			bgp.add(triple);
		}
		Template template = new Template(bgp);
		constructQuery.setConstructTemplate(template);
		// get the WHERE part and add a FILTER to bind the resource to the
		// variable
		Element wherePart = q.getQueryPattern();
		// get the corresponding variable in the query
		String var = getVariable(query, resource);
		((ElementGroup) wherePart).addElementFilter(new ElementFilter(
				new E_Equals(new ExprVar(var), NodeValue
						.makeNode(ResourceFactory.createResource(resource)
								.asNode()))));
		constructQuery.setQueryPattern(wherePart);

		System.out.println(constructQuery);

		StringWriter sw = new StringWriter();
		Model explanation = executeConstruct(constructQuery);
		explanation.write(sw, "TURTLE");

		String explanationString = sw.toString();
		while (explanationString.indexOf("@prefix") >= 0) {
			explanationString = explanationString.substring(explanationString
					.indexOf("\n") + 1);
		}

		System.out.println(explanationString.trim());

	}
	
	public Model getExplanation2(Query query, QuerySolution qs){
		Query copy = QueryFactory.create(query);
		copy.setQueryResultStar(true);
		ParameterizedSparqlString template = new ParameterizedSparqlString(copy.toString());
		QuerySolutionMap qmap = new QuerySolutionMap();
		qmap.addAll(qs);
		template.setParams(qmap);
		System.out.println(template.asQuery());
		QueryExecution qe = qef.createQueryExecution(template.asQuery());
		ResultSet rs = qe.execSelect();
		QuerySolution qs2;
		if(rs.hasNext()){
			qs2 = rs.next();
			qmap.addAll(qs2);
			template.setParams(qmap);
			Query filledQuery = template.asQuery();
			TriplePatternExtractor extractor = new TriplePatternExtractor();
			Model explanation = ModelFactory.createDefaultModel();
			for(Triple t : extractor.extractTriplePattern(filledQuery)){
				explanation.add(explanation.asStatement(t));
			}
			return explanation;
		}
		return null;
		
	}
	
	public Model getExplanation2(String queryString, QuerySolution qs){
		return getExplanation2(QueryFactory.create(queryString), qs);
	}

	public Model getExplanation(Query query, QuerySolution qs) {
		QueryUnionPartRewriter unionPartRewriter = new QueryUnionPartRewriter();
		Query copy = QueryFactory.create(query);
		copy = unionPartRewriter.rewriteUnionParts(query);
		// create a CONSTRUCT query
		Query constructQuery = new Query();
		constructQuery.setQueryConstructType();
		// create the template part
		Set<Triple> triples = new TriplePatternExtractor()
				.extractTriplePattern(copy);
		BasicPattern bgp = new BasicPattern();
		for (Triple triple : triples) {
			bgp.add(triple);
		}
		Template template = new Template(bgp);
		constructQuery.setConstructTemplate(template);
		// get the WHERE part
		Element wherePart = copy.getQueryPattern();
		// add FILTERs to bind the result to the variable if it is SELECT query
		if (copy.isSelectType()) {
			for (Iterator<String> iterator = qs.varNames(); iterator.hasNext();) {
				String var = iterator.next();
				RDFNode node = qs.get(var);
				NodeValue nv = null;
				if(node.isURIResource()){
					nv = NodeValue.makeNode(node.asNode());
					((ElementGroup) wherePart).addElementFilter(new ElementFilter(
							new E_Equals(new ExprVar(var), nv)));
				} else if(node.isLiteral()){
					nv = NodeValue.makeNodeString(node.asLiteral().getLexicalForm());
					((ElementGroup) wherePart).addElementFilter(new ElementFilter(
							new E_Equals(new E_Str(new ExprVar(var)), nv)));
				}
			}
		}

		constructQuery.setQueryPattern(wherePart);
		
		
		constructQuery.setLimit(1);
		
		logger.info("Generated CONSTRUCT query:\n" + constructQuery);

		Model explanation = executeConstruct(constructQuery);

		return explanation;
	}

	public Model getExplanation(String queryString, QuerySolution qs) {
		// parse the query
		Query query = QueryFactory.create(queryString, Syntax.syntaxARQ);
		return getExplanation(query, qs);
	}

	private String getVariable(String query, String resource) {
		ResultSet rs = qef.createQueryExecution(query).execSelect();
		QuerySolution qs;
		while (rs.hasNext()) {
			qs = rs.next();
			for (Iterator<String> iterator = qs.varNames(); iterator.hasNext();) {
				String var = iterator.next();
				if (qs.getResource(var).getURI().equals(resource)) {
					return var;
				}
			}
		}
		return null;
	}

	private Model executeConstruct(Query query) {
		QueryExecution qe = qef.createQueryExecution(query);
		Model model = qe.execConstruct();
		qe.close();
		return model;
	}

	public static void main(String[] args) {
		SparqlEndpoint endpoint = SparqlEndpoint.getEndpointDBpedia();
		String query = "SELECT DISTINCT ?uri WHERE {?uri <http://dbpedia.org/ontology/birthPlace> ?place. "
				+ "?place <http://dbpedia.org/property/population> ?pop. OPTIONAL{?uri a ?type}"
				+ "FILTER(?pop > 2000000)" + "} LIMIT 2";
		
		query = "PREFIX  rdfs: <http://www.w3.org/2000/01/rdf-schema#> PREFIX  res:  <http://dbpedia.org/resource/>"+
"PREFIX  foaf: <http://xmlns.com/foaf/0.1/>"+
"PREFIX  dbo:  <http://dbpedia.org/ontology/>"+
"PREFIX  rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"+
"SELECT DISTINCT  ?date "+
"WHERE"+
 " { res:Charmed dbo:starring ?actor ."+
  "  ?actor dbo:birthDate ?date"+
"  }";	
		
		query = "PREFIX  dbo:  <http://dbpedia.org/ontology/> PREFIX  res:  <http://dbpedia.org/resource/> SELECT DISTINCT  ?uri WHERE  { ?x a dbo:Album .?x dbo:artist res:Elvis_Presley .?x dbo:releaseDate ?y .?x dbo:recordLabel ?uri  } ORDER BY ASC(?y) LIMIT 1";
//		System.out.println(QueryFactory.create(query, Syntax.syntaxARQ));
//		ResultSet rs = new SparqlQuery(query, endpoint).send();
//		QuerySolution qs;
//		while (rs.hasNext()) {
//			qs = rs.next();
//			System.out.println(new SPARQLExplain(endpoint).getExplanation(query, qs));
//			System.out.println(new SPARQLExplain(endpoint).getExplanation2(query, qs));
//		}
		
		query = "PREFIX  rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
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
		QuerySolutionMap qsMap = new QuerySolutionMap();
		qsMap.add("person", new ResourceImpl("http://dbpedia.org/resource/Ana_Voog"));
		QueryExecutionFactoryHttp qef = new org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp(endpoint.getURL().toString(), endpoint.getDefaultGraphURIs());
		System.out.println(new SPARQLExplain(qef).getExplanation2(query, qsMap));
		System.out.println(new SPARQLExplain(qef).getExplanation(query, qsMap));
	}
}
