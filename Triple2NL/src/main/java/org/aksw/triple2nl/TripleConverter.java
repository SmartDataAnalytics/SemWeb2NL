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

import com.google.common.collect.Lists;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import net.sf.extjwnl.dictionary.Dictionary;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.jena_sparql_api.model.QueryExecutionFactoryModel;
import org.aksw.triple2nl.converter.DefaultIRIConverter;
import org.aksw.triple2nl.converter.IRIConverter;
import org.aksw.triple2nl.converter.LiteralConverter;
import org.aksw.triple2nl.gender.Gender;
import org.aksw.triple2nl.gender.GenderDetector;
import org.aksw.triple2nl.gender.DictionaryBasedGenderDetector;
import org.aksw.triple2nl.nlp.relation.BoaPatternSelector;
import org.aksw.triple2nl.nlp.stemming.PlingStemmer;
import org.aksw.triple2nl.property.PropertyVerbalization;
import org.aksw.triple2nl.property.PropertyVerbalizationType;
import org.aksw.triple2nl.property.PropertyVerbalizer;
import org.aksw.triple2nl.util.GenericType;
import org.apache.commons.collections15.ListUtils;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.reasoning.SPARQLReasoner;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simplenlg.features.Feature;
import simplenlg.features.InternalFeature;
import simplenlg.features.LexicalFeature;
import simplenlg.framework.*;
import simplenlg.lexicon.Lexicon;
import simplenlg.phrasespec.NPPhraseSpec;
import simplenlg.phrasespec.SPhraseSpec;
import simplenlg.realiser.english.Realiser;
import uk.ac.manchester.cs.owl.owlapi.OWLDataPropertyImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectPropertyImpl;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Convert triple(s) into natural language.
 * @author Lorenz Buehmann
 * 
 */
public class TripleConverter {
	
	private static final Logger logger = LoggerFactory.getLogger(TripleConverter.class);

	private static String DEFAULT_CACHE_BASE_DIR = System.getProperty("java.io.tmpdir");
	private static String DEFAULT_CACHE_DIR = DEFAULT_CACHE_BASE_DIR + "/triple2nl-cache";

	public Lexicon lexicon;
	public NLGFactory nlgFactory;
	public Realiser realiser;

	private IRIConverter uriConverter;
	private LiteralConverter literalConverter;
	private PropertyVerbalizer pp;
	private SPARQLReasoner reasoner;
	
	private boolean determinePluralForm = false;
	//show language as adjective for literals
	private boolean considerLiteralLanguage = true;
	//encapsulate string literals in quotes ""
	private boolean encapsulateStringLiterals = true;
	//for multiple types use 'as well as' to coordinate the last type
	private boolean useAsWellAsCoordination = true;

	private boolean returnAsSentence = true;

	private boolean useGenderInformation = true;

	private GenderDetector genderDetector;

	public TripleConverter() {
		this(new QueryExecutionFactoryModel(ModelFactory.createDefaultModel()), DEFAULT_CACHE_DIR, Lexicon.getDefaultLexicon());
	}

	public TripleConverter(SparqlEndpoint endpoint) {
		this(endpoint, DEFAULT_CACHE_DIR);
	}

	public TripleConverter(QueryExecutionFactory qef, String cacheDirectory) {
		this(qef, cacheDirectory, (Dictionary)null);
	}
	
	public TripleConverter(QueryExecutionFactory qef, String cacheDirectory, Dictionary wordnetDirectory) {
		this(qef, null, null, cacheDirectory, wordnetDirectory, null);
	}
	
	public TripleConverter(SparqlEndpoint endpoint, String cacheDirectory) {
		this(endpoint, cacheDirectory, null);
	}
	
	public TripleConverter(SparqlEndpoint endpoint, String cacheDirectory, Dictionary wordnetDirectory) {
		this(new QueryExecutionFactoryHttp(endpoint.getURL().toString(), endpoint.getDefaultGraphURIs()), 
				null, null, cacheDirectory, wordnetDirectory, Lexicon.getDefaultLexicon());
	}
	
	public TripleConverter(SparqlEndpoint endpoint, String cacheDirectory, Dictionary wordnetDirectory, Lexicon lexicon) {
		this(new QueryExecutionFactoryHttp(endpoint.getURL().toString(), endpoint.getDefaultGraphURIs()), null,
				null, cacheDirectory, wordnetDirectory, lexicon);
	}

