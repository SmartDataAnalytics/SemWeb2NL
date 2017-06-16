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
package org.aksw.avatar.dataset;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;

import org.aksw.avatar.clustering.Node;
import org.aksw.avatar.clustering.WeightedGraph;
import org.aksw.avatar.exceptions.NoGraphAvailableException;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.apache.log4j.Logger;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.reasoning.SPARQLReasoner;
import org.dllearner.utilities.MapUtils;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import simplenlg.framework.NLGFactory;
import simplenlg.lexicon.Lexicon;
import simplenlg.phrasespec.SPhraseSpec;
import simplenlg.realiser.english.Realiser;
import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectPropertyImpl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

/**
 * @author Lorenz Buehmann
 *
 */
public class DatasetBasedGraphGenerator {
	
	public enum Cooccurrence {
        PROPERTIES, TRIPLESTORE
    }
    
    public enum Direction {
    	OUTGOING, INCOMING
    }
	
	private static final Logger logger = Logger.getLogger(DatasetBasedGraphGenerator.class);

    private QueryExecutionFactory qef;
    protected SPARQLReasoner reasoner;
    private Set<String> blacklist = Sets.newHashSet(
    		"http://dbpedia.org/ontology/wikiPageExternalLink", 
    		"http://dbpedia.org/ontology/abstract",
            "http://dbpedia.org/ontology/thumbnail", 
            "http://dbpedia.org/ontology/wikiPageID",
            "http://dbpedia.org/ontology/wikiPageRevisionID",
            "http://dbpedia.org/ontology/wikiPageRedirects",
            "http://dbpedia.org/ontology/wikiPageDisambiguates",
            "http://dbpedia.org/ontology/individualisedPnd");
    
    boolean useIncomingProperties = false;
    
    Map<OWLClass, Set<OWLObjectProperty>> class2OutgoingProperties;

	private String cacheDirectory;

    public DatasetBasedGraphGenerator(SparqlEndpoint endpoint) {
        this(endpoint, (String)null);
    }

    public DatasetBasedGraphGenerator(QueryExecutionFactory qef, String cacheDirectory) {
        this.qef = qef;
        
		init();
    }
    
    public DatasetBasedGraphGenerator(QueryExecutionFactory qef, File cacheDirectory) {
        this(qef, cacheDirectory.getPath());
    }
    
    public DatasetBasedGraphGenerator(SparqlEndpoint endpoint, String cacheDirectory ) {
       this(new QueryExecutionFactoryHttp(endpoint.getURL().toString(), endpoint.getDefaultGraphURIs()), cacheDirectory);
    }
    
    public DatasetBasedGraphGenerator(SparqlEndpoint endpoint, File cacheDirectory ) {
    	this(new QueryExecutionFactoryHttp(endpoint.getURL().toString(), endpoint.getDefaultGraphURIs()), cacheDirectory.getPath());
    }
    
    private void init(){
        
//		qef = new QueryExecutionFactoryPaginated(qef, 10000);
//		qef = new QueryExecutionFactoryDelay(qef, 500);

        reasoner = new SPARQLReasoner(qef);
        
        class2OutgoingProperties = new HashMap<OWLClass, Set<OWLObjectProperty>>();
    }
    
    /**
     * @param personTypes the personTypes to set
     */
    public void setPropertiesBlacklist(Set<String> blacklist) {
        this.blacklist = blacklist;
    }

    public Map<OWLClass, WeightedGraph> generateGraphs(double threshold) {
        return generateGraphs(threshold, null);
    }

    public Map<OWLClass, WeightedGraph> generateGraphs(double threshold, String namespace) {
        Map<OWLClass, WeightedGraph> graphs = new HashMap<OWLClass, WeightedGraph>();

        // get all classes in knowledge base with given prefix
        Set<OWLClass> classes = reasoner.getTypes(namespace, true);

        // generate a weighted graph for each class
        for (OWLClass cls : classes) {
            try {
            	WeightedGraph wg = generateGraph(cls, threshold, namespace);
				graphs.put(cls, wg);
			} catch (NoGraphAvailableException e) {
				e.printStackTrace();
			}
        }

        return graphs;
    }

