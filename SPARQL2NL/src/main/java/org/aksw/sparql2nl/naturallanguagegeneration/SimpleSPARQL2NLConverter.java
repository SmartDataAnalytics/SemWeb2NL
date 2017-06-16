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
/**
 * 
 */
package org.aksw.sparql2nl.naturallanguagegeneration;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.aksw.jena_sparql_api.cache.core.QueryExecutionFactoryCacheEx;
import org.aksw.jena_sparql_api.cache.extra.CacheFrontend;
import org.aksw.jena_sparql_api.cache.h2.CacheUtilsH2;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.sparql2nl.naturallanguagegeneration.graph.Edge;
import org.aksw.sparql2nl.naturallanguagegeneration.graph.QueryGraphGenerator;
import org.aksw.sparql2nl.queryprocessing.TriplePatternExtractor;
import org.aksw.triple2nl.property.PredicateAsNounConversionType;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.reasoning.SPARQLReasoner;
import org.jgrapht.event.ConnectedComponentTraversalEvent;
import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.TraversalListener;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.traverse.DepthFirstIterator;
import org.jgrapht.traverse.GraphIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import simplenlg.features.InternalFeature;
import simplenlg.framework.CoordinatedPhraseElement;
import simplenlg.framework.DocumentElement;
import simplenlg.framework.NLGElement;
import simplenlg.framework.NLGFactory;
import simplenlg.framework.PhraseElement;
import simplenlg.lexicon.Lexicon;
import simplenlg.lexicon.NIHDBLexicon;
import simplenlg.phrasespec.NPPhraseSpec;
import simplenlg.realiser.english.Realiser;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

/**
 * @author Lorenz Buehmann
 *
 */
public class SimpleSPARQL2NLConverter implements Sparql2NLConverter{
	
	private static final Logger logger = LoggerFactory.getLogger(SimpleSPARQL2NLConverter.class);
	
	public static final String SPARQL2NL_NS = "http://sparql2nl.aksw.org/";

	private static final Node ENTITY_NODE_TYPE = NodeFactory.createURI(SPARQL2NL_NS + "entity");
	private static final Node LITERAL_NODE_TYPE = NodeFactory.createURI(SPARQL2NL_NS + "value");
	
	private TriplePatternConverter triplePatternConverter;
	private NLGFactory nlgFactory;
	private Realiser realiser;


	private SPARQLReasoner reasoner;


	private QueryExecutionFactory qef;

	private Map<Node, NLGElement> var2Description;

	private Map<Node, NLGElement> var2TypeDescription;

	private Map<Node, List<Node>> varTypes;

	public SimpleSPARQL2NLConverter(SparqlEndpoint endpoint, String cacheDirectory) {
		this(endpoint, cacheDirectory, Lexicon.getDefaultLexicon());
	}
	
	public SimpleSPARQL2NLConverter(SparqlEndpoint endpoint, String cacheDirectory, Lexicon lexicon) {
		qef = new QueryExecutionFactoryHttp(endpoint.getURL().toString(), endpoint.getDefaultGraphURIs());
		if(cacheDirectory != null){
			CacheFrontend cacheFrontend = CacheUtilsH2.createCacheFrontend("./sparql2nl", false, TimeUnit.DAYS.toMillis(7));
			qef = new QueryExecutionFactoryCacheEx(qef, cacheFrontend);
		}
		reasoner = new SPARQLReasoner(qef);
		nlgFactory = new NLGFactory(lexicon);
		realiser = new Realiser(lexicon);
		triplePatternConverter = new TriplePatternConverter(qef, cacheDirectory, lexicon);
	}

	/* (non-Javadoc)
	 * @see org.aksw.sparql2nl.naturallanguagegeneration.Sparql2NLConverter#getNLR(org.apache.jena.query.Query)
	 */
	@Override
	public String getNLR(Query query) {
		//1. convert query to graph
		QueryGraphGenerator graphGenerator = new QueryGraphGenerator();
		graphGenerator.setIgnoreOptionalPatterns(true);
		final DirectedMultigraph<Node, Edge> queryGraph = graphGenerator.generateQueryGraph(query);
//		QueryGraphGenerator.showGraph(queryGraph);
		
		//2. determine root variable
		List<Var> projectVars = query.getProjectVars();
		
		varTypes = new HashMap<>();
		var2TypeDescription = new HashMap<>();
		for (Node node : queryGraph.vertexSet()) {
			if(node.isVariable()){
				var2TypeDescription.put(node, getVarType(node, queryGraph));
			}
		}
		
		//determine root
		final Var rootVar = determineRootVar(query);
		
		//transform graph
//		QueryGraphGenerator.showGraph(queryGraph);
		graphGenerator.transform(queryGraph, rootVar);
//		QueryGraphGenerator.showGraph(queryGraph);
		
		var2Description = new HashMap<>();
	
		GraphIterator<Node, Edge> iterator = new DepthFirstIterator<>(queryGraph, rootVar.asNode());
		iterator.addTraversalListener(new TraversalListener<Node, Edge>() {
			
			@Override
			public void vertexTraversed(VertexTraversalEvent<Node> e) {
//				logger.info("Processing node " + e.getVertex());
			}
			
			@Override
			public void vertexFinished(VertexTraversalEvent<Node> e) {
				logger.info("Computing description for node " + e.getVertex() + "...");
				computeDescription(e.getVertex(), queryGraph);
				logger.info("...done.");
			}
			
			@Override
			public void edgeTraversed(EdgeTraversalEvent<Node, Edge> e) {
			}
			
			@Override
			public void connectedComponentStarted(ConnectedComponentTraversalEvent e) {
				logger.info("Started");
			}
			
			@Override
			public void connectedComponentFinished(ConnectedComponentTraversalEvent e) {
				logger.info("Finished");
			}
		});
        while (iterator.hasNext()) {
        	iterator.next();
        }
        
        //4. verbalize
        NLGElement description = var2Description.get(rootVar);
		return realiser.realiseSentence(description);
	}
	
