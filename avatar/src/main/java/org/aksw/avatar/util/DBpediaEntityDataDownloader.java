package org.aksw.avatar.util;

import com.google.common.collect.Sets;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;

import java.util.Set;

/**
 * @author Lorenz Buehmann
 */
public class DBpediaEntityDataDownloader extends EntityDataDownloader{

    private static final String QUERY_TEMPLATE_STRING =
            "PREFIX  dbo:  <http://dbpedia.org/ontology/>\n" +
                    "PREFIX  rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                    "PREFIX  rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                    "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                    "\n" +
                    "CONSTRUCT \n" +
                    "  { \n" +
                    "    ?s ?p ?o .\n" +
                    "    #2 ?o ?p1 ?o1 .\n" +
                    "    #3 ?o a ?type .\n" +
//                    " ?o dbo:rank ?rank .\n" +
                    "  }\n" +
                    "WHERE\n" +
                    "  { ?s  ?p  ?o .\n" + " ?p a ?p_type . VALUES ?p_type {owl:ObjectProperty owl:DatatypeProperty}" +
                    " #3 OPTIONAL{?o a ?type . FILTER(STRSTARTS(STR(?type), 'http://dbpedia.org/ontology/'))}" +
                    "    FILTER ( ( isURI(?o) || ( datatype(?o) != \"\" ) ) || langMatches(lang(?o), \"en\") )\n" +
//                    "BIND(IF(isURI(?o),  <LONG::IRI_RANK> (?o) , '') AS ?rank)" +
                    "    #1 FILTER ( ?p NOT IN ($IGNORED_PROPERTIES$) )\n" +
                    "    #2 OPTIONAL\n" +
                    "    #2   { VALUES ?cls { $EXPANSION_CLASSES$ }\n" +
                    "    #2    ?o  ?p1  ?o1\n" +
                    "    #2    FILTER EXISTS { ?o rdf:type/(rdfs:subClassOf|owl:equivalentClass|^owl:equivalentClass)* ?cls }\n" +
                    "    #2   }\n" +
                    "    VALUES ?s { ?entity }\n" +
                    "  }";

    private static final Set<String> expansionClasses = Sets.newHashSet(
            "http://dbpedia.org/ontology/PersonFunction",
            "http://dbpedia.org/ontology/TimePeriod"
            );

    private static final Set<String> ignoredProperties = Sets.newHashSet(
            OWL.sameAs.getURI(),
            RDFS.seeAlso.getURI(),
            "http://dbpedia.org/ontology/wikiPageExternalLink",
            "http://dbpedia.org/ontology/wikiPageID",
            "http://dbpedia.org/ontology/wikiPageRevisionID",
            "http://www.w3.org/ns/prov#wasDerivedFrom");

    public DBpediaEntityDataDownloader(QueryExecutionFactory qef) {
        super(qef, QUERY_TEMPLATE_STRING, expansionClasses, ignoredProperties);
    }
}
