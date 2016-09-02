/*
 * #%L
 * Triple2NL
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
package org.aksw.triple2nl;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.junit.BeforeClass;
import org.junit.Test;

import com.hp.hpl.jena.datatypes.BaseDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

import simplenlg.features.Feature;
import simplenlg.features.LexicalFeature;
import simplenlg.lexicon.Lexicon;
import simplenlg.phrasespec.SPhraseSpec;
import simplenlg.realiser.english.Realiser;

/**
 * @author Lorenz Buehmann
 *
 */
public class TripleConverterTest {
	
	private static final SparqlEndpoint ENDPOINT_DBPEDIA = SparqlEndpoint.getEndpointDBpedia();
	private static final SparqlEndpointKS KS = new SparqlEndpointKS(ENDPOINT_DBPEDIA);
	
	private static TripleConverter converter;

	private static Realiser r;
	
	@BeforeClass
	public static void init() throws Exception {
		KS.init();
		
		converter = new TripleConverter(KS.getQueryExecutionFactory(), "cache", (Lexicon)null);

		r = new Realiser();
	}
	
	/**
	 * Test method for {@link org.aksw.triple2nl.TripleConverter#convert(List)} .
	 */
	@Test
	public void testConvertTriplesToText() {
		//check conversion of set of triples for the same subject
		List<Triple> triples = new ArrayList<Triple>();
		Node subject = NodeFactory.createURI("http://dbpedia.org/resource/Albert_Einstein");
		triples.add(Triple.create(
				subject,
				RDF.type.asNode(),
				NodeFactory.createURI("http://dbpedia.org/ontology/Person")));
		triples.add(Triple.create(
				subject,
				NodeFactory.createURI("http://dbpedia.org/ontology/birthPlace"),
				NodeFactory.createURI("http://dbpedia.org/resource/Ulm")));
		triples.add(Triple.create(
				subject,
				NodeFactory.createURI("http://dbpedia.org/ontology/birthDate"),
				NodeFactory.createLiteral("1879-03-14", XSDDatatype.XSDdate)));
		
		String text = converter.convert(triples);
		System.out.println(triples + "\n-> " + text);
		assertEquals("Albert Einstein is a person. His birth place is Ulm and his birth date is 14 March 1879.", text);
		
		triples = new ArrayList<Triple>();
		triples.add(Triple.create(
				subject,
				RDF.type.asNode(),
				NodeFactory.createURI("http://dbpedia.org/ontology/Person")));
		triples.add(Triple.create(
				subject,
				NodeFactory.createURI("http://dbpedia.org/ontology/birthDate"),
				NodeFactory.createLiteral("1879-03-14", XSDDatatype.XSDdate)));
		
		//2 types
		triples = new ArrayList<Triple>();
		triples.add(Triple.create(
				subject,
				RDF.type.asNode(),
				NodeFactory.createURI("http://dbpedia.org/ontology/Physican")));
		triples.add(Triple.create(
				subject,
				RDF.type.asNode(),
				NodeFactory.createURI("http://dbpedia.org/ontology/Musican")));
		triples.add(Triple.create(
				subject,
				NodeFactory.createURI("http://dbpedia.org/ontology/birthDate"),
				NodeFactory.createLiteral("1879-03-14", XSDDatatype.XSDdate)));
		
		text = converter.convert(triples);
		System.out.println(triples + "\n-> " + text);
		assertEquals("Albert Einstein is a musican as well as a physican. His birth date is 14 March 1879.", text);
		
		//more than 2 types
		triples = new ArrayList<Triple>();
		triples.add(Triple.create(
				subject,
				RDF.type.asNode(),
				NodeFactory.createURI("http://dbpedia.org/ontology/Physican")));
		triples.add(Triple.create(
				subject,
				RDF.type.asNode(),
				NodeFactory.createURI("http://dbpedia.org/ontology/Musican")));
		triples.add(Triple.create(
				subject,
				RDF.type.asNode(),
				NodeFactory.createURI("http://dbpedia.org/ontology/Philosopher")));
		triples.add(Triple.create(
				subject,
				NodeFactory.createURI("http://dbpedia.org/ontology/birthDate"),
				NodeFactory.createLiteral("1879-03-14", XSDDatatype.XSDdate)));
		
		text = converter.convert(triples);
		System.out.println(triples + "\n-> " + text);
		assertEquals("Albert Einstein is a physican and a philosopher as well as a musican. His birth date is 14 March 1879.", text);
		
		//no type
		triples = new ArrayList<Triple>();
		triples.add(Triple.create(
				subject,
				NodeFactory.createURI("http://dbpedia.org/ontology/birthPlace"),
				NodeFactory.createURI("http://dbpedia.org/resource/Ulm")));
		triples.add(Triple.create(
				subject,
				NodeFactory.createURI("http://dbpedia.org/ontology/birthDate"),
				NodeFactory.createLiteral("1879-03-14", XSDDatatype.XSDdate)));
		
		text = converter.convert(triples);
		System.out.println(triples + "\n-> " + text);
		assertEquals("Albert Einstein's birth place is Ulm and his birth date is 14 March 1879.", text);

		// no type with verb
		triples = new ArrayList<Triple>();
		triples.add(Triple.create(
				subject,
				NodeFactory.createURI("http://dbpedia.org/ontology/birthPlace"),
				NodeFactory.createURI("http://dbpedia.org/resource/Ulm")));
		triples.add(Triple.create(
				subject,
				NodeFactory.createURI("http://dbpedia.org/ontology/influenced"),
				NodeFactory.createURI("http://dbpedia.org/resource/Nathan_Rosen")));

		text = converter.convert(triples);
		System.out.println(triples + "\n-> " + text);
		assertEquals("Albert Einstein's birth place is Ulm and he influenced Nathan Rosen.", text);
	}