	public TripleConverter(QueryExecutionFactory qef, IRIConverter uriConverter, String cacheDirectory, Dictionary wordnetDirectory) {
		this(qef, null, uriConverter, cacheDirectory, wordnetDirectory, Lexicon.getDefaultLexicon());
	}
	
	public TripleConverter(QueryExecutionFactory qef, String cacheDirectory, Lexicon lexicon) {
		this(qef, null, null, cacheDirectory, null, lexicon);
	}
	
	public TripleConverter(QueryExecutionFactory qef, PropertyVerbalizer propertyVerbalizer, IRIConverter uriConverter, String cacheDirectory, Dictionary wordnetDirectory, Lexicon lexicon) {
		if(uriConverter == null){
			uriConverter = new DefaultIRIConverter(qef, cacheDirectory);
		}
		this.uriConverter = uriConverter;
		
		if(propertyVerbalizer == null){
			propertyVerbalizer = new PropertyVerbalizer(uriConverter, wordnetDirectory);
		}
		pp = propertyVerbalizer;

		this.lexicon = (lexicon == null) ? Lexicon.getDefaultLexicon() : lexicon;
		
		nlgFactory = new NLGFactory(lexicon);
		realiser = new Realiser(lexicon);
		
		literalConverter = new LiteralConverter(uriConverter);
		literalConverter.setEncapsulateStringLiterals(encapsulateStringLiterals);
		
		reasoner = new SPARQLReasoner(qef);

		genderDetector = new DictionaryBasedGenderDetector();
	}
	
	/**
	 * Return a textual representation for the given triple.
	 *
	 * @param t the triple to convert
	 * @return the textual representation
	 */
	public String convert(Triple t){
		return convert(t, false);
	}

	/**
	 * Return a textual representation for the given triple.
	 *
	 * @param t the triple to convert
	 * @param negated if phrase is negated
	 * @return the textual representation
	 */
	public String convert(Triple t, boolean negated){
		NLGElement phrase = convertToPhrase(t, negated);
		String text;
		if(returnAsSentence) {
			text = realiser.realiseSentence(phrase);
		} else {
			text = realiser.realise(phrase).getRealisation();
		}
		return text;
	}
	
	/**
	 * Return a textual representation for the given triples.
	 * Currently we assume that all triples have the same subject!
	 * 
	 * @param triples the triples to convert
	 * @return the textual representation
	 */
	public String convert(List<Triple> triples){
		// combine with conjunction
		CoordinatedPhraseElement typesConjunction = nlgFactory.createCoordinatedPhrase();
		
		// separate type triples from others
		List<Triple> typeTriples = triples.stream().filter(t -> t.predicateMatches(RDF.type.asNode())).collect(Collectors.toList());
		List<Triple> otherTriples = ListUtils.subtract(triples, typeTriples);

		// convert the type triples
		List<SPhraseSpec> typePhrases = convertToPhrases(typeTriples);
		
		// if there is more than one type, we combine them into a single clause
		if(typePhrases.size() > 1){
			// combine all objects in a coordinated phrase
			CoordinatedPhraseElement combinedObject = nlgFactory.createCoordinatedPhrase();
			
			// the last 2 phrases are combined via 'as well as'
			if(useAsWellAsCoordination){
				SPhraseSpec phrase1 = typePhrases.remove(typePhrases.size() - 1);
				SPhraseSpec phrase2 = typePhrases.get(typePhrases.size() - 1);
				// combine all objects in a coordinated phrase
				CoordinatedPhraseElement combinedLastTwoObjects = nlgFactory.createCoordinatedPhrase(phrase1.getObject(), phrase2.getObject());
				combinedLastTwoObjects.setConjunction("as well as");
				combinedLastTwoObjects.setFeature(Feature.RAISE_SPECIFIER, false);
				combinedLastTwoObjects.setFeature(InternalFeature.SPECIFIER, "a");
				phrase2.setObject(combinedLastTwoObjects);
			}
			
			Iterator<SPhraseSpec> iterator = typePhrases.iterator();
			// pick first phrase as representative
			SPhraseSpec representative = iterator.next();
			combinedObject.addCoordinate(representative.getObject());
			
			while(iterator.hasNext()){
				SPhraseSpec phrase = iterator.next();
				NLGElement object = phrase.getObject();
				combinedObject.addCoordinate(object);
			}
			
			combinedObject.setFeature(Feature.RAISE_SPECIFIER, true);
			// set the coordinated phrase as the object
			representative.setObject(combinedObject);
			// return a single phrase
			typePhrases = Lists.newArrayList(representative);
		}
		for (SPhraseSpec phrase : typePhrases) {
			typesConjunction.addCoordinate(phrase);
		}
		
		// convert the other triples
		CoordinatedPhraseElement othersConjunction = nlgFactory.createCoordinatedPhrase();
		List<SPhraseSpec> otherPhrases = convertToPhrases(otherTriples);
		// we have to keep one triple with subject if we have no type triples
		if(typeTriples.isEmpty()) {
			othersConjunction.addCoordinate(otherPhrases.remove(0));
		}
		// make subject pronominal, i.e. -> he/she/it
		otherPhrases.stream().forEach(p -> asPronoun(p.getSubject()));
		for (SPhraseSpec phrase : otherPhrases) {
			othersConjunction.addCoordinate(phrase);
		}

		List<DocumentElement> sentences = new ArrayList();
		if(!typeTriples.isEmpty()) {
			sentences.add(nlgFactory.createSentence(typesConjunction));
		}

		if(!otherTriples.isEmpty()) {
			sentences.add(nlgFactory.createSentence(othersConjunction));
		}

		DocumentElement paragraph = nlgFactory.createParagraph(sentences);
		String realisation = realiser.realise(paragraph).getRealisation().trim();

		return realisation;
	}