    public WeightedGraph generateGraph(OWLClass cls, double threshold) throws NoGraphAvailableException {
        return generateGraph(cls, threshold, null);
    }

    public WeightedGraph generateGraph(OWLClass cls, double threshold, String namespace) throws NoGraphAvailableException {
        return generateGraph(cls, threshold, namespace, Cooccurrence.TRIPLESTORE);
    }

    public WeightedGraph generateGraph(OWLClass cls, double threshold, String namespace, Cooccurrence c) throws NoGraphAvailableException {
    	
    	// check if class is empty
    	int individualsCount = reasoner.getIndividualsCount(cls);
    	
    	if(individualsCount == 0) {
    		return null;
    	}
    	
        //get the outgoing properties with a prominence score above threshold
        final SortedSet<OWLObjectProperty> outgoingProperties = getMostProminentProperties(cls, threshold, namespace, Direction.OUTGOING);
        class2OutgoingProperties.put(cls, outgoingProperties);
        
        //get the incoming properties with a prominence score above threshold
        SortedSet<OWLObjectProperty> incomingProperties = new TreeSet<OWLObjectProperty>();
        if(useIncomingProperties){
        	incomingProperties = getMostProminentProperties(cls, threshold, namespace, Direction.INCOMING);
        } 
        
        //add properties that have as domain the class
//        outgoingProperties.addAll(reasoner.getObjectPropertiesWithDomain(cls));
        
        Set<OWLObjectProperty> allRelevantProperties = Sets.union(outgoingProperties, incomingProperties);
        
        //compute the frequency for each pair of properties
        Map<Set<OWLObjectProperty>, Double> cooccurrences;
        if (c.equals(Cooccurrence.TRIPLESTORE)) {
            cooccurrences = getCooccurrences(cls, outgoingProperties, incomingProperties);
        } else {
            cooccurrences = getPropertySimilarities(cls, allRelevantProperties);
        }

        //create the weighted graph
        WeightedGraph wg = new WeightedGraph();
        LoadingCache<OWLObjectProperty, Node> property2Node = CacheBuilder.newBuilder().maximumSize(allRelevantProperties.size()).build(
                new CacheLoader<OWLObjectProperty, Node>() {

                    public Node load(OWLObjectProperty property) {
                        return outgoingProperties.contains(property) ? new Node(property.toStringID()) : new Node(property.toStringID(), false);
                    }
                });
        for (Entry<Set<OWLObjectProperty>, Double> entry : cooccurrences.entrySet()) {
            Set<OWLObjectProperty> pair = entry.getKey();
            Double frequency = entry.getValue();

            Iterator<OWLObjectProperty> iterator = pair.iterator();
            OWLObjectProperty property1 = iterator.next();
            OWLObjectProperty property2 = iterator.next();

            try {
            	Node node1 = property2Node.get(property1);
                Node node2 = property2Node.get(property2);
                wg.addNode(node1, 0d);
                wg.addNode(node2, 0d);
                if (frequency > 0) {
                    
                    wg.addEdge(node1, node2, frequency);
                    if (c.equals(Cooccurrence.PROPERTIES)) {
                        wg.addEdge(node2, node1, frequency);
                    }
                }
            } catch (ExecutionException e) {
                logger.error(e, e);
            }
        }
        if(allRelevantProperties.size() == 1){
        	wg.addNode(new Node(allRelevantProperties.iterator().next().toStringID()), 1d);
        }
        return wg;
    }
    
    public boolean isOutgoingProperty(OWLClass cls, OWLObjectProperty property){
    	return class2OutgoingProperties.containsKey(cls) && class2OutgoingProperties.get(cls).contains(property);
    }