	/**
	 * Test method for {@link org.aksw.triple2nl.TripleConverter#convert(com.hp.hpl.jena.graph.Triple)}.
	 */
	@Test
	public void testConvertTripleToTextTriple() {
		Triple t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/Leipzig"),
				NodeFactory.createURI("http://dbpedia.org/ontology/leaderParty"),
				NodeFactory.createURI("http://dbpedia.org/resource/Social_Democratic_Party_of_Germany"));
		String text = converter.convert(t);
		System.out.println(t + " -> " + text);
		assertEquals("Leipzig's leader party is Social Democratic Party of Germany.", text);
		
		t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/Brad_Pitt"),
				NodeFactory.createURI("http://dbpedia.org/ontology/isBornIn"),
				NodeFactory.createURI("http://dbpedia.org/resource/Shawnee,_Oklahoma"));
		text = converter.convert(t);
		System.out.println(t + " -> " + text);
		assertEquals("Brad Pitt is born in Shawnee, Oklahoma.", text);
		
		t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/Brad_Pitt"),
				RDF.type.asNode(),
				NodeFactory.createURI("http://dbpedia.org/ontology/OldActor"));
		text = converter.convert(t);
		System.out.println(t + " -> " + text);
		assertEquals("Brad Pitt is an old actor.", text);
		
		t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/Ferrari"),
				NodeFactory.createURI("http://dbpedia.org/ontology/hasColor"),
				NodeFactory.createURI("http://dbpedia.org/resource/red"));
		text = converter.convert(t);
		System.out.println(t + " -> " + text);
		assertEquals("Ferrari has color red.", text);
		
		t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/John"),
				NodeFactory.createURI("http://dbpedia.org/ontology/likes"),
				NodeFactory.createURI("http://dbpedia.org/resource/Mary"));
		text = converter.convert(t);
		System.out.println(t + " -> " + text);
		assertEquals("John likes Mary.", text);
		
		t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/Mount_Everest"),
				NodeFactory.createURI("http://dbpedia.org/ontology/height"),
				NodeFactory.createLiteral("8000", XSDDatatype.XSDinteger));
		text = converter.convert(t);
		System.out.println(t + " -> " + text);
		assertEquals("Mount Everest's height is 8000.", text);
		
		t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/Albert_Einstein"),
				NodeFactory.createURI("http://dbpedia.org/ontology/birthPlace"),
				NodeFactory.createURI("http://dbpedia.org/resource/Ulm"));
		text = converter.convert(t, false);
		System.out.println(t + " -> " + text);
		assertEquals("Albert Einstein's birth place is Ulm.", text);
	
		t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/Mount_Everest"),
				NodeFactory.createURI("http://dbpedia.org/ontology/isLargerThan"),
				NodeFactory.createURI("http://dbpedia.org/resource/K2"));
		text = converter.convert(t, false);
		System.out.println(t + " -> " + text);
		assertEquals("Mount Everest is larger than K 2.", text);
			
		t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/Albert_Einstein"),
				NodeFactory.createURI("http://dbpedia.org/ontology/birthDate"),
				NodeFactory.createLiteral("1879-03-14", XSDDatatype.XSDdate));
		text = converter.convert(t);
		System.out.println(t + " -> " + text);
		assertEquals("Albert Einstein's birth date is 14 March 1879.", text);
		
		t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/Albert_Einstein"),
				NodeFactory.createURI("http://dbpedia.org/ontology/birthDate"),
				NodeFactory.createVariable("date"));
		text = converter.convert(t);
		System.out.println(t + " -> " + text);
		assertEquals("Albert Einstein's birth date is ?date.", text);
		
		t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/Lionel_Messi"),
				NodeFactory.createURI("http://dbpedia.org/ontology/team"),
				NodeFactory.createVariable("team"));
		text = converter.convert(t);
		System.out.println(t + " -> " + text);
		assertEquals("Lionel Messi's team is ?team.", text);
		
		converter.setDeterminePluralForm(true);
		text = converter.convert(t);
		converter.setDeterminePluralForm(false);
		System.out.println(t + " -> " + text);
		assertEquals("Lionel Messi's teams are ?team.", text);
		
		t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/Living_Bird_III"),
				NodeFactory.createURI("http://dbpedia.org/ontology/isPeerReviewed"),
				NodeFactory.createVariable("isReviewed"));
		text = converter.convert(t);
		System.out.println(t + " -> " + text);
		assertEquals("Living Bird III is peer reviewed ?isReviewed.", text);
		
		t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/Lionel_Messi"),
				RDFS.label.asNode(),
				NodeFactory.createLiteral("Lionel Messi", "en", null));
		text = converter.convert(t);
		System.out.println(t + " -> " + text);
		assertEquals("Lionel Messi's English label is \"Lionel Messi\".", text);
		
		t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/London"),
				NodeFactory.createURI("http://dbpedia.org/ontology/PopulatedPlace/areaTotal"),
				NodeFactory.createLiteral("1572.122782973952", null, new BaseDatatype("http://dbpedia.org/datatype/squareKilometre")));
		text = converter.convert(t);
		System.out.println(t + " -> " + text);
		assertEquals("London's area total is 1572.122782973952 square kilometres.", text);
		
		
	}
	
	@Test
	public void testConvertBooleanValueTriples() throws Exception {
		Triple t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/Mathematics_of_Computation"),
				NodeFactory.createURI("http://dbpedia.org/ontology/isPeerReviewed"),
				NodeFactory.createLiteral("true", XSDDatatype.XSDboolean));
		String text = converter.convert(t);
		System.out.println(t + " -> " + text);
		assertEquals("Mathematics of Computation is peer reviewed.", text);
		
		t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/Living_Bird"),
				NodeFactory.createURI("http://dbpedia.org/ontology/isPeerReviewed"),
				NodeFactory.createLiteral("false", XSDDatatype.XSDboolean));
		text = converter.convert(t);
		System.out.println(t + " -> " + text);
		assertEquals("Living Bird is not peer reviewed.", text);
		
		t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/Usain_Bolt"),
				NodeFactory.createURI("http://dbpedia.org/ontology/isGoldMedalWinner"),
				NodeFactory.createLiteral("false", XSDDatatype.XSDboolean));
		text = converter.convert(t);
		System.out.println(t + " -> " + text);
		assertEquals("Usain Bolt is not gold medal winner.", text);
		
		t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/Albury_railway_station"),
				NodeFactory.createURI("http://dbpedia.org/ontology/isHandicappedAccessible"),
				NodeFactory.createLiteral("false", XSDDatatype.XSDboolean));
		text = converter.convert(t);
		System.out.println(t + " -> " + text);
		assertEquals("Albury railway station is not handicapped accessible.", text);
	}
	
	@Test
	public void testPassiveTriples() throws Exception {
		Triple t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/Baruch_Spinoza"),
				NodeFactory.createURI("http://dbpedia.org/ontology/influenced"),
				NodeFactory.createURI("http://dbpedia.org/ontology/Albert_Einstein"));
		String text = converter.convert(t);
		System.out.println(t + " -> " + text);
