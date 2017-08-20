package org.aksw.avatar.util.dbpedia;

import org.aksw.avatar.util.DBpediaEntityDataDownloader;
import org.aksw.avatar.util.EntityDataDownloader;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.update.UpdateAction;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.SparqlEndpoint;

/**
 * We convert from
 * ?s dbo:careerStation ?o. ?o dbo:team ?t
 * to
 * ?s dbo:careerStation ?t
 * @author Lorenz Buehmann
 */
public class TimePeriodConverter {

	// query to determine the properties used by dbo:PersonFunction instances
	String query = "SELECT ?p count(?o) sample(?s) sample(?o1) {\n" +
			"?s <http://dbpedia.org/ontology/careerStation> ?o. ?o ?p ?o1\n" +
			"filter(?p != rdf:type)\n" +
			"}\n" +
			"group by ?p";


	public void convert(Model model) {

	}

	public static void main(String[] args) throws Exception {
		SparqlEndpointKS ks = new SparqlEndpointKS(SparqlEndpoint.getEndpointDBpedia());
		ks.init();

		EntityDataDownloader dl = new DBpediaEntityDataDownloader(ks.getQueryExecutionFactory());

		Model model = dl.loadData("http://dbpedia.org/resource/Neymar");
		model.setNsPrefix("dbo", "http://dbpedia.org/ontology/");
		model.setNsPrefix("", "http://dbpedia.org/resource/");
		model.setNsPrefixes(PrefixMapping.Extended);

		String update =
				"PREFIX dbo: <http://dbpedia.org/ontology/> " +
						"DELETE {?s dbo:careerStation ?o . ?o ?p ?o1}" +
						"INSERT {?s dbo:careerStation ?team}" +
						"WHERE {?s dbo:careerStation ?o . ?o ?p ?o1 . ?o dbo:team ?t . Optional {?o dbo:years ?year}" +
						"BIND(IF(bound(?year), uri(concat(str(?t), str(?year))), ?t) as ?team)}";


		model.write(System.out, "TURTLE");
		UpdateAction.parseExecute(update, model);
		model.write(System.out, "TURTLE");
	}
}
