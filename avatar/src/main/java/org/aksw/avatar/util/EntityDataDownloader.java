package org.aksw.avatar.util;

import com.google.common.collect.Sets;
import net.sf.extjwnl.data.Exc;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.SparqlEndpoint;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Lorenz Buehmann
 */
public class EntityDataDownloader {

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
                    "  }\n" +
                    "WHERE\n" +
                    "  { ?s  ?p  ?o .\n" + " ?p a ?p_type . VALUES ?p_type {owl:ObjectProperty owl:DatatypeProperty}" +
                    "    FILTER ( ( isURI(?o) || ( datatype(?o) != \"\" ) ) || langMatches(lang(?o), \"en\") )\n" +
                    "    #1 FILTER ( ?p NOT IN ($IGNORED_PROPERTIES$) )\n" +
                    "    #2 OPTIONAL\n" +
                    "    #2   { VALUES ?cls { $EXPANSION_CLASSES$ }\n" +
                    "    #2    ?o  ?p1  ?o1\n" +
                    "    #2    FILTER EXISTS { ?o rdf:type/(rdfs:subClassOf)* ?cls }\n" +
                    "    #2   }\n" +
                    "    VALUES ?s { ?entity }\n" +
                    "  }";

    private final ParameterizedSparqlString QUERY_TEMPLATE;

    private final QueryExecutionFactory qef;

    public EntityDataDownloader(QueryExecutionFactory qef, Set<String> expansionClasses, Set<String> ignoredProperties) {
        this.qef = qef;

        String queryStr = QUERY_TEMPLATE_STRING;
        // set ignored properties
        if(!ignoredProperties.isEmpty()) {
            queryStr = queryStr.replace("#1", "");
            queryStr = queryStr.replace(
                            "$IGNORED_PROPERTIES$",
                    ignoredProperties.stream().map(p -> "<" + p + ">").collect(Collectors.joining(", ")));
        }

        // enable class expansion
        if(!expansionClasses.isEmpty()) {
            queryStr = queryStr.replace("#2", "");
            queryStr = queryStr.replace(
                            "$EXPANSION_CLASSES$",
                    expansionClasses.stream().map(cls -> "<" + cls + ">").collect(Collectors.joining(" ")));
        }

        System.out.println(queryStr);
        QUERY_TEMPLATE = new ParameterizedSparqlString(queryStr);
    }

    /**
     * Load the instance data for the given entity.
     * @param entity the entity
     * @return a model containing the instance data
     */
    public Model loadData(String entity) {
        QUERY_TEMPLATE.setIri("entity", entity);
        String query = QUERY_TEMPLATE.toString();
        try(QueryExecution qe = qef.createQueryExecution(query)) {
            return qe.execConstruct();
        }
    }

    public static void main(String[] args) throws Exception{
        SparqlEndpointKS ks = new SparqlEndpointKS(SparqlEndpoint.getEndpointDBpedia());
        ks.init();

        EntityDataDownloader dl = new EntityDataDownloader(
                ks.getQueryExecutionFactory(),
                Sets.newHashSet("http://dbpedia.org/ontology/PersonFunction"),
                Sets.newHashSet(OWL.sameAs.getURI(), RDFS.seeAlso.getURI(),
                        "http://dbpedia.org/ontology/wikiPageExternalLink",
                        "http://dbpedia.org/ontology/wikiPageID",
                        "http://dbpedia.org/ontology/wikiPageRevisionID"));

        Model model = dl.loadData("http://dbpedia.org/resource/Paris");
        model.write(System.out, "TURTLE");
    }

}
