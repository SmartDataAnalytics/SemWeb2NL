package org.aksw.avatar;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.triple2nl.gender.DelegateGenderDetector;
import org.aksw.triple2nl.gender.DictionaryBasedGenderDetector;
import org.aksw.triple2nl.gender.PropertyBasedGenderDetector;
import org.aksw.triple2nl.gender.TypeAwareGenderDetector;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ResultSet;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLIndividual;
import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;

/**
 * A DBpedia test to run on a bunch of random resources and check for errors and inspect possible performance issues.
 *
 * @author Lorenz Buehmann
 */
public class PerformanceTestRunner {

    static int nrOfClasses = 400;
    static int nrOfResourcesPerClass = 3;
    static int nrOfResourcesTotal = 100;

    public static void main(String[] args) throws Exception {

        SparqlEndpointKS ks = new SparqlEndpointKS(SparqlEndpoint.getEndpointDBpedia());
        ks.setRetryCount(0);
        ks.init();
        QueryExecutionFactory qef = ks.getQueryExecutionFactory();

        Verbalizer v = new Verbalizer(qef, "/tmp/avatar-test/");
        TypeAwareGenderDetector genderDetector = new TypeAwareGenderDetector(qef, new DelegateGenderDetector(Lists.newArrayList(
                new PropertyBasedGenderDetector(qef, Lists.newArrayList("http://xmlns.com/foaf/0.1/gender")),
                new DictionaryBasedGenderDetector())));
        genderDetector.setPersonTypes(Sets.newHashSet("http://dbpedia.org/ontology/Person"));
        v.setGenderDetector(genderDetector);
        v.setAllowedNamespaces(Sets.newHashSet("http://dbpedia.org/ontology/"));

        ParameterizedSparqlString queryTemplate = new ParameterizedSparqlString(
                "SELECT ?s {?s a ?cls . } LIMIT " + nrOfResourcesPerClass);

        String query = "SELECT * {?s a owl:Class } ORDER BY DESC(?s) LIMIT " + nrOfClasses;

        StringBuilder sb = new StringBuilder();

        try(QueryExecution qe = qef.createQueryExecution(query)) {
            ResultSet rs = qe.execSelect();

            while(rs.hasNext()) {
                String cls = rs.next().getResource("s").getURI();
                System.out.println(cls);

                queryTemplate.setIri("cls", cls);

                query = queryTemplate.toString();

                try(QueryExecution qe2 = qef.createQueryExecution(query)) {
                    ResultSet rs2 = qe2.execSelect();

                    while(rs2.hasNext()) {
                        String iri = rs2.next().getResource("s").getURI();
//                        iri = "http://dbpedia.org/resource/Shiranui_Dakuemon";
//                        iri = "http://dbpedia.org/resource/Red_Bull_Arena_(Salzburg)";
//                        iri = "http://dbpedia.org/resource/Alexander_Morrison_(judge)__1";
                        OWLIndividual ind = new OWLNamedIndividualImpl(IRI.create(iri));

                        String summary = v.summarize(ind);
                        System.err.println(summary);
                        sb.append(ind).append("\t\t").append(summary).append("\n");
//                        System.exit(0);
                    }
                }
            }
        }

        System.out.println(sb);


    }
}
