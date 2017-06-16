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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.avatar.dump;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.aksw.avatar.SPARQLQueryProcessor;
import org.aksw.avatar.clustering.Node;
import org.aksw.avatar.clustering.WeightedGraph;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.reasoning.SPARQLReasoner;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLProperty;

import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;

import com.google.common.collect.Multimap;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 *
 * @author ngonga
 */
public class Controller {

    public static boolean selectQueriesWithEmptyResults = true;
    private static final String testFile = "resources/dbpediaLog/dbpedia.log-valid-select-nonjava.gz";
    private static SparqlEndpointKS ks = new SparqlEndpointKS(SparqlEndpoint.getEndpointDBpediaLiveAKSW());

    /**
     * Generates the weighted graph for a given class
     *
     * @param ontClass Named Class
     * @param dumpFile Dump file to be processed
     * @return Weighted Graph
     */
    public static WeightedGraph generateGraph(OWLClass ontClass, String dumpFile) {
        //get dump
        DBpediaDumpProcessor dp = new DBpediaDumpProcessor();
        List<LogEntry> entries = dp.processDump(dumpFile, selectQueriesWithEmptyResults);
        return generateGraph(ontClass, entries);
    }

    /**
     * Generates the weighted graph for the whole dump without considering the
     * class
     *
     * @param dumpFile Dump file to be processed
     * @return Weighted Graph
     */
    public static WeightedGraph generateGraph(String dumpFile) {
        //get dump
        DBpediaDumpProcessor dp = new DBpediaDumpProcessor();
        List<LogEntry> entries = dp.processDump(dumpFile, selectQueriesWithEmptyResults);
        return generateGraph(entries);
    }

    /**
     * Generates the weighted graph for a given class
     *
     * @param ontClass Named Class
     * @param entries List of log entries (e.g., from a dump file)
     * @return Weighted Graph
     */
    public static WeightedGraph generateGraph(OWLClass ontClass, List<LogEntry> entries) {
        WeightedGraph wg = new WeightedGraph();
        
        SPARQLQueryProcessor processor = new SPARQLQueryProcessor(ks);
        int count = 0;
        //feed data into the processor and then into the graph
        for (LogEntry l : entries) {
            Map<OWLClass, Set<OWLProperty>> result = processor.processQuery(l.sparqlQuery);
            if (result.containsKey(ontClass)) {
                count++;
                Set<OWLProperty> properties = result.get(ontClass);
                Set<Node> nodes = new HashSet<Node>();
                for (OWLProperty p : properties) {
                    if (!p.toStringID().equals(RDF.type.getURI())) {
                        if (wg.getNode(p.toStringID()) == null) {
                            Node newNode = new Node(p.toStringID());
                            nodes.add(newNode);
                        } else {
                            nodes.addAll(wg.getNode(p.toStringID()));
                        }
                    }
                }
                wg.addClique(nodes);
            }
        }
        System.out.println("Found " + count + " matching queries for " + ontClass.toStringID());
        wg.scale((double) count);
        return wg;
    }

    /**
     * Generates the weighted graph for a given class
     *
     * @param ontClass Named Class
     * @param entries List of log entries (e.g., from a dump file)
     * @return Weighted Graph
     */
    public static WeightedGraph generateGraphMultithreaded(OWLClass ontClass, Collection<Map<OWLClass, Set<OWLProperty>>> result) {
        WeightedGraph wg = new WeightedGraph();
        int count = 0;
        //feed data into the processor and then into the graph
        for (Map<OWLClass, Set<OWLProperty>> map : result) {
            if (map.containsKey(ontClass)) {
                count++;
                Set<OWLProperty> properties = map.get(ontClass);
                Set<Node> nodes = new HashSet<Node>();
                for (OWLProperty p : properties) {
                    if (!p.toStringID().equals(RDF.type.getURI())) {
                        if (wg.getNode(p.toStringID()) == null) {
                            Node newNode = new Node(p.toStringID());
                            nodes.add(newNode);
                        } else {
                            nodes.addAll(wg.getNode(p.toStringID()));
                        }
                    }
                }
                wg.addClique(nodes);
            }
        }
        System.out.println("Found " + count + " matching queries for " + ontClass.toStringID());
        wg.scale((double) count);
        return wg;
    }

