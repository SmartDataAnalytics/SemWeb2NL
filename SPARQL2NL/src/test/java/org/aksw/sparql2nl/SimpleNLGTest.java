/*
 * #%L
 * SPARQL2NL
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
package org.aksw.sparql2nl;

import org.aksw.sparql2nl.naturallanguagegeneration.TriplePatternConverter;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.junit.BeforeClass;
import org.junit.Test;
import simplenlg.features.Feature;
import simplenlg.features.Gender;
import simplenlg.features.LexicalFeature;
import simplenlg.framework.LexicalCategory;
import simplenlg.framework.NLGElement;
import simplenlg.framework.NLGFactory;
import simplenlg.lexicon.Lexicon;
import simplenlg.lexicon.NIHDBLexicon;
import simplenlg.phrasespec.NPPhraseSpec;
import simplenlg.phrasespec.SPhraseSpec;
import simplenlg.realiser.english.Realiser;

/**
 * @author Lorenz Buehmann
 *
 */
public class SimpleNLGTest {
	
	
	private static Lexicon lexicon;
	private static NLGFactory nlgFactory;
	private static Realiser realiser;

	@BeforeClass
	public static void setUp(){
		lexicon = Lexicon.getDefaultLexicon();
		nlgFactory = new NLGFactory(lexicon);
		realiser = new Realiser(lexicon);
	}
	

	@Test
	public void testDefaultLexicon() {
		String cls = "airport";
		NLGElement word = nlgFactory.createWord(cls, LexicalCategory.NOUN);
		NLGElement nounPhrase = nlgFactory.createNounPhrase(word);
		System.out.println(nounPhrase.getAllFeatures());
		System.out.println(nounPhrase.getRealisation());
		nounPhrase.setFeature(Feature.POSSESSIVE, true);
		nounPhrase = realiser.realise(nounPhrase);
		System.out.println(nounPhrase.getAllFeatures());
		System.out.println(nounPhrase.getRealisation());
		
		word = nlgFactory.createWord(cls, LexicalCategory.NOUN);
		nounPhrase = nlgFactory.createNounPhrase(word);
		nounPhrase = realiser.realise(nounPhrase);
		System.out.println(nounPhrase.getAllFeatures());
		System.out.println(nounPhrase.getRealisation());
	}
	
	//@Test
	public void testNIHLexicon() {
		String cls = "airport";
		Lexicon lexicon = new NIHDBLexicon("/home/me/tools/lexAccess2013lite/data/HSqlDb/lexAccess2013.data");
		NLGFactory nlgFactory = new NLGFactory(lexicon);
		Realiser realiser = new Realiser(lexicon);
		NLGElement word = nlgFactory.createWord(cls, LexicalCategory.NOUN);
		NLGElement nounPhrase = nlgFactory.createNounPhrase(word);
		nounPhrase = realiser.realise(nounPhrase);
		System.out.println(nounPhrase.getAllFeatures());
		System.out.println(nounPhrase.getRealisation());
	}
	
	@Test
	public void testRelativeClause(){
		SPhraseSpec cl1 = nlgFactory.createClause(null, "have", "system of government Republic");
		NPPhraseSpec np1 = nlgFactory.createNounPhrase("states");
		np1.addComplement(cl1);
		System.out.println(realiser.realise(np1));
		
		SPhraseSpec cl2 = nlgFactory.createClause(null, "be located in", nlgFactory.createClause(np1, null));
		NPPhraseSpec np2 = nlgFactory.createNounPhrase("cities");
		np2.addComplement(cl2);
		System.out.println(realiser.realise(np2));
		
		Triple t = new Triple(
				NodeFactory.createVariable("city"),
				NodeFactory.createURI("http://dbpedia.org/ontology/locatedIn"),
				NodeFactory.createVariable("state"));
		
		TriplePatternConverter conv = new TriplePatternConverter(SparqlEndpoint.getEndpointDBpedia(), "cache", null);
		NPPhraseSpec cl3 = conv.convertTriplePattern(t, nlgFactory.createNounPhrase("cities"), np1, true, false, false);
		System.out.println(realiser.realise(cl3));
        
        NPPhraseSpec np = nlgFactory.createNounPhrase(nlgFactory.createWord("darts player", LexicalCategory.NOUN));
        np.setPlural(true);
        System.out.println(realiser.realise(np));
	}
	
	@Test
	public void testPossessive() throws Exception {
		NPPhraseSpec sisterNP = nlgFactory.createNounPhrase("sister");
		NLGElement word = nlgFactory.createWord("Albert Einstein", LexicalCategory.NOUN);
		word.setFeature(LexicalFeature.PROPER, true);
		NPPhraseSpec possNP = nlgFactory.createNounPhrase(word);
		possNP.setFeature(Feature.POSSESSIVE, true);
		sisterNP.setSpecifier(possNP);
		System.out.println(realiser.realise(sisterNP));
		sisterNP.setPlural(true);
		System.out.println(realiser.realise(sisterNP));
		sisterNP.setPlural(false);
		possNP.setFeature(LexicalFeature.GENDER, Gender.MASCULINE);
        possNP.setFeature(Feature.PRONOMINAL, true);
        System.out.println(realiser.realise(sisterNP));
        possNP.setPlural(false);
        sisterNP.setPlural(true);
        System.out.println(realiser.realise(sisterNP));
	}

}
