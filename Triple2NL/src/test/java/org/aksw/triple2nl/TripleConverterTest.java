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

import simplenlg.lexicon.Lexicon;

/**
 * @author Lorenz Buehmann
 *
 */
public class TripleConverterTest {
	
	private static final SparqlEndpoint ENDPOINT_DBPEDIA = SparqlEndpoint.getEndpointDBpedia();
	private static final SparqlEndpointKS KS = new SparqlEndpointKS(ENDPOINT_DBPEDIA);
	
	private static TripleConverter converter;
	
	@BeforeClass
	public static void init() throws Exception {
		KS.init();
		
		converter = new TripleConverter(KS.getQueryExecutionFactory(), "cache", (Lexicon)null);
	}
	
	/**
	 * Test method for {@link org.aksw.triple2nl.TripleConverter#convertTriplesToText(java.util.Collection)}.
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
		
		String text = converter.convertTriplesToText(triples);
		System.out.println(triples + "\n-> " + text);
		assertEquals("Albert Einstein is a person, whose's birth place is Ulm and whose's birth date is 14 March 1879.", text);
		
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
		
		text = converter.convertTriplesToText(triples);
		System.out.println(triples + "\n-> " + text);
		assertEquals("Albert Einstein is a musican as well as a physican and its birth date is 14 March 1879.", text);
		
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
		
		text = converter.convertTriplesToText(triples);
		System.out.println(triples + "\n-> " + text);
		assertEquals("Albert Einstein is a physican and a philosopher as well as a musican and its birth date is 14 March 1879.", text);
		
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
		
		text = converter.convertTriplesToText(triples);
		System.out.println(triples + "\n-> " + text);
		assertEquals("Albert Einstein's birth place is Ulm and its birth date is 14 March 1879.", text);
	}

	/**
	 * Test method for {@link org.aksw.triple2nl.TripleConverter#convertTripleToText(com.hp.hpl.jena.graph.Triple)}.
	 */
	@Test
	public void testConvertTripleToTextTriple() {
		Triple t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/Leipzig"),
				NodeFactory.createURI("http://dbpedia.org/ontology/leaderParty"),
				NodeFactory.createURI("http://dbpedia.org/resource/Social_Democratic_Party_of_Germany"));
		String text = converter.convertTripleToText(t);
		System.out.println(t + " -> " + text);
		assertEquals("Leipzig's leader party is Social Democratic Party of Germany", text);
		
