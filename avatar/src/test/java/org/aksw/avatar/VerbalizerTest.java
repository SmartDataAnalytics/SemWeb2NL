/**
 * 
 */
package org.aksw.avatar;

import org.dllearner.kb.sparql.SparqlEndpoint;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLIndividual;

import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;

/**
 * @author Lorenz Buehmann
 *
 */
public class VerbalizerTest {
	//set up the SPARQL endpoint, in our case it's DBpedia
	private static final SparqlEndpoint endpoint = SparqlEndpoint.getEndpointDBpedia();
	
	//create the verbalizer used to generate the textual summarization
	private static final Verbalizer verbalizer = new Verbalizer(endpoint, "cache", null);

	/**
	 * Test method for {@link org.aksw.avatar.Verbalizer#summarize(org.dllearner.core.owl.Individual)}.
	 */
	@Test
	public void testSummarizeIndividual() {
		//define the entity to summarize
		OWLIndividual ind = new OWLNamedIndividualImpl(IRI.create("http://dbpedia.org/resource/Albert_Einstein"));

		//compute summarization of the entity and verbalize it
		String summary = verbalizer.summarize(ind);
		System.out.println(summary);
	}

	/**
	 * Test method for {@link org.aksw.avatar.Verbalizer#summarize(org.dllearner.core.owl.Individual, org.dllearner.core.owl.NamedClass)}.
	 */
	@Test
	public void testSummarizeIndividualNamedClass() {
		//define the class of the entity
		OWLClass cls = new OWLClassImpl(IRI.create("http://dbpedia.org/ontology/Scientist"));

		//define the entity to summarize
		OWLIndividual ind = new OWLNamedIndividualImpl(IRI.create("http://dbpedia.org/resource/Albert_Einstein"));

		//compute summarization of the entity and verbalize it
		String summary = verbalizer.summarize(ind, cls);
		System.out.println(summary);
	}

}