	private NLGElement getVarType(Node varNode, DirectedMultigraph<Node, Edge> graph){
		logger.info("Determining type of " + varNode + "...");
		//check for type paths
		List<Node> types = new ArrayList<>();
		Set<Edge> edges = graph.outgoingEdgesOf(varNode);
		for (Edge edge : edges) {
			Node predicate = edge.getPredicateNode();
			if(predicate.equals(RDF.type.asNode())){
				Node typeNode = graph.getEdgeTarget(edge);
				//TODO check if type is var and resolve the rdfs:subClassOf path
				types.add(typeNode);
			}
		}
		//try to get type via outgoing property domains/incoming property ranges
		if(types.isEmpty()){
			//check the range of incoming edges
			Set<Edge> incomingEdges = graph.incomingEdgesOf(varNode);
			for (Edge edge : incomingEdges) {
				Node predicate = edge.asTriple().getPredicate();
				Model model = qef.createQueryExecution("DESCRIBE <" + predicate.getURI() + ">").execDescribe();
				StmtIterator iter = model.listStatements(null, RDFS.range, (RDFNode)null);
				while(iter.hasNext()){
					logger.info("Range: " + iter.next().getObject());
				}
			}
			//check the domain of outgoing edges
			
		}
		//check if is literal or resource
		if(types.isEmpty()){
			//if resource
			Set<Edge> incomingEdges = graph.incomingEdgesOf(varNode);
			boolean objectProperty = false;
			boolean dataProperty = false;
			for (Edge edge : incomingEdges) {
				objectProperty = reasoner.isObjectProperty(edge.asTriple().getPredicate().getURI());
				dataProperty = reasoner.isDataProperty(edge.asTriple().getPredicate().getURI());
			}
			if(dataProperty && !objectProperty){
				types.add(LITERAL_NODE_TYPE);
			} else {
				types.add(ENTITY_NODE_TYPE);
			}
		} 
		logger.info("Type(" + varNode + ")=" + types);
		varTypes.put(varNode, types);
		//pick one type
		//TODO how to handle multiple types?
		NLGElement element = triplePatternConverter.processClassNode(types.iterator().next(), false);
		element.setPlural(true);
		return element;
	}

	/* (non-Javadoc)
	 * @see org.aksw.sparql2nl.naturallanguagegeneration.Sparql2NLConverter#convert2NLE(org.apache.jena.query.Query)
	 */
	@Override
	public DocumentElement convert2NLE(Query query) {
		//1. convert query to graph
		
		return null;
	}
	
	private void computeDescription(Node node, DirectedMultigraph<Node, Edge> graph){
		boolean plural = true;
		
		List<PhraseElement> phrases = new ArrayList<>();
		for (Edge edge : graph.outgoingEdgesOf(node)) {
			Triple triple = edge.asTriple();
			//we handle rdf:type triples separately
			if(!triple.predicateMatches(RDF.type.asNode())){
				//use the already computed verbalization of the object
				NLGElement objectElement = null;
				//get the already computed description of the variable if exist
				if (triple.getObject().isVariable()) {
					if(var2Description.containsKey(triple.getObject())){
						objectElement = var2Description.get(triple.getObject());
					}
				} else {
					objectElement = triplePatternConverter.processNode(triple.getObject());
				}
				
				
				//for triple patterns where the subject is literal (this is possible due to graph transformation)
				//we try to use "v(PREDICATE)s of v(OBJECT)" instead of "VALUES that are v(PREDICATE) of v(OBJECT)"
				if(varTypes.get(triple.getSubject()).iterator().next() == LITERAL_NODE_TYPE){
					NPPhraseSpec phrase = triplePatternConverter.convertTriplePatternCompactOfForm(triple, objectElement);
					phrases.add(phrase);
				} else {
					NLGElement subjectElement = var2TypeDescription.get(triple.getSubject());
					
					NPPhraseSpec phrase = triplePatternConverter.convertTriplePattern(edge.asTriple(), subjectElement, objectElement, plural, false, edge.isReverted(), PredicateAsNounConversionType.RELATIVE_CLAUSE_PRONOUN);
					phrases.add(phrase);
//					logger.info(realiser.realise(phrase).getRealisation());
				}
			}
		}
		
		//combine the phrases we got for each outgoing triple pattern if we have more than 1
		if(phrases.size() == 0){
			//TODO
		} else if(phrases.size() == 1){
			var2Description.put(node, phrases.get(0));
		} else {
	        
			//combine the complements
			CoordinatedPhraseElement cp = nlgFactory.createCoordinatedPhrase();
			for (PhraseElement phrase : phrases) {
				cp.addCoordinate(phrase.getFeatureAsElementList(InternalFeature.COMPLEMENTS).get(0));
			}
			//create new NP with 
			NPPhraseSpec np = nlgFactory.createNounPhrase();
			NLGElement head = phrases.get(0).getHead();
			np.setHead(head);
			head.setPlural(plural);
			np.setPlural(plural);
			np.setComplement(cp);
			var2Description.put(node, np);
		}
	}
	