		t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/Brad_Pitt"),
				NodeFactory.createURI("http://dbpedia.org/ontology/isBornIn"),
				NodeFactory.createURI("http://dbpedia.org/resource/Shawnee,_Oklahoma"));
		text = converter.convertTripleToText(t);
		System.out.println(t + " -> " + text);
		assertEquals("Brad Pitt is born in Shawnee, Oklahoma", text);
		
		t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/Brad_Pitt"),
				RDF.type.asNode(),
				NodeFactory.createURI("http://dbpedia.org/ontology/OldActor"));
		text = converter.convertTripleToText(t);
		System.out.println(t + " -> " + text);
		assertEquals("Brad Pitt is an old actor", text);
		
		t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/Ferrari"),
				NodeFactory.createURI("http://dbpedia.org/ontology/hasColor"),
				NodeFactory.createURI("http://dbpedia.org/resource/red"));
		text = converter.convertTripleToText(t);
		System.out.println(t + " -> " + text);
		assertEquals("Ferrari has color red", text);
		
		t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/John"),
				NodeFactory.createURI("http://dbpedia.org/ontology/likes"),
				NodeFactory.createURI("http://dbpedia.org/resource/Mary"));
		text = converter.convertTripleToText(t);
		System.out.println(t + " -> " + text);
		assertEquals("John likes Mary", text);
		
		t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/Mount_Everest"),
				NodeFactory.createURI("http://dbpedia.org/ontology/height"),
				NodeFactory.createLiteral("8000", XSDDatatype.XSDinteger));
		text = converter.convertTripleToText(t);
		System.out.println(t + " -> " + text);
		assertEquals("Mount Everest's height is 8000", text);
		
		t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/Albert_Einstein"),
				NodeFactory.createURI("http://dbpedia.org/ontology/birthPlace"),
				NodeFactory.createURI("http://dbpedia.org/resource/Ulm"));
		text = converter.convertTripleToText(t, false);
		System.out.println(t + " -> " + text);
		assertEquals("Albert Einstein's birth place is Ulm", text);
	
		t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/Mount_Everest"),
				NodeFactory.createURI("http://dbpedia.org/ontology/isLargerThan"),
				NodeFactory.createURI("http://dbpedia.org/resource/K2"));
		text = converter.convertTripleToText(t, false);
		System.out.println(t + " -> " + text);
		assertEquals("Mount Everest is larger than K 2", text);
			
		t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/Albert_Einstein"),
				NodeFactory.createURI("http://dbpedia.org/ontology/birthDate"),
				NodeFactory.createLiteral("1879-03-14", XSDDatatype.XSDdate));
		text = converter.convertTripleToText(t);
		System.out.println(t + " -> " + text);
		assertEquals("Albert Einstein's birth date is 14 March 1879", text);
		
		t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/Albert_Einstein"),
				NodeFactory.createURI("http://dbpedia.org/ontology/birthDate"),
				NodeFactory.createVariable("date"));
		text = converter.convertTripleToText(t);
		System.out.println(t + " -> " + text);
		assertEquals("Albert Einstein's birth date is ?date", text);
		
		t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/Lionel_Messi"),
				NodeFactory.createURI("http://dbpedia.org/ontology/team"),
				NodeFactory.createVariable("team"));
		text = converter.convertTripleToText(t);
		System.out.println(t + " -> " + text);
		assertEquals("Lionel Messi's team is ?team", text);
		
		converter.setDeterminePluralForm(true);
		text = converter.convertTripleToText(t);
		converter.setDeterminePluralForm(false);
		System.out.println(t + " -> " + text);
		assertEquals("Lionel Messi's teams are ?team", text);
		
		t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/Living_Bird_III"),
				NodeFactory.createURI("http://dbpedia.org/ontology/isPeerReviewed"),
				NodeFactory.createVariable("isReviewed"));
		text = converter.convertTripleToText(t);
		System.out.println(t + " -> " + text);
		assertEquals("Living Bird III is peer reviewed ?isReviewed", text);
		
		t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/Lionel_Messi"),
				RDFS.label.asNode(),
				NodeFactory.createLiteral("Lionel Messi", "en", null));
		text = converter.convertTripleToText(t);
		System.out.println(t + " -> " + text);
		assertEquals("Lionel Messi's English label is \"Lionel Messi\"", text);
		
		t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/London"),
				NodeFactory.createURI("http://dbpedia.org/ontology/PopulatedPlace/areaTotal"),
				NodeFactory.createLiteral("1572.122782973952", null, new BaseDatatype("http://dbpedia.org/datatype/squareKilometre")));
		text = converter.convertTripleToText(t);
		System.out.println(t + " -> " + text);
		assertEquals("London's area total is 1572.122782973952 square kilometres", text);
		
		
	}
	
	@Test
	public void testConvertBooleanValueTriples() throws Exception {
		Triple t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/Mathematics_of_Computation"),
				NodeFactory.createURI("http://dbpedia.org/ontology/isPeerReviewed"),
				NodeFactory.createLiteral("true", XSDDatatype.XSDboolean));
		String text = converter.convertTripleToText(t);
		System.out.println(t + " -> " + text);
		assertEquals("Mathematics of Computation is peer reviewed", text);
		
		t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/Living_Bird"),
				NodeFactory.createURI("http://dbpedia.org/ontology/isPeerReviewed"),
				NodeFactory.createLiteral("false", XSDDatatype.XSDboolean));
		text = converter.convertTripleToText(t);
		System.out.println(t + " -> " + text);
		assertEquals("Living Bird is not peer reviewed", text);
		
		t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/Usain_Bolt"),
				NodeFactory.createURI("http://dbpedia.org/ontology/isGoldMedalWinner"),
				NodeFactory.createLiteral("false", XSDDatatype.XSDboolean));
		text = converter.convertTripleToText(t);
		System.out.println(t + " -> " + text);
		assertEquals("Usain Bolt is not gold medal winner", text);
		
		t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/Albury_railway_station"),
				NodeFactory.createURI("http://dbpedia.org/ontology/isHandicappedAccessible"),
				NodeFactory.createLiteral("false", XSDDatatype.XSDboolean));
		text = converter.convertTripleToText(t);
		System.out.println(t + " -> " + text);
		assertEquals("Albury railway station is not handicapped accessible", text);
	}
	
	@Test
	public void testPassiveTriples() throws Exception {
		Triple t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/Baruch_Spinoza"),
				NodeFactory.createURI("http://dbpedia.org/ontology/influenced"),
				NodeFactory.createURI("http://dbpedia.org/ontology/Albert_Einstein"));
		String text = converter.convertTripleToText(t);
		System.out.println(t + " -> " + text);
//		assertEquals("Mathematics of Computation is peer reviewed", text);
		
		t = Triple.create(
				NodeFactory.createURI("http://dbpedia.org/resource/Baruch_Spinoza"),
				NodeFactory.createURI("http://dbpedia.org/ontology/influencedBy"),
				NodeFactory.createURI("http://dbpedia.org/ontology/Albert_Einstein"));
		text = converter.convertTripleToText(t);
		System.out.println(t + " -> " + text);
//		assertEquals("Living Bird is not peer reviewed", text);
		
	}
}