	private void asPronoun(NLGElement el) {
		if(el.hasFeature(InternalFeature.SPECIFIER)) {
			NLGElement specifier = el.getFeatureAsElement(InternalFeature.SPECIFIER);
			if(specifier.hasFeature(Feature.POSSESSIVE)) {
				specifier.setFeature(Feature.PRONOMINAL, true);
			}
		} else {
			el.setFeature(Feature.PRONOMINAL, true);
		}
	}

	/**
	 * Convert a triple into a phrase object
	 * 
	 * @param t the triple
	 * @return the phrase
	 */
	public SPhraseSpec convertToPhrase(Triple t) {
		return convertToPhrase(t, false);
	}
	
	/**
	 * Convert a triple into a phrase object
	 * 
	 * @param t the triple
	 * @return the phrase
	 */
	public SPhraseSpec convertToPhrase(Triple t, boolean negated) {
		return convertToPhrase(t, negated, false);
	}

	/**
	 * Convert a triple into a phrase object
	 *
	 * @param t       the triple
	 * @param negated if phrase is negated
	 * @param reverse whether subject and object should be changed during verbalization
	 * @return the phrase
	 */
	public SPhraseSpec convertToPhrase(Triple t, boolean negated, boolean reverse) {
		logger.debug("Verbalizing triple " + t);
		SPhraseSpec p = nlgFactory.createClause();

		Node subject = t.getSubject();
		Node predicate = t.getPredicate();
		Node object = t.getObject();

		// process predicate
		// start with variables
		if (predicate.isVariable()) {
			// if subject is variable then use variable label, else generate
			// textual representation
			// first get the string representation for the subject
			NLGElement subjectElement = processSubject(subject);
			p.setSubject(subjectElement);

			// predicate is variable, thus simply use variable label
			p.setVerb("be related via " + predicate.toString() + " to");

			// then process the object
			NLGElement objectElement = processObject(object, false);
			p.setObject(objectElement);
		} // more interesting case. Predicate is not a variable
			// then check for noun and verb. If the predicate is a noun or a
			// verb, then
			// use possessive or verbal form, else simply get the boa pattern
		else {
			//check if object is class
			boolean objectIsClass = predicate.matches(RDF.type.asNode());
			
			// first get the string representation for the subject
			NLGElement subjectElement = processSubject(subject);

			// then process the object
			NPPhraseSpec objectElement = processObject(object, objectIsClass);

			// handle the predicate
			PropertyVerbalization propertyVerbalization = pp.verbalize(predicate.getURI());
			String predicateAsString = propertyVerbalization.getVerbalizationText();
			
			// if the object is a class we generate 'SUBJECT be a(n) OBJECT'
			if (objectIsClass) {
				p.setSubject(subjectElement);
				p.setVerb("be");
				objectElement.setSpecifier("a");
				p.setObject(objectElement);
			} else {
				// get the lexicalization type of the predicate
				
				PropertyVerbalizationType type;
				if (predicate.matches(RDFS.label.asNode())) {
					type = PropertyVerbalizationType.NOUN;
				} else {
					type = propertyVerbalization.getVerbalizationType();
				}
				
				/*-
				 * if the predicate is a noun we generate a possessive form, i.e. 'SUBJECT'S PREDICATE be OBJECT'
				 */
				if (type == PropertyVerbalizationType.NOUN) {
					//subject is a noun with possessive feature
//					NLGElement subjectWord = nlgFactory.createInflectedWord(realiser.realise(subjectElement).getRealisation(), LexicalCategory.NOUN);
//					subjectWord.setFeature(LexicalFeature.PROPER, true);
//					subjectElement = nlgFactory.createNounPhrase(subjectWord);
					subjectElement.setFeature(Feature.POSSESSIVE, true);
	                //build the noun phrase for the predicate
	                NPPhraseSpec predicateNounPhrase = nlgFactory.createNounPhrase(PlingStemmer.stem(predicateAsString));
	                //set the possessive subject as specifier
	                predicateNounPhrase.setFeature(InternalFeature.SPECIFIER, subjectElement);
					
					//check if object is a string literal with a language tag
					if(considerLiteralLanguage){
						if(object.isLiteral() && object.getLiteralLanguage() != null && !object.getLiteralLanguage().isEmpty()){
							String languageTag = object.getLiteralLanguage();
							String language = Locale.forLanguageTag(languageTag).getDisplayLanguage(Locale.ROOT);
							predicateNounPhrase.setPreModifier(language);
						}
					}
					
					p.setSubject(predicateNounPhrase);
					
					// we use 'be' as the new predicate
					p.setVerb("be");
					
					//add object
					p.setObject(objectElement);
					
					//check if we have to use the plural form
					//simple heuristic: OBJECT is variable and predicate is of type owl:FunctionalProperty or rdfs:range is xsd:boolean
					boolean isPlural = determinePluralForm && usePluralForm(t);
					predicateNounPhrase.setPlural(isPlural);
					p.setPlural(isPlural);
					
					//check if we reverse the triple representation
					if(reverse){
						subjectElement.setFeature(Feature.POSSESSIVE, false);
			        	p.setSubject(subjectElement);
			        	p.setVerbPhrase(nlgFactory.createVerbPhrase("be " + predicateAsString + " of"));
			        	p.setObject(objectElement);
					}
				}// if the predicate is a verb 
				else if (type == PropertyVerbalizationType.VERB) {
					p.setSubject(subjectElement);
					p.setVerb(pp.getInfinitiveForm(predicateAsString));
					p.setObject(objectElement);
					p.setFeature(Feature.TENSE, propertyVerbalization.getTense());
				}// in other cases, use the BOA pattern
				else {

					List<org.aksw.triple2nl.nlp.relation.Pattern> l = BoaPatternSelector
							.getNaturalLanguageRepresentation(predicate.toString(), 1);
					if (l.size() > 0) {
						String boaPattern = l.get(0).naturalLanguageRepresentation;
						// range before domain
						if (boaPattern.startsWith("?R?")) {
							p.setSubject(subjectElement);
							p.setObject(objectElement);
						} else {
							p.setObject(subjectElement);
							p.setSubject(objectElement);
						}
						p.setVerb(BoaPatternSelector.getNaturalLanguageRepresentation(predicate.toString(), 1)
								.get(0).naturalLanguageRepresentationWithoutVariables);
					} // last resort, i.e., no BOA pattern found
					else {
						p.setSubject(subjectElement);
						p.setVerb("be related via \"" + predicateAsString + "\" to");
						p.setObject(objectElement);
					}
				}
			}
		}
		//check if the meaning of the triple is it's negation, which holds for boolean properties with FALSE as value
		if(!negated){
			//check if object is boolean literal
			if(object.isLiteral() && object.getLiteralDatatype() != null && object.getLiteralDatatype().equals(XSDDatatype.XSDboolean)){
				//omit the object
				p.setObject(null);
				
				negated = !(boolean) object.getLiteralValue();
				
			}
		}
		
		// set negation
		if(negated){
			p.setFeature(Feature.NEGATED, negated);
		}
		
		// set present time as tense
//		p.setFeature(Feature.TENSE, Tense.PRESENT);
//		System.out.println(realiser.realise(p));
		return p;
	}