    /**
     * Generates the weighted graph for a given class
     *
     * @param entries List of log entries (e.g., from a dump file)
     * @return Weighted Graph
     */
    public static WeightedGraph generateGraph(List<LogEntry> entries) {
        WeightedGraph wg = new WeightedGraph();
        SPARQLQueryProcessor processor = new SPARQLQueryProcessor(ks);
        int count = 0;
        //feed data into the processor and then into the graph
        for (LogEntry l : entries) {
            Map<OWLClass, Set<OWLProperty>> result = processor.processQuery(l.sparqlQuery);
            for (OWLClass ontClass : result.keySet()) {
                count++;
                Set<OWLProperty> properties = result.get(ontClass);
                Set<Node> nodes = new HashSet<Node>();
                for (OWLProperty p : properties) {
                    if (!p.toStringID().equals(RDF.type.getURI())) {
                        if (wg.getNode(p.toStringID()) == null) {
                            Node newNode = new Node(p.toStringID());
                            nodes.add(newNode);
                        } else {
                            nodes.addAll(wg.getNode(p.toStringID()));
                        }
                    }
                }
                wg.addClique(nodes);
            }
        }
        System.out.println("Found " + count + " matching queries overall");
        wg.scale((double) count);
        return wg;
    }

    /**
     * Generates the weighted graph for a given class
     *System.out.println("Reference Graph =============== ");
//            System.out.println("Edges = " + reference);
//            System.
     * @param entries List of log entries (e.g., from a dump file)
     * @return Weighted Graph
     */
    public static WeightedGraph generateGraphMultithreaded(Collection<Map<OWLClass, Set<OWLProperty>>> result) {
        WeightedGraph wg = new WeightedGraph();
        int count = 0;
        //feed data into the processor and then into the graph
       
        for (Map<OWLClass, Set<OWLProperty>> map : result) {
        	count++;
            for (OWLClass ontClass : map.keySet()) {
                Set<OWLProperty> properties = map.get(ontClass);
                Set<Node> nodes = new HashSet<Node>();
                for (OWLProperty p : properties) {
                    if (!p.toStringID().equals(RDF.type.getURI())) {
                        if (wg.getNode(p.toStringID()) == null) {
                            Node newNode = new Node(p.toStringID());
                            nodes.add(newNode);
                        } else {
                            nodes.addAll(wg.getNode(p.toStringID()));
                        }
                    }
                }
                wg.addClique(nodes);
            }
        }
        System.out.println("Found " + count + " matching queries overall");
        wg.scale((double) count);
        return wg;
    }

    public static void main(String args[]) {
//        test();
        testDumpReader();
//        testSPARQLQueryProcessor();
    }
    
	public static void testSPARQLQueryProcessor() {
		SPARQLQueryProcessor queryProcessor = new SPARQLQueryProcessor(ks);
		DBpediaDumpProcessor dp = new DBpediaDumpProcessor();
		List<LogEntry> entries = dp.processDump(testFile, false, 20000);
		
		// group by IP address
		Multimap<String, LogEntry> ip2Entries = LogEntryGrouping.groupByIPAddress(entries);
		System.out.println("#IP addresses: " + ip2Entries.keySet().size());
		
		// group by user agent
		Multimap<String, LogEntry> userAgent2Entries = LogEntryGrouping.groupByUserAgent(entries);
		System.out.println("#User agent: " + userAgent2Entries.keySet().size());
		
		for (Entry<String, Collection<LogEntry>> entry : userAgent2Entries.asMap().entrySet()) {
			String userAgent = entry.getKey();
			Collection<LogEntry> entriesForUserAgent = entry.getValue();
			System.out.println(userAgent + ": " + entriesForUserAgent.size());
		}

//		for (Entry<String, Collection<LogEntry>> entry : ip2Entries.asMap().entrySet()) {
//			String ip = entry.getKey();
//			Collection<LogEntry> entriesForIP = entry.getValue();
//			System.out.println(ip + ": " + entriesForIP.size());
//			// print top n
//			int n = 3;
//			for (LogEntry e : new ArrayList<LogEntry>(entriesForIP).subList(0, Math.min(entriesForIP.size(), n))) {
//				System.out.println(e.getSparqlQuery());
//				Map<OWLClass, Set<Property>> result = queryProcessor.processQuery(e.getSparqlQuery());
//				System.out.println(result);
//			}
//		}
	}

