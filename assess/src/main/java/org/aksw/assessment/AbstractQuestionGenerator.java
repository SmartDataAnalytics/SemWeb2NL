/*
 * #%L
 * ASSESS
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
package org.aksw.assessment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aksw.assessment.answer.Answer;
import org.aksw.assessment.answer.SimpleAnswer;
import org.aksw.assessment.question.QuestionType;
import org.aksw.assessment.util.BlackList;
import org.aksw.assessment.util.DefaultPropertyBlackList;
import org.aksw.avatar.Verbalizer;
import org.aksw.avatar.clustering.hardening.HardeningFactory;
import org.aksw.avatar.clustering.hardening.HardeningFactory.HardeningType;
import org.aksw.avatar.dataset.DatasetBasedGraphGenerator.Cooccurrence;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.sparql2nl.naturallanguagegeneration.SimpleNLGwithPostprocessing;
import org.aksw.sparqltools.util.SPARQLEndpointType;
import org.aksw.triple2nl.TripleConverter;
import org.aksw.triple2nl.converter.DefaultIRIConverter;
import org.aksw.triple2nl.converter.LiteralConverter;
import org.dllearner.reasoning.SPARQLReasoner;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.jena.graph.Triple;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.dictionary.Dictionary;
import simplenlg.framework.NLGElement;
import simplenlg.framework.NLGFactory;
import simplenlg.lexicon.Lexicon;
import simplenlg.realiser.english.Realiser;
import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;

/**
 * @author Lorenz Buehmann
 *
 */
public abstract class AbstractQuestionGenerator implements QuestionGenerator {
	
	private static final Logger logger = LoggerFactory.getLogger(AbstractQuestionGenerator.class);
	
	protected int DIFFICULTY = 1;
	protected SimpleNLGwithPostprocessing nlg;
	protected LiteralConverter literalConverter;
	protected int maxNrOfAnswersPerQuestion = 5;
	
	protected String wordNetDir;
	protected String cacheDirectory;
	protected QueryExecutionFactory qef;
	protected SPARQLEndpointType endpointType = SPARQLEndpointType.Virtuoso;
	protected SPARQLReasoner reasoner;
	
	protected Verbalizer verbalizer;
	protected final int maxShownValuesPerProperty = 3;
	protected double propertyFrequencyThreshold = 0.2;
	protected Cooccurrence cooccurrenceType = Cooccurrence.PROPERTIES;
	protected HardeningType hardeningType = HardeningFactory.HardeningType.SMALLEST;
	
	
	protected Map<OWLEntity, Set<OWLObjectProperty>> restrictions;
	protected Set<RDFNode> usedWrongAnswers;
	
	protected TripleConverter tripleConverter;
	
	protected NLGFactory nlgFactory;
	protected Realiser realiser;
	
	protected BlackList blackList = new DefaultPropertyBlackList();
	
	protected Set<String> personTypes = new HashSet<>();
	
	protected Set<String> locationTypes = new HashSet<>();
	
	protected String namespace;
	
	protected boolean generateHints = false;
	
	public AbstractQuestionGenerator(QueryExecutionFactory qef, String cacheDirectory,
				Map<OWLEntity, Set<OWLObjectProperty>> restrictions) {
		this.qef = qef;
		this.cacheDirectory = cacheDirectory;
		this.restrictions = restrictions;
		
		wordNetDir = "wordnet/" + (SimpleNLGwithPostprocessing.isWindows() ? "windows" : "linux") + "/dict";
        wordNetDir = this.getClass().getClassLoader().getResource(wordNetDir).getPath();
        System.setProperty("wordnet.database.dir", wordNetDir);
        
        Dictionary dictionary;
		try {
			dictionary = Dictionary.getDefaultResourceInstance();
		} catch (JWNLException e) {
			throw new RuntimeException("Failed to create WordNet instance.", e);
		}
        
        // a reasoner on SPARQL
        reasoner = new SPARQLReasoner(qef);
        
        // converter for triples
        tripleConverter = new TripleConverter(qef, cacheDirectory, dictionary);
        
        // converter for literals
        literalConverter = new LiteralConverter(new DefaultIRIConverter(qef, cacheDirectory));
      
        // NLG objects
        Lexicon lexicon = Lexicon.getDefaultLexicon();
        nlgFactory = new NLGFactory(lexicon);
        realiser = new Realiser(lexicon);
        nlg = new SimpleNLGwithPostprocessing(qef, cacheDirectory, dictionary);
	}
	
	/**
	 * @return the type of question supported by the generator
	 */
	public abstract QuestionType getQuestionType();
	
	/**
	 * @param personTypes a set of classes that contain persons
	 */
	public void setPersonTypes(Set<String> personTypes) {
		this.personTypes = personTypes;
	}
	
	/**
	 * @param locationTypes a set of classes that contain locations
	 */
	public void setLocationTypes(Set<String> locationTypes) {
		this.locationTypes = locationTypes;
	}
	
	/**
	 * @param generateHints whether to generate hints in forms if short
	 *            descriptions for the correct answers
	 */
	public void setGenerateHints(boolean generateHints) {
		this.generateHints = generateHints;
	}
	
	/**
	 * @param blackList a list of entities that must not be used in the
	 *            questions
	 */
	public void setEntityBlackList(BlackList blackList) {
		this.blackList = blackList;
	}
	