	/**
	 * Converts a collection of triples into a list of phrases.
	 *
	 * @param triples the triples
	 * @return a list of phrases
	 */
	public List<SPhraseSpec> convertToPhrases(Collection<Triple> triples) {
		List<SPhraseSpec> phrases = new ArrayList<>();
		for (Triple triple : triples) {
			phrases.add(convertToPhrase(triple));
		}
		return phrases;
	}
	
	
	/**
	 * Whether to encapsulate the value of string literals in "".
	 * {@see LiteralConverter#setEncapsulateStringLiterals(boolean)}
	 * @param encapsulateStringLiterals TRUE if string has to be wrapped in "", otherwise FALSE
	 */
	public void setEncapsulateStringLiterals(boolean encapsulateStringLiterals) {
		this.literalConverter.setEncapsulateStringLiterals(encapsulateStringLiterals);
	}
	
	/**
	 * @param determinePluralForm the determinePluralForm to set
	 */
	public void setDeterminePluralForm(boolean determinePluralForm) {
		this.determinePluralForm = determinePluralForm;
	}
	
	/**
	 * @param considerLiteralLanguage the considerLiteralLanguage to set
	 */
	public void setConsiderLiteralLanguage(boolean considerLiteralLanguage) {
		this.considerLiteralLanguage = considerLiteralLanguage;
	}
	
