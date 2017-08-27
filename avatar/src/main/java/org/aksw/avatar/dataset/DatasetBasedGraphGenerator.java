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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;

import com.inamik.text.tables.GridTable;
import com.inamik.text.tables.SimpleTable;
import com.inamik.text.tables.grid.Border;
import com.inamik.text.tables.grid.Util;
import org.aksw.avatar.clustering.Node;
import org.aksw.avatar.clustering.WeightedGraph;
import org.aksw.avatar.exceptions.NoGraphAvailableException;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.apache.jena.query.*;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.reasoning.SPARQLReasoner;
import org.dllearner.utilities.MapUtils;
import org.dllearner.utilities.OWLAPIUtils;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import static com.inamik.text.tables.Cell.Functions.RIGHT_ALIGN;

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
	
	private static final Logger logger = LoggerFactory.getLogger(DatasetBasedGraphGenerator.class);

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
        
        class2OutgoingProperties = new HashMap<>();
    }
    
    /**
     * @param blacklist the property blacklist
     */
    public void setPropertiesBlacklist(Set<String> blacklist) {
        this.blacklist = blacklist;
    }

    public Map<OWLClass, WeightedGraph> generateGraphs(double threshold) {
        return generateGraphs(threshold, null);
    }

    public Map<OWLClass, WeightedGraph> generateGraphs(double threshold, String namespace) {
        Map<OWLClass, WeightedGraph> graphs = new HashMap<>();

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
    	    logger.warn("Can't generate summary graph based on dataset statistics. Reason: Empty class " + cls);
    		return null;
    	}
    	
        // get the outgoing properties with a prominence score above threshold
        final SortedSet<OWLObjectProperty> outgoingProperties = getMostProminentProperties(cls, threshold, namespace, Direction.OUTGOING);
        class2OutgoingProperties.put(cls, outgoingProperties);
        
        //get the incoming properties with a prominence score above threshold
        SortedSet<OWLObjectProperty> incomingProperties = new TreeSet<>();
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
                logger.error("Failed to generate summary graph for class " + cls, e);
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
        Map<Set<OWLObjectProperty>, Double> pair2Frequency = new HashMap<>();
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
        Map<Set<OWLObjectProperty>, Double> pair2Frequency = new HashMap<>();
        
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

    private static final ParameterizedSparqlString PROPERTY_FREQUENCY_TEMPLATE = new ParameterizedSparqlString(
            "PREFIX owl:<http://www.w3.org/2002/07/owl#> "
            + "SELECT (COUNT(DISTINCT ?s) AS ?cnt) WHERE {"
            + "?s a ?cls ."
            + "?s ?p ?o ."
            + "}");

    private static final ParameterizedSparqlString PROPERTIES_OF_CLASS_TEMPLATE = new ParameterizedSparqlString(
            "PREFIX owl:<http://www.w3.org/2002/07/owl#> "
                    + "SELECT DISTINCT ?p WHERE {"
                    + "?s a ?cls ."
                    + "?s ?p ?o . {select ?p {?p a owl:ObjectProperty}}"
                    + "}");

    private Map<OWLObjectProperty, Integer> getPropertiesWithFrequencySingle(OWLClass cls, Direction propertyDirection) {
        Map<OWLObjectProperty, Integer> properties = new HashMap<>();

        PROPERTIES_OF_CLASS_TEMPLATE.setIri("cls", cls.toStringID());
        PROPERTY_FREQUENCY_TEMPLATE.setIri("cls", cls.toStringID());

        String query = PROPERTIES_OF_CLASS_TEMPLATE.toString();

        try(ResultSetCloseable rs = executeSelectQuery(query)) {
            while (rs.hasNext()) {
                QuerySolution qs = rs.next();
                String property = qs.getResource("p").getURI();
                if(!property.startsWith("http://dbpedia.org/ontology/")) continue;

                PROPERTY_FREQUENCY_TEMPLATE.setIri("p", property);
                String query2 = PROPERTY_FREQUENCY_TEMPLATE.toString();

                try(ResultSetCloseable rs2 = executeSelectQuery(query2)) {
                    int frequency = rs2.next().getLiteral("cnt").getInt();
                    if(frequency > 0) {
                        String uri = qs.getResource("p").getURI();
                        if (!blacklist.contains(uri)) {
                            properties.put(new OWLObjectPropertyImpl(IRI.create(uri)), frequency);
                        }
                    }
                }
            }
        }

        return properties;
    }

    /**
     * Get all properties and its frequencies based on how often each property
     * is used by instances of the given class.
     *
     * @param cls
     * @param propertyDirection
     * @return
     */
    private Map<OWLObjectProperty, Integer> getPropertiesWithFrequencyBatch(OWLClass cls, Direction propertyDirection) {
        Map<OWLObjectProperty, Integer> properties = new HashMap<>();
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

        try(ResultSetCloseable rs = executeSelectQuery(query)) {
            while (rs.hasNext()) {
                QuerySolution qs = rs.next();
                int frequency = qs.getLiteral("cnt").getInt();
                if(frequency > 0) {
                    String uri = qs.getResource("p").getURI();
                    if (!blacklist.contains(uri)) {
                        properties.put(new OWLObjectPropertyImpl(IRI.create(uri)), frequency);
                    }
                }

            }
        }

        query = "PREFIX owl:<http://www.w3.org/2002/07/owl#> "
        		+ "SELECT ?p (COUNT(DISTINCT ?s) AS ?cnt) WHERE {"
         		+ "?s a <" + cls.toStringID() + "> ."
         		+ " ?p a owl:DatatypeProperty . "
         		+ "?s ?p ?o ."
         		+ "} GROUP BY ?p";

        try(ResultSetCloseable rs = executeSelectQuery(query)) {
            while (rs.hasNext()) {
                QuerySolution qs = rs.next();
                int frequency = qs.getLiteral("cnt").getInt();
                if(frequency > 0) {
                    String uri = qs.getResource("p").getURI();
                    if (!blacklist.contains(uri)) {
                        properties.put(new OWLObjectPropertyImpl(IRI.create(uri)), frequency);
                    }
                }
            }
        }

        return properties;
    }
    
    private void aPriori(OWLClass cls, Set<OWLObjectProperty> properties, int minSupport) {
        System.out.println("Candidates: " + properties);
        System.out.println("Min. support: " + minSupport);
        Set<Set<OWLObjectProperty>> nonFrequentSubsets = new HashSet<>();
        for (int i = 2; i <= properties.size(); i++) {
            Set<Set<OWLObjectProperty>> subsets = getSubsets(properties, i);
            Set<Set<OWLObjectProperty>> nonFrequentSubsetsTmp = new HashSet<>();
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

    ParameterizedSparqlString SUPER_CLASSES_QUERY = new ParameterizedSparqlString("PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                                                                                          "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                                                                                          "SELECT DISTINCT ?sup WHERE {?cls rdfs:subClassOf+ ?sup . FILTER(?sup != owl:Thing)}");

    private SortedSet<OWLObjectProperty> getMostProminentProperties(OWLClass cls, double threshold, String namespace, Direction propertyDirection) {
    	logger.info("Computing most prominent " + propertyDirection.name().toLowerCase() + " properties for class " + cls + " ...");
        SortedSet<OWLObjectProperty> properties = new TreeSet<>();

        SUPER_CLASSES_QUERY.setIri("cls", cls.toStringID());
        try(ResultSetCloseable rs = executeSelectQuery(SUPER_CLASSES_QUERY.toString())){
            while(rs.hasNext()) {
                cls = new OWLClassImpl(IRI.create(rs.next().getResource("sup").getURI()));

                // get total number of instances for the class
                int instanceCount = getInstanceCount(cls);
                logger.info("Number of instances in class: " + instanceCount);



                // get all properties+frequency that are used by instances of the class
                Map<OWLObjectProperty, Integer> propertiesWithFrequency =
                        (instanceCount > 500000)
                                ? getPropertiesWithFrequencySingle(cls, propertyDirection)
                                : getPropertiesWithFrequencyBatch(cls, propertyDirection);

                if(propertiesWithFrequency.isEmpty()) {
                    logger.warn("No properties found for class " + cls);
                }

                SimpleTable table = SimpleTable.of();

                // get all properties with a relative frequency above the threshold
                for (Entry<OWLObjectProperty, Integer> entry : MapUtils.sortByValues(propertiesWithFrequency)) {
                    OWLObjectProperty property = entry.getKey();
                    Integer frequency = entry.getValue();

                    double score = frequency / (double) instanceCount;

                    if (score >= threshold) {
                        properties.add(property);
                    }

                    table.nextRow()
                            .nextCell().addLine(property.toString())
                            .nextCell().addLine(String.valueOf(frequency)).applyToCell(RIGHT_ALIGN.withWidth(8))
                            .nextCell().addLine(df.format(score));
                }

                logger.info(prettyString(table));
            }
        }

        //get total number of instances for the class
        int instanceCount = getInstanceCount(cls);
        logger.info("Number of instances in class: " + instanceCount);



        // get all properties+frequency that are used by instances of the class
        Map<OWLObjectProperty, Integer> propertiesWithFrequency =
                (instanceCount > 500000)
                ? getPropertiesWithFrequencySingle(cls, propertyDirection)
                : getPropertiesWithFrequencyBatch(cls, propertyDirection);

        if(propertiesWithFrequency.isEmpty()) {
            logger.warn("No properties found for class " + cls);
        }

        SimpleTable table = SimpleTable.of();

        // get all properties with a relative frequency above the threshold
        for (Entry<OWLObjectProperty, Integer> entry : MapUtils.sortByValues(propertiesWithFrequency)) {
            OWLObjectProperty property = entry.getKey();
            Integer frequency = entry.getValue();
           
            double score = frequency / (double) instanceCount;

            if (score >= threshold) {
                properties.add(property);
            }

            table.nextRow()
                    .nextCell().addLine(property.toString())
                    .nextCell().addLine(String.valueOf(frequency)).applyToCell(RIGHT_ALIGN.withWidth(8))
                    .nextCell().addLine(df.format(score));
        }

        if(properties.isEmpty()) {
            logger.warn("No prominent properties above threshold found for class " + cls);
        } else {
            logger.info(prettyString(table));
            logger.info("Most prominent " + propertyDirection.name().toLowerCase() + " properties for class " + cls + ":\n" + properties);
        }

       return properties;
    }

    private static final DecimalFormat df = new DecimalFormat("#.000000");

    private String prettyString(SimpleTable table) {
        GridTable g = table.toGrid();
        g = Border.of(Border.Chars.of('+', '-', '|')).apply(g);
        try {
            try(ByteArrayOutputStream baos = new ByteArrayOutputStream()){
                PrintStream ps = new PrintStream(baos);
                Util.print(g, ps);
                try {
                    return new String(baos.toByteArray(), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    logger.error("Failed to convert table.", e);
                }
            }
        } catch (IOException e) {
            logger.error("Failed to convert table.", e);
        }

        return null;
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

    private ResultSetCloseable executeSelectQuery(String query) {
    	logger.debug(query);
        System.out.println(query);

//        QueryEngineHTTP qe = new QueryEngineHTTP("http://dbpedia.org/sparql", QueryFactory.create(query));

        QueryExecution qe = qef.createQueryExecution(QueryFactory.create(query));
        return ResultSetCloseable.closeableResultSet(qe);
    }

    private <T> Set<Set<T>> getSubsets(Set<T> set, int size) {
        Set<Set<T>> subsets = new HashSet<>();
        Set<Set<T>> powerSet = Sets.powerSet(set);
        for (Set<T> subset : powerSet) {
            if (subset.size() == size) {
                subsets.add(subset);
            }
        }
        return subsets;
    }

    private <T> Set<Set<T>> getSupersets(Set<Set<T>> sets, int size) {
        Set<Set<T>> supersets = new HashSet<>();
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