    private Map<Set<OWLObjectProperty>, Double> getCooccurrences(OWLClass cls, Set<OWLObjectProperty> properties) {
        Map<Set<OWLObjectProperty>, Double> pair2Frequency = new HashMap<Set<OWLObjectProperty>, Double>();
        //compute the frequency for each pair
        ResultSet rs;
        Set<OWLObjectProperty> pair;
        for (OWLObjectProperty prop1 : properties) {
            for (OWLObjectProperty prop2 : properties) {
                if (!prop1.equals(prop2) && !pair2Frequency.containsKey(pair = Sets.newHashSet(prop1, prop2))) {
                    String query = "SELECT (COUNT(DISTINCT ?s) AS ?cnt) WHERE {"
                            + "?s a <" + cls.toStringID() + ">."
                            + "?s <" + prop1.toStringID() + "> ?o1."
                            + "?s <" + prop2.toStringID() + "> ?o2."
                            + "}";
                    rs = executeSelectQuery(query);
                    double frequency = (double) (rs.next().getLiteral("cnt").getInt());
                    pair2Frequency.put(pair, frequency);
                }
            }
        }
        return pair2Frequency;
    }
    
    private Map<Set<OWLObjectProperty>, Double> getCooccurrences(OWLClass cls, Set<OWLObjectProperty> outgoingProperties, Set<OWLObjectProperty> incomingProperties) {
        Map<Set<OWLObjectProperty>, Double> pair2Frequency = new HashMap<Set<OWLObjectProperty>, Double>();
        
        SetView<OWLObjectProperty> allProperties = Sets.union(outgoingProperties, incomingProperties);
        
        //compute the frequency for each pair
        ResultSet rs;
        Set<OWLObjectProperty> pair;
        for (OWLObjectProperty prop1 : allProperties) {
            for (OWLObjectProperty prop2 : allProperties) {
                if (!prop1.equals(prop2) && !pair2Frequency.containsKey(pair = Sets.newHashSet(prop1, prop2))) {
                    String query = "SELECT (COUNT(DISTINCT ?s) AS ?cnt) WHERE {"
                            + "?s a <" + cls.toStringID() + ">.";
                    if(outgoingProperties.contains(prop1)){
                    	query += "?s <" + prop1.toStringID() + "> ?o1.";
                    } else {
                    	query += "?o1 <" + prop1.toStringID() + "> ?s.";
                    }
                    if(outgoingProperties.contains(prop2)){
                    	query += "?s <" + prop2.toStringID() + "> ?o2.";
                    } else {
                    	query += "?o2 <" + prop2.toStringID() + "> ?s.";
                    }
                    query +=  "}";
                    rs = executeSelectQuery(query);
                    double frequency = (double) (rs.next().getLiteral("cnt").getInt());
                    pair2Frequency.put(pair, frequency);
                }
            }
        }
        return pair2Frequency;
    }

    private Map<Set<OWLObjectProperty>, Double> getPropertySimilarities(OWLClass cls, Set<OWLObjectProperty> properties) {
        return PropertySimilarityCorrelation.getCooccurrences(cls, properties);
    }