//		assertEquals("Mathematics of Computation is peer reviewed", text);
		
		t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/Baruch_Spinoza"),
				NodeFactory.createURI("http://dbpedia.org/ontology/influencedBy"),
				NodeFactory.createURI("http://dbpedia.org/ontology/Albert_Einstein"));
		text = converter.convert(t);
		System.out.println(t + " -> " + text);
//		assertEquals("Living Bird is not peer reviewed", text);
		
	}

	@Test
	public void testConvertTriplesNoOntology() throws Exception {
		TripleConverter converter = new TripleConverter();
		Triple t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/Baruch_Spinoza"),
				NodeFactory.createURI("http://dbpedia.org/ontology/influenced"),
				NodeFactory.createURI("http://dbpedia.org/ontology/Albert_Einstein"));
		String text = converter.convert(t);
		System.out.println(t + " -> " + text);
//		assertEquals("Mathematics of Computation is peer reviewed", text);

		t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/Baruch_Spinoza"),
				NodeFactory.createURI("http://dbpedia.org/ontology/influencedBy"),
				NodeFactory.createURI("http://dbpedia.org/ontology/Albert_Einstein"));
		text = converter.convert(t);
		System.out.println(t + " -> " + text);
//		assertEquals("Living Bird is not peer reviewed", text);

	}

	@Test
	public void testConvertTriplePatterns() throws Exception {
		TripleConverter converter = new TripleConverter();
		Triple t = Triple.create(
				NodeFactory.createVariable("person"),
				NodeFactory.createURI("http://dbpedia.org/ontology/occupation"),
				NodeFactory.createURI("http://dbpedia.org/resource/Writer"));
		String text = converter.convert(t);
		System.out.println(t + " -> " + text);

		t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/ontology/Albert"),
				NodeFactory.createURI("http://dbpedia.org/ontology/occupation"),
				NodeFactory.createURI("http://dbpedia.org/resource/Writer"));
		SPhraseSpec phrase = converter.convertToPhrase(t);
		phrase.getSubject().getChildren().get(0).setFeature(Feature.PRONOMINAL, true);

		System.out.println(t + " -> " + r.realise(phrase));

	}
}
