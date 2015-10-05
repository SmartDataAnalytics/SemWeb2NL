package org.aksw.assessment;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.aksw.triple2nl.DefaultIRIConverter;
import org.aksw.triple2nl.LiteralConverter;
import org.aksw.triple2nl.TripleConverter;
import org.dllearner.reasoning.SPARQLReasoner;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.RDFNode;

import simplenlg.framework.NLGElement;
import simplenlg.framework.NLGFactory;
import simplenlg.lexicon.Lexicon;
import simplenlg.realiser.english.Realiser;
import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;

/**
 * @author Lorenz Buehmann
 *
 */
public abstract class AbstractQuestionGenerator implements QuestionGenerator {
	
	private static final Logger logger = LoggerFactory.getLogger(AbstractQuestionGenerator.class);
	
	protected static int DIFFICULTY = 1;
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
	
	protected Map<OWLClass, Set<OWLObjectProperty>> restrictions;
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
				Map<OWLClass, Set<OWLObjectProperty>> restrictions) {
		this.qef = qef;
		this.cacheDirectory = cacheDirectory;
		this.restrictions = restrictions;
		
		wordNetDir = "wordnet/" + (SimpleNLGwithPostprocessing.isWindows() ? "windows" : "linux") + "/dict";
        wordNetDir = this.getClass().getClassLoader().getResource(wordNetDir).getPath();
        System.setProperty("wordnet.database.dir", wordNetDir);
        
        // a reasoner on SPARQL
        reasoner = new SPARQLReasoner(qef);
        
        // converter for triples
        tripleConverter = new TripleConverter(qef, cacheDirectory, wordNetDir);
        
        // converter for literals
        literalConverter = new LiteralConverter(new DefaultIRIConverter(qef, cacheDirectory));
      
        // NLG objects
        Lexicon lexicon = Lexicon.getDefaultLexicon();
        nlgFactory = new NLGFactory(lexicon);
        realiser = new Realiser(lexicon);
        nlg = new SimpleNLGwithPostprocessing(qef, cacheDirectory, wordNetDir);
	}
	
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
	 * @return the type of question supported by the generator
	 */
	public abstract QuestionType getQuestionType();
	
	 /**
     * Returns a textual summary for a given individual and its type.
     * @param ind the individual
     * @param type the type of the individual
     * @return a textual summary for a given individual and its type
     */
	protected String getEntitySummary(String entityURI, OWLClass type) {
		logger.info("Generating summary for " + entityURI + " of type " + type + "...");
		
		// create the summary
		OWLNamedIndividualImpl ind = new OWLNamedIndividualImpl(IRI.create(entityURI));
		List<NLGElement> text = verbalizer.verbalize(ind, type, namespace, propertyFrequencyThreshold, cooccurrenceType, hardeningType);
		if (text == null)
			return null;
		String summary = verbalizer.realize(text);
		if (summary == null)
			return null;
		summary = summary.replaceAll("\\s?\\((.*?)\\)", "");
		summary = summary.replace(" , among others,", ", among others,");
		logger.info("...finished generating summary.");
		return summary;
	}

}