    /**
     * Get all properties and its frequencies based on how often each property
     * is used by instances of the given class.
     *
     * @param cls
     * @param namespace
     * @return
     */
    private Map<OWLObjectProperty, Integer> getPropertiesWithFrequency(OWLClass cls, Direction propertyDirection) {
        Map<OWLObjectProperty, Integer> properties = new HashMap<OWLObjectProperty, Integer>();
        String query;
        if(propertyDirection == Direction.OUTGOING){
        	 query = "PREFIX owl:<http://www.w3.org/2002/07/owl#> "
        	 		+ "SELECT ?p (COUNT(DISTINCT ?s) AS ?cnt) WHERE {"
             		+ "?s a <" + cls.toStringID() + ">."
             		+ " {?p a owl:ObjectProperty.} UNION {?p a owl:DatatypeProperty.} "
             		+ "?s ?p ?o."
             		+ "} GROUP BY ?p";
        } else {
        	 query = "PREFIX owl:<http://www.w3.org/2002/07/owl#> "
        	 		+ "SELECT ?p (COUNT(DISTINCT ?s) AS ?cnt) WHERE {"
             		+ "?s a <" + cls.toStringID() + ">."
             		+ " {?p a owl:ObjectProperty.} UNION {?p a owl:DatatypeProperty.} "
             		+ "?o ?p ?s."
             		+ "} GROUP BY ?p";
        }
        
        //split into 2 queries because triple stores sometimes do not answer the query above
        query = "PREFIX owl:<http://www.w3.org/2002/07/owl#> "
        		+ "SELECT ?p (COUNT(DISTINCT ?s) AS ?cnt) WHERE {"
         		+ "?s a <" + cls.toStringID() + "> ."
         		+ "?p a owl:ObjectProperty . "
         		+ "?s ?p ?o ."
         		+ "} GROUP BY ?p";

        ResultSet rs = executeSelectQuery(query);
        QuerySolution qs;
        while (rs.hasNext()) {
            qs = rs.next();
            int frequency = qs.getLiteral("cnt").getInt();
            if(frequency > 0) {
                String uri = qs.getResource("p").getURI();
                if (!blacklist.contains(uri)) {
                    properties.put(new OWLObjectPropertyImpl(IRI.create(uri)), frequency);
                }
            }
            
        }
        
        query = "PREFIX owl:<http://www.w3.org/2002/07/owl#> "
        		+ "SELECT ?p (COUNT(DISTINCT ?s) AS ?cnt) WHERE {"
         		+ "?s a <" + cls.toStringID() + "> ."
         		+ " ?p a owl:DatatypeProperty . "
         		+ "?s ?p ?o ."
         		+ "} GROUP BY ?p";
        logger.info(query);
        rs = executeSelectQuery(query);
        while (rs.hasNext()) {
            qs = rs.next();
            int frequency = qs.getLiteral("cnt").getInt();
            if(frequency > 0) {
	            String uri = qs.getResource("p").getURI();
	            if (!blacklist.contains(uri)) {
	                properties.put(new OWLObjectPropertyImpl(IRI.create(uri)), frequency);
	            }
            }
        }
        return properties;
    }
    
    private void aPriori(OWLClass cls, Set<OWLObjectProperty> properties, int minSupport) {
        System.out.println("Candidates: " + properties);
        System.out.println("Min. support: " + minSupport);
        Set<Set<OWLObjectProperty>> nonFrequentSubsets = new HashSet<Set<OWLObjectProperty>>();
        for (int i = 2; i <= properties.size(); i++) {
            Set<Set<OWLObjectProperty>> subsets = getSubsets(properties, i);
            Set<Set<OWLObjectProperty>> nonFrequentSubsetsTmp = new HashSet<Set<OWLObjectProperty>>();
            ResultSet rs;
            for (Set<OWLObjectProperty> set : subsets) {
                if (Sets.intersection(getSubsets(set, i - 1), nonFrequentSubsets).isEmpty()) {
                    String query = "SELECT (COUNT(DISTINCT ?s) AS ?cnt) WHERE {\n" + "?s a <" + cls.toStringID() + ">.";
                    int cnt = 1;
                    for (OWLObjectProperty property : set) {
                        query += "?s <" + property.toStringID() + "> ?o" + cnt++ + ".\n";
                    }
                    query += "}";

                    rs = executeSelectQuery(query);
                    int frequency = rs.next().getLiteral("cnt").getInt();
                    if (frequency < minSupport) {
                        nonFrequentSubsetsTmp.add(set);
                        System.out.println("Too low support(" + frequency + "): " + set);
                    } else {
                        System.out.println(set);
                    }
                } else {
                    System.err.println("BLA");
                }
            }
            nonFrequentSubsets = nonFrequentSubsetsTmp;
        }
    }