	/**
	 * @param maxNrOfAnswersPerQuestion the maximum number of answers per questions if
	 * allowed by the question type
	 */
	public void setMaxNrOfAnswersPerQuestion(int maxNrOfAnswersPerQuestion) {
		this.maxNrOfAnswersPerQuestion = maxNrOfAnswersPerQuestion;
	}
	
	/**
	 * @param namespace the namespace to set
	 */
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}
	
	/**
	 * @param restrictions the restrictions to set
	 */
	public void setRestrictions(Map<OWLEntity, Set<OWLObjectProperty>> restrictions) {
		this.restrictions = restrictions;
	}
	
	/**
	 * Whether the given class denotes persons.
	 * 
	 * @param cls the class to check
	 * @return <code>true</code> if it's a person type, otherwise
	 *         <code>false</code>
	 */
	protected boolean isPersonType(String cls) {
		for (String personType : personTypes) {
			if (cls.equals(personType)) {
				return true;
			} else if (reasoner.isSuperClassOf(
					new OWLClassImpl(IRI.create(personType)),
					new OWLClassImpl(IRI.create(cls)))) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Whether the given class denotes locations.
	 * 
	 * @param cls the class to check
	 * @return <code>true</code> if it's a location type, otherwise
	 *         <code>false</code>
	 */
	protected boolean isLocationType(String cls) {
		for (String locationType : locationTypes) {
			if (cls.equals(locationType)) {
				return true;
			} else if (reasoner.isSuperClassOf(
					new OWLClassImpl(IRI.create(locationType)),
					new OWLClassImpl(IRI.create(cls)))) {
				return true;
			}
		}
		return false;
	}
	
	protected ResultSet executeSelectQuery(String query) {
		logger.debug("Sending query \n" + query);
        QueryExecution qe = qef.createQueryExecution(query);
        ResultSet rs = null;
        try {
            rs = qe.execSelect();
        } catch (Exception e) {
            logger.error("Error when executing query\n" + query, e);
        }
        return rs;
    }
	
	/**
	 * Returns the triples used for summarization of <code>entity</code> 
	 * @param entity the entity to summarize
	 * @return a set of triples
	 */
	protected Collection<Triple> getSummaryTriples(String entity) {
		return verbalizer.getSummaryTriples(new OWLNamedIndividualImpl(IRI.create(entity)));
	}
	
	/**
	 * Returns a textual summary for a given individual and its type.
	 * 
	 * @param entityURI the individual
	 * @param type the type of the individual
	 * @return a textual summary for a given individual and its type
	 */
	protected String getEntitySummary(String entityURI, OWLClass type) {
		logger.info("Generating summary for " + entityURI + " of type " + type + "...");

		// create the summary
		OWLNamedIndividualImpl ind = new OWLNamedIndividualImpl(IRI.create(entityURI));
		List<NLGElement> text = verbalizer.verbalize(ind, type, namespace, propertyFrequencyThreshold, cooccurrenceType,
				hardeningType);
		if (text == null) {
			return null;
		}
		String summary = verbalizer.realize(text);
		if (summary == null) {
			return null;
		}
		summary = summary.replaceAll("\\s?\\((.*?)\\)", "");
		summary = summary.replace(" , among others,", ", among others,");
		logger.info("...finished generating summary.");
		return summary;
	}
	
	 /**
     * Returns a textual summary for a given individual.
     * @param entityURI
     * @return
     */
	protected String getEntitySummary(String entityURI) {
		//get the most specific type(s) of the individual
		Set<OWLClass> mostSpecificTypes = getMostSpecificTypes(entityURI);
		
		// pick the most prominent type
		if (mostSpecificTypes.iterator().hasNext()) {
			// return the summary
			OWLClass mostSpecificType = mostSpecificTypes.iterator().next();
			return getEntitySummary(entityURI, mostSpecificType);
		} else {
			return "No hint available";
		}
	}
	
	protected List<Answer> generateAnswers(Collection<RDFNode> resources, boolean addHint) {
		List<Answer> answers = new ArrayList<>();

		for (RDFNode node : resources) {
			// get a textual representation of the resource
			String textualRepresentation = getTextualRepresentation(node);

			// if answer is a resource, additionally generate a summary which can be used as hint 
			String hint = null;
			if (addHint && node.isURIResource()) {
				hint = generateHint(node.asResource());
			}
			Answer answer = new SimpleAnswer(textualRepresentation, hint);
			answers.add(answer);
		}
		return answers;
	}
	
	protected String generateHint(Resource r) {
		String hint = getEntitySummary(r.getURI());
		return hint;
	}

	/**
	 * Generate textual representation of RDFNode object.
	 * 
	 * @param node
	 * @return
	 */
	protected String getTextualRepresentation(RDFNode node) {
		String s;
		if (node.isURIResource()) {
			s = realiser.realise(nlg.getNPPhrase(node.asResource().getURI(), false, false)).getRealisation();
		} else if (node.isLiteral()) {
			s = literalConverter.convert(node.asLiteral());
		} else {
			throw new IllegalArgumentException("Conversion of blank node " + node + " not supported yet!");
		}
		return s;
	}
	
	protected Set<OWLClass> getMostSpecificTypes(String uri) {
		Set<OWLClass> types = reasoner.getMostSpecificTypes(new OWLNamedIndividualImpl(IRI.create(uri)));
		for (Iterator<OWLClass> iter = types.iterator(); iter.hasNext();) {
			OWLClass cls = iter.next();
			if (namespace != null && !cls.toStringID().startsWith(namespace)) {
				iter.remove();
			}
		}
		return types;
	}

}