	private boolean usePluralForm(Triple triple){
		return triple.getObject().isVariable() 
				&& !(reasoner.isFunctional(
						new OWLObjectPropertyImpl(IRI.create(triple.getPredicate().getURI()))) 
					|| reasoner.getRange(
							new OWLDataPropertyImpl(IRI.create(triple.getPredicate().getURI()))).asOWLDatatype().getIRI().equals(OWL2Datatype.XSD_BOOLEAN.getIRI()));
	}

	/**
	 * @param returnAsSentence whether the style of the returned result is a proper English sentence or just a phrase
	 */
	public void setReturnAsSentence(boolean returnAsSentence) {
		this.returnAsSentence = returnAsSentence;
	}

	/**
	 * @param useGenderInformation whether to use the gender information about a resource
	 */
	public void setUseGenderInformation(boolean useGenderInformation) {
		this.useGenderInformation = useGenderInformation;
	}

	public void setGenderDetector(GenderDetector genderDetector) {
		this.genderDetector = genderDetector;
	}

	/**
	 * Process the node and return an NLG element that contains the textual
	 * representation. The output depends on the node type, i.e.
	 * variable, URI or literal.
	 * 
	 * @param node the node to process
	 * @return the NLG element containing the textual representation of the node
	 */
	public NLGElement processNode(Node node) {
		NLGElement element;
		if (node.isVariable()) {
			element = processVarNode(node);
		} else if(node.isURI()){
			element = processResourceNode(node);
		} else if(node.isLiteral()){
			element = processLiteralNode(node);
		} else {
			throw new UnsupportedOperationException("Can not convert blank node.");
		}
		return element;
	}
	
	/**
	 * Converts the node that is supposed to represent a class in the knowledge base into an NL phrase.
	 * @param node the node
	 * @param plural whether the plural form should be used
	 * @return the NL phrase
	 */
	public NPPhraseSpec processClassNode(Node node, boolean plural) {
		NPPhraseSpec object;
		if (node.equals(OWL.Thing.asNode())) {
			object = nlgFactory.createNounPhrase(GenericType.ENTITY.getNlr());
		} else if (node.equals(RDFS.Literal.asNode())) {
			object = nlgFactory.createNounPhrase(GenericType.VALUE.getNlr());
		} else if (node.equals(RDF.Property.asNode())) {
			object = nlgFactory.createNounPhrase(GenericType.RELATION.getNlr());
		} else if (node.equals(RDF.type.asNode())) {
			object = nlgFactory.createNounPhrase(GenericType.TYPE.getNlr());
		} else {
			String label = uriConverter.convert(node.getURI());
			if (label != null) {
				//get the singular form
				label = PlingStemmer.stem(label);
				//we assume that classes are always used in lower case format
				label = label.toLowerCase();
				object = nlgFactory.createNounPhrase(nlgFactory.createInflectedWord(label, LexicalCategory.NOUN));
			} else {
				object = nlgFactory.createNounPhrase(GenericType.ENTITY.getNlr());
			}

		}
		//set plural form
		object.setPlural(plural);
		return object;
	}
	
	public NPPhraseSpec processVarNode(Node varNode) {
		return nlgFactory.createNounPhrase(nlgFactory.createWord(varNode.toString(), LexicalCategory.NOUN));
	}
	
	public NPPhraseSpec processLiteralNode(Node node) {
		LiteralLabel lit = node.getLiteral();
		// convert the literal
		String literalText = literalConverter.convert(lit);
		NPPhraseSpec np = nlgFactory.createNounPhrase(nlgFactory.createWord(literalText, LexicalCategory.NOUN));
		np.setPlural(literalConverter.isPlural(lit));
		return np;
	}
	
