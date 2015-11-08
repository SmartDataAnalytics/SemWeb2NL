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
