package org.aksw.avatar.util.dbpedia;

import com.google.common.collect.Sets;
import org.aksw.avatar.util.DBpediaEntityDataDownloader;
import org.aksw.avatar.util.EntityDataDownloader;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.SparqlEndpoint;

/**
 * We convert from
 * ?s dbo:personFunction ?o. ?o dbo:title ?t
 * to
 * ?s dbo:personFunction ?t
 * @author Lorenz Buehmann
 */
public class PersonFunctionConverter {

	// query to determine the properties used by dbo:PersonFunction instances
	String query = "SELECT ?p count(?o) sample(?s) sample(?o1) {\n" +
			"?s <http://dbpedia.org/ontology/personFunction> ?o. ?o ?p ?o1\n" +
			"filter(?p != rdf:type)\n" +
			"}\n" +
			"group by ?p";


	public void convert(Model model) {

	}

	public static void main(String[] args) throws Exception {
		SparqlEndpointKS ks = new SparqlEndpointKS(SparqlEndpoint.getEndpointDBpedia());
		ks.init();

		EntityDataDownloader dl = new DBpediaEntityDataDownloader(ks.getQueryExecutionFactory());

		Model model = dl.loadData("http://dbpedia.org/resource/Rosy_Senanayake");
		model.setNsPrefix("dbo", "http://dbpedia.org/ontology/");
		model.setNsPrefix("", "http://dbpedia.org/resource/");

		String update =
				"PREFIX dbo: <http://dbpedia.org/ontology/> " +
						"DELETE {?s dbo:personFunction ?o}" +
						"INSERT {?s dbo:personFunction ?t}" +
						"WHERE {?s dbo:personFunction ?o . ?o dbo:title ?t}";

		UpdateAction.parseExecute(update, model);

		model.write(System.out, "TURTLE");
	}
}