	private Var determineRootVar(Query query){
		List<Var> candidateVars = new ArrayList<>(query.getProjectVars());
		
		if(candidateVars.size() == 1){
			return candidateVars.get(0);
		}
		
		TriplePatternExtractor extractor = new TriplePatternExtractor();
		Map<Var, Set<Triple>> outgoingTriplePatterns = extractor.extractOutgoingTriplePatternsForProjectionVars(query);
		Map<Var, Set<Triple>> incomingTriplePatterns = extractor.extractIncomingTriplePatternsForProjectionVars(query);
		Set<Triple> optionalTriplePatterns = extractor.getOptionalTriplePatterns();
		
		// filter out OPTIONAL only vars
		for (Iterator<Var> iterator = candidateVars.iterator(); iterator.hasNext();) {
			Var var = iterator.next();
			
			//remove if occurs only in OPTIONAL
			Set<Triple> nonOptionalTriplePatterns = extractor.extractNonOptionalTriplePatterns(query, var);
			if(nonOptionalTriplePatterns.isEmpty()){
				iterator.remove();
			}
		}
		//prefer vars with rdf:type
		for (Iterator<Var> iterator = candidateVars.iterator(); iterator.hasNext();) {
			Var var = iterator.next();
			boolean isTyped = false;
			for (Triple tp : outgoingTriplePatterns.get(var)) {
				if(tp.predicateMatches(RDF.type.asNode())){
					isTyped = true;
					break;
				}
			};
			if(!isTyped){
				iterator.remove();
			}
		}
		//prefer vars with label
		if(candidateVars.size() > 1){
			for (Iterator<Var> iterator = candidateVars.iterator(); iterator.hasNext();) {
				Var var = iterator.next();
				boolean hasLabel = false;
				for (Triple tp : outgoingTriplePatterns.get(var)) {
					if (tp.predicateMatches(RDFS.label.asNode())) {
						hasLabel = true;
						break;
					}
				}
				;
				if (!hasLabel) {
					iterator.remove();
				}
			}
		}
		
		logger.info("Remaining candidates:" + candidateVars);
		return candidateVars.get(0);
	}
	
	public static void main(String[] args) throws Exception {
		Query query = QueryFactory.create(
				"PREFIX : <http://sparql2nl.aksw.org/> PREFIX rdfs: <" + RDFS.getURI() + "> "
				+ "SELECT ?state ?city ?label WHERE "
				+ "{"
				+ "?city :isLocatedIn ?state . "
				+ "?city a :City . "
				+ "?state a :State ."
				+ "?city :mayor ?mayor . "
				+ "?state :systemOfGovernment :Republic ."
				+ "?state :climateZone :Subtropics ."
				+ " OPTIONAL{?city rdfs:label ?label}}"
						);
//		query = QueryFactory.create(
//				"PREFIX : <http://sparql2nl.aksw.org/> SELECT DISTINCT  ?uri			"
//				+ "	WHERE				  { ?uri :isLocatedIn :Germany .				    ?uri a :City				  }");
		
		query = QueryFactory.create(
				"PREFIX : <http://sparql2nl.aksw.org/> SELECT DISTINCT  ?uri WHERE { :Mike :isKnownFor ?uri .}");
		
		Lexicon lexicon = new NIHDBLexicon("/home/me/tools/lexAccess2013lite/data/HSqlDb/lexAccess2013.data");
		SparqlEndpoint endpoint = SparqlEndpoint.getEndpointDBpedia();
		SimpleSPARQL2NLConverter sparql2nlConverter = new SimpleSPARQL2NLConverter(endpoint, "cache/sparql2nl", lexicon);
		String nlr = sparql2nlConverter.getNLR(query);
		logger.info(nlr);
		
		SimpleNLGwithPostprocessing snlg = new SimpleNLGwithPostprocessing(endpoint);
		nlr = snlg.getNLR(query);
		logger.info(nlr);
	}

}
