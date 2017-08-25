package org.aksw.avatar.util.dbpedia;

import org.aksw.avatar.util.DBpediaEntityDataDownloader;
import org.aksw.avatar.util.EntityDataDownloader;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.update.UpdateAction;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.SparqlEndpoint;

/**
 * We convert from
 * <p>
 *    <code>
 *     ?s dbo:careerStation ?o. ?o dbo:team ?t
 * </code>
 * </p>
 * to
 * <p>
 *     <code>
 *     ?s dbo:careerStation ?t
 * </code>
 * </p>
 *
 *
 * @author Lorenz Buehmann
 */
public class TimePeriodConverter {

	// query to determine the properties used by dbo:PersonFunction instances
	String query = "SELECT ?p count(?o) sample(?s) sample(?o1) {\n" +
			"?s <http://dbpedia.org/ontology/careerStation> ?o. ?o ?p ?o1\n" +
			"filter(?p != rdf:type)\n" +
			"}\n" +
			"group by ?p";

	private static final String UPDATE_QUERY =
			"PREFIX dbo: <http://dbpedia.org/ontology/> " +
					"DELETE {?s dbo:careerStation ?o . ?o ?p ?o1}" +
					"INSERT {?s dbo:careerStation ?team}" +
					"WHERE {?s dbo:careerStation ?o . ?o ?p ?o1 . ?o dbo:team ?t . Optional {?o dbo:years ?year}" +
					"BIND(IF(bound(?year), uri(concat(str(?t)+\",\", str(?year))), ?t) as ?team)}";


	public Model convert(Model model) {
		Model copy = ModelFactory.createDefaultModel().add(model);
		UpdateAction.parseExecute(UPDATE_QUERY, copy);
		return copy;
	}

	public static void main(String[] args) throws Exception {
		SparqlEndpointKS ks = new SparqlEndpointKS(SparqlEndpoint.getEndpointDBpedia());
		ks.init();

		EntityDataDownloader dl = new DBpediaEntityDataDownloader(ks.getQueryExecutionFactory());

		Model model = dl.loadData("http://dbpedia.org/resource/Neymar");
		model.setNsPrefix("dbo", "http://dbpedia.org/ontology/");
		model.setNsPrefix("", "http://dbpedia.org/resource/");
		model.setNsPrefixes(PrefixMapping.Extended);

		model.write(System.out, "TURTLE");
		model = new TimePeriodConverter().convert(model);
		model.write(System.out, "TURTLE");
	}
}