	public NPPhraseSpec processResourceNode(Node node) {
		// get string from URI
		String s = uriConverter.convert(node.getURI());

		// create word
		NLGElement word = nlgFactory.createWord(s, LexicalCategory.NOUN);

		// add gender information if enabled
		if(useGenderInformation) {
			Gender gender = genderDetector.getGender(node.getURI(), s);

			if(gender == Gender.FEMALE) {
				word.setFeature(LexicalFeature.GENDER, simplenlg.features.Gender.FEMININE);
			} else if(gender == Gender.MALE) {
				word.setFeature(LexicalFeature.GENDER, simplenlg.features.Gender.MASCULINE);
			}
		}

		// should be a proper noun, thus, will not be pluralized by morphology
		word.setFeature(LexicalFeature.PROPER, true);

		// wrap in NP
		NPPhraseSpec np = nlgFactory.createNounPhrase(word);
		return np;
	}

	private NLGElement processSubject(Node subject) {
		NLGElement element;
		if (subject.isVariable()) {
			element = processVarNode(subject);
		} else if(subject.isURI()){
			element = processResourceNode(subject);
		} else if(subject.isLiteral()){
			element = processLiteralNode(subject);
		} else {
			throw new UnsupportedOperationException("Can not convert " + subject);
		}
		return element;
	}

	private NPPhraseSpec processObject(Node object, boolean isClass) {
		NPPhraseSpec element;
		if (object.isVariable()) {
			element = processVarNode(object);
		} else if (object.isLiteral()) {
			element = processLiteralNode(object);
		} else if (object.isURI()) {
			if(isClass){
				element = processClassNode(object, false);
			} else {
				element = processResourceNode(object);
			}
		} else {
			throw new IllegalArgumentException("Can not convert blank node " + object + ".");
		}
		return element;
	}

	/**
	 * Takes a URI and returns a noun phrase for it
	 * 
	 * @param uri the URI to convert
	 * @param plural whether it is in plural form
	 * @param isClass if URI is supposed to be a class
	 * @return the noun phrase
	 */
	public NPPhraseSpec getNPPhrase(String uri, boolean plural, boolean isClass) {
		NPPhraseSpec object;
		if (uri.equals(OWL.Thing.getURI())) {
			object = nlgFactory.createNounPhrase(GenericType.ENTITY.getNlr());
		} else if (uri.equals(RDFS.Literal.getURI())) {
			object = nlgFactory.createNounPhrase(GenericType.VALUE.getNlr());
		} else if (uri.equals(RDF.Property.getURI())) {
			object = nlgFactory.createNounPhrase(GenericType.RELATION.getNlr());
		} else if (uri.equals(RDF.type.getURI())) {
			object = nlgFactory.createNounPhrase(GenericType.TYPE.getNlr());
		} else {
			String label = uriConverter.convert(uri);
			if (label != null) {
				if (isClass) {
					//get the singular form
					label = PlingStemmer.stem(label);
					//we assume that classes are always used in lower case format
					label = label.toLowerCase();
				}
				object = nlgFactory.createNounPhrase(nlgFactory.createInflectedWord(label, LexicalCategory.NOUN));
			} else {
				object = nlgFactory.createNounPhrase(GenericType.ENTITY.getNlr());
			}

		}
		object.setPlural(plural);

		return object;
	}

	public static void main(String[] args) throws Exception {
		// http://protege.stanford.edu/plugins/owl/owl-library/koala.owl#Felix http://protege.stanford.edu/plugins/owl/owl-library/koala.owl#isHardWorking ""false""^^http://www.w3.org/2001/XMLSchema#boolean"
		System.out.println(new TripleConverter().convert(
				Triple.create(
						NodeFactory.createURI("http://dbpedia.org/resource/Albert_Einstein"),
						NodeFactory.createURI("http://dbpedia.org/ontology/birthPlace"),
						NodeFactory.createURI("http://dbpedia.org/resource/Ulm")
						)));

		System.out.println(new TripleConverter().convert(
				Triple.create(
						NodeFactory.createURI("http://dbpedia.org/resource/Albert_Einstein"),
						NodeFactory.createURI("http://dbpedia.org/ontology/isHardWorking"),
						NodeFactory.createLiteral("false", XSDDatatype.XSDboolean)
				)));
	}
}