    private SortedSet<OWLObjectProperty> getMostProminentProperties(OWLClass cls, double threshold, String namespace, Direction propertyDirection) {
    	logger.info("Computing most prominent " + propertyDirection.name().toLowerCase() + " properties for class " + cls + " ...");
        SortedSet<OWLObjectProperty> properties = new TreeSet<OWLObjectProperty>();

        //get total number of instances for the class
        int instanceCount = getInstanceCount(cls);
        logger.info("Number of instances in class: " + instanceCount);

        //get all properties+frequency that are used by instances of the class
        Map<OWLObjectProperty, Integer> propertiesWithFrequency = getPropertiesWithFrequency(cls, propertyDirection);

        //get all properties with a relative frequency above the threshold
        for (Entry<OWLObjectProperty, Integer> entry : MapUtils.sortByValues(propertiesWithFrequency)) {
            OWLObjectProperty property = entry.getKey();
            Integer frequency = entry.getValue();
           
            double score = frequency / (double) instanceCount;
            logger.info(property + ": " + frequency + " -> " + score);
            if (score >= threshold) {
                properties.add(property);
            }
        }
        logger.info("...got " + properties);
        return properties;
    }

    /**
     * Get the total number of instances for the given class.
     *
     * @param cls
     * @return
     */
    private int getInstanceCount(OWLClass cls) {
        String query = "SELECT (COUNT(?s) AS ?cnt) WHERE {?s a <" + cls.toStringID() + ">.}";
        ResultSet rs = executeSelectQuery(query);
        return rs.next().getLiteral("cnt").getInt();
    }

    private ResultSet executeSelectQuery(String query) {
    	logger.debug(query);
        QueryExecution qe = qef.createQueryExecution(query);
        ResultSet rs = qe.execSelect();
        return rs;
    }

    private <T> Set<Set<T>> getSubsets(Set<T> set, int size) {
        Set<Set<T>> subsets = new HashSet<Set<T>>();
        Set<Set<T>> powerSet = Sets.powerSet(set);
        for (Set<T> subset : powerSet) {
            if (subset.size() == size) {
                subsets.add(subset);
            }
        }
        return subsets;
    }

    private <T> Set<Set<T>> getSupersets(Set<Set<T>> sets, int size) {
        Set<Set<T>> supersets = new HashSet<Set<T>>();
        for (Set<T> set1 : sets) {
            for (Set<T> set2 : sets) {
                if (set1 != set2 && !Sets.intersection(set1, set2).isEmpty()) {
                    Set<T> union = Sets.newHashSet();
                    union.addAll(set1);
                    union.addAll(set2);
                    supersets.add(union);
                }
            }
        }
        return supersets;
    }
    
    /**
	 * @param useIncomingProperties the useIncomingProperties to set
	 */
	public void setUseIncomingProperties(boolean useIncomingProperties) {
		this.useIncomingProperties = useIncomingProperties;
	}

    public static void main(String[] args) throws Exception {
        Lexicon lexicon = Lexicon.getDefaultLexicon();
        NLGFactory nlg = new NLGFactory(lexicon);
        Realiser realiser = new Realiser(lexicon);
    	SPhraseSpec clause = nlg.createClause();
    	clause.setSubject(nlg.createNounPhrase("Stephen King"));
    	clause.setVerbPhrase(nlg.createVerbPhrase("be author of"));
    	clause.setObject("Mysery");
    	System.out.println(realiser.realise(clause));
        
        
    	
        SparqlEndpoint endpoint = SparqlEndpoint.getEndpointDBpedia();
        //endpoint.getDefaultGraphURIs().add("http://dbpedia.org/sparql");
        DatasetBasedGraphGenerator graphGenerator = new DatasetBasedGraphGenerator(endpoint, "cache3");
        graphGenerator.setUseIncomingProperties(true);
		WeightedGraph wg = graphGenerator.generateGraph(new OWLClassImpl(IRI.create("http://dbpedia.org/ontology/Organisation")), 0.2, "http://dbpedia.org/ontology/", Cooccurrence.PROPERTIES);
//		generateGraph(0.5, "http://dbpedia.org/ontology/");
    }
}