    public static void testDumpReader() {
        SPARQLQueryProcessor processor = new SPARQLQueryProcessor(ks);

//        OWLClass nc = new OWLClass("http://dbpedia.org/ontology/Person");

        DBpediaDumpProcessor dp = new DBpediaDumpProcessor();
        List<LogEntry> entries = dp.processDump(testFile, false);
        Collection<Map<OWLClass, Set<OWLProperty>>> result = processor.processEntries(entries);
        WeightedGraph reference = Controller.generateGraphMultithreaded(result);
        for(OWLClass nc : new SPARQLReasoner(ks).getOWLClasses()){
        	 WeightedGraph wg = Controller.generateGraphMultithreaded(nc, result);
            System.out.println("\n\nBasic Graph =============== ");
            System.out.println("Edges = " + wg);
            System.out.println("Nodes = " + wg.getNodes());
//            System.out.println("Reference Graph =============== ");
//            System.out.println("Edges = " + reference);
//            System.out.println("Nodes = " + reference.getNodes());
            System.out.println("Difference Graph =============== ");
            wg.nodeConservingMinus(reference);
//            System.out.println("Edges = " + wg);
            System.out.println("Nodes = " + wg.getNodes());
        }

    }

    public static void test() {
        SPARQLQueryProcessor processor = new SPARQLQueryProcessor(ks);
        List<LogEntry> entries = new ArrayList<LogEntry>();

        String q = "PREFIX dbr: <http://dbpedia.org/resource/> "
                + "PREFIX dbo: <http://dbpedia.org/ontology/> "
                + "SELECT ?s ?place ?date WHERE {?s a dbo:Person. ?s dbo:birthPlace ?place. ?s dbo:birthDate ?date.}";
        Query query = QueryFactory.create(q);
        Map<OWLClass, Set<OWLProperty>> occurrences = processor.processQuery(query);
        OWLClass nc = new OWLClassImpl(IRI.create("http://dbpedia.org/ontology/Person"));
//        System.out.println(Joiner.on("\n").withKeyValueSeparator("=").join(occurrences));

        LogEntry lg = new LogEntry(q);
        entries.add(lg);

        q = "PREFIX dbr: <http://dbpedia.org/resource/> "
                + "PREFIX dbo: <http://dbpedia.org/ontology/> "
                + "SELECT ?s ?place ?date WHERE {?s a dbo:Person. ?s dbo:birthPlace ?place. }";
        LogEntry lg2 = new LogEntry(q);
        entries.add(lg2);

        q = "PREFIX dbr: <http://dbpedia.org/resource/> "
                + "PREFIX dbo: <http://dbpedia.org/ontology/> "
                + "SELECT ?s ?place ?date WHERE {?s a dbo:Person. ?s dbo:birthPlace ?place. ?s dbo:birthDate ?date.}";
        LogEntry lg3 = new LogEntry(q);
        entries.add(lg3);

        q = "PREFIX dbr: <http://dbpedia.org/resource/> "
                + "PREFIX dbo: <http://dbpedia.org/ontology/> "
                + "SELECT ?s ?place ?date WHERE {?s a dbo:Person. ?s dbo:birthPlace ?place. }";

//        OWLClass nc = ne    w OWLClass("http://dbpedia.org/resource/Person");
        WeightedGraph wg = Controller.generateGraph(nc, entries);
        System.out.println("Edges = " + wg);
        System.out.println("Nodes = " + wg.getNodes());
    }
}
