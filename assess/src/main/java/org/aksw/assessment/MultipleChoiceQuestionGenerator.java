package org.aksw.assessment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.aksw.assessment.answer.Answer;
import org.aksw.assessment.answer.SimpleAnswer;
import org.aksw.assessment.question.Question;
import org.aksw.assessment.question.QuestionType;
import org.aksw.assessment.question.SimpleQuestion;
import org.aksw.assessment.util.DBpediaPropertyBlackList;
import org.aksw.assessment.util.GeneralPropertyBlackList;
import org.aksw.assessment.util.RDFNodeComparator;
import org.aksw.assessment.util.SPARQLQueryUtils;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.RandomGeneratorFactory;
import org.apache.log4j.Logger;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.utilities.OwlApiJenaUtils;
import org.semanticweb.owlapi.model.EntityType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.core.Var;

import simplenlg.features.Feature;
import simplenlg.features.InterrogativeType;
import simplenlg.phrasespec.SPhraseSpec;
import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectPropertyImpl;

/**
 * A generator for multiple choice questions.
 * @author Lorenz Buehmann
 * @author Axel Ngonga
 */
public class MultipleChoiceQuestionGenerator extends AbstractQuestionGenerator {

	private static final Logger logger = Logger.getLogger(MultipleChoiceQuestionGenerator.class.getName());
	
    public MultipleChoiceQuestionGenerator(QueryExecutionFactory qef, String cacheDirectory,
			Map<OWLClass, Set<OWLObjectProperty>> restrictions) {
    	super(qef, cacheDirectory, restrictions);
    }
    
    /* (non-Javadoc)
     * @see org.aksw.assessment.AbstractQuestionGenerator#getQuestionType()
     */
    @Override
    public QuestionType getQuestionType() {
    	return QuestionType.MC;
    }
	
	@Override
    public Set<Question> getQuestions(Map<Triple, Double> informativenessMap, int difficulty, int numberOfQuestions) {
    	 usedWrongAnswers = new HashSet<>();
        Set<Question> questions = new HashSet<>();
        
        RandomGenerator rndGen = RandomGeneratorFactory.createRandomGenerator(new Random(123));
        
        // 1. we generate possible resources
        Map<Resource, OWLClass> resources = getMostProminentResources(restrictions.keySet());
        
        //  2. we generate question(s) as long as we have resources or we got the maximum number of questions
        Iterator<Entry<Resource, OWLClass>> iterator = resources.entrySet().iterator();
        
        while(questions.size() < numberOfQuestions && iterator.hasNext()){
        	Entry<Resource, OWLClass> entry = iterator.next();
        	
        	Resource focusEntity = entry.getKey();
        	OWLClass cls = entry.getValue();
        	
        	// whether target entity is in subject or object position
            boolean inSubjectPosition = true;//rndGen.nextBoolean();
            
            // hide subject or object in question
            boolean hideSubject = false;//rndGen.nextBoolean();
        	
            // generate a question
        	Question q = generateQuestion(focusEntity, cls, inSubjectPosition, hideSubject);
            if (q != null) {
                questions.add(q);
            } else {
            	logger.warn("Could not generate question.");
            }
        }
        
        return questions;
    }
	
	public Question generateQuestion(Resource r, OWLClass type, boolean inSubjectPosition, boolean hideSubject) {
        logger.info("Generating question for resource " + r + "...");
        
        // select the property used in the question
        String property = selectQuestionProperty(r, type, inSubjectPosition);
        
        // generate a focus triple
        Triple focusTriple = generateFocusTriple(r, property, inSubjectPosition);
        
        // generate the question text
        String questionText = generateQuestionText(focusTriple, inSubjectPosition, hideSubject);
        
        // generate correct answers
        Set<RDFNode> correctAnswers = generateCorrectAnswerCandidates(focusTriple, inSubjectPosition, hideSubject);
        
        // if there is no correct answer, we stop
        if(correctAnswers.isEmpty()){
        	logger.warn("Could not find a correct answer.");
        	return null;
        }
        
        // generate wrong answer candidates
        Set<RDFNode> wrongAnswerCandidates = generateWrongAnswerCandidates(focusTriple, inSubjectPosition, hideSubject);
        wrongAnswerCandidates.removeAll(usedWrongAnswers);
        
        // we pick at least 1 and at most N correct answers randomly
        Random rnd = new Random(123);
        List<RDFNode> correctAnswerList = new ArrayList<RDFNode>(correctAnswers);
        Collections.shuffle(correctAnswerList, rnd);
        int maxNumberOfCorrectAnswers = rnd.nextInt((maxNrOfAnswersPerQuestion - 1) + 1) + 1;
        correctAnswerList = correctAnswerList.subList(0, Math.min(correctAnswerList.size(), maxNumberOfCorrectAnswers));
        
        // we pick (N - #correctAnswers) wrong answers randomly
        rnd = new Random(123);
        List<RDFNode> wrongAnswerList = new ArrayList<RDFNode>(wrongAnswerCandidates);
        Collections.shuffle(wrongAnswerList, rnd);
        wrongAnswerList = wrongAnswerList.subList(0, Math.min(wrongAnswerList.size(), maxNrOfAnswersPerQuestion - correctAnswerList.size()));
        usedWrongAnswers.addAll(wrongAnswerList);
        logger.info("...got " + wrongAnswerList);
        
        return new SimpleQuestion(
        		questionText, 
        		generateAnswers(correctAnswerList, generateHints), 
        		generateAnswers(wrongAnswerList, false), 
        		DIFFICULTY, 
        		null, 
        		QuestionType.MC);
    }
    
    private String selectQuestionProperty(Resource r, OWLClass type, boolean subjectPosition) {
    	// generate the property candidates
    	Set<String> propertyCandidates = new HashSet<String>();
    	
    	 // if there are no restrictions on the class we randomly select a property based on the existing instance data
        if(restrictions.get(type).isEmpty()){
			// first of all, we check if there exists any meaningful information about the given resource, 
			// i.e. whether there are interesting triples
			// this is done by getting all properties for the given resource and filter out the black listed ones
            logger.info("Getting property candidates...");
            String query;
            if(subjectPosition) {
            	 query = String.format("select distinct ?p where {<%s> ?p [] . }", r.getURI());
            } else {
            	query = String.format("select distinct ?p where { [] ?p <%s> . }", r.getURI());
            }
            
            ResultSet rs = executeSelectQuery(query);
         
            while (rs.hasNext()) {
            	QuerySolution qs = rs.next();
                String property = qs.getResource("p").getURI();
                // filter by black list
                if (!GeneralPropertyBlackList.getInstance().contains(property)) {
                	propertyCandidates.add(property);
                }
            }
        } else {
        	for (OWLObjectProperty p : restrictions.get(type)) {
				propertyCandidates.add(p.toStringID());
			}
        }
        logger.info("Property candidates: " + propertyCandidates);
        
        // early termination if resource has no meaningful properties
        if (propertyCandidates.isEmpty()) {
            return null;
        }
        
        // random selection
        Random rnd = new Random();
        String property = propertyCandidates.toArray(new String[]{})[rnd.nextInt(propertyCandidates.size())];
        logger.info("Randomly chosen property: " + property);
        
        return property;
    }
    
    private String generateQuestionText(Triple t, boolean inSubjectPosition, boolean hideSubject) {
    	logger.info("Generating question...");
    	
    	// convert to phrase
    	SPhraseSpec p = tripleConverter.convertTriple(t);
    	
    	// decide which interrogative type to use
    	InterrogativeType interrogativeType;
    	if(hideSubject) {
    		interrogativeType = InterrogativeType.WHAT_SUBJECT;
    	} else {
    		interrogativeType = InterrogativeType.WHAT_OBJECT;
    	}
    	
    	// use 'who' instead of which if type is person
    	if(hideSubject) {
    		OWLClassExpression domain = reasoner.getDomain(new OWLObjectPropertyImpl(IRI.create(t.getPredicate().getURI())));
        	if(isPersonType(domain.asOWLClass().toStringID())) {
        		interrogativeType = InterrogativeType.WHO_SUBJECT;
        	} 
    	} else {
    		OWLClassExpression range = reasoner.getRange(new OWLObjectPropertyImpl(IRI.create(t.getPredicate().getURI())));
        	if(isPersonType(range.asOWLClass().toStringID())) {
        		interrogativeType = InterrogativeType.WHO_OBJECT;
        	} else if(isLocationType(range.asOWLClass().toStringID())) {
        		interrogativeType = InterrogativeType.WHERE_OBJECT;
        	}
    	}
		
		p.setFeature(Feature.INTERROGATIVE_TYPE, interrogativeType);
		String question = realiser.realiseSentence(p);
        
        logger.info("Question:" + question);
        return question;
    }
    
    private String generateQuestionText2(Triple t, boolean inSubjectPosition, boolean hideSubject) {
    	logger.info("Generating question...");
    	
    	// convert to phrase
    	SPhraseSpec p = tripleConverter.convertTriple(t);
    	
    	// get the type of the hidden entity
    	OWLNamedIndividual entity;
    	if(hideSubject) {
    		entity = OwlApiJenaUtils.asOWLEntity(t.getSubject(), EntityType.NAMED_INDIVIDUAL);
    	} else {
    		entity = OwlApiJenaUtils.asOWLEntity(t.getObject(), EntityType.NAMED_INDIVIDUAL);
    	}
    	Set<OWLClass> types = reasoner.getMostSpecificTypes(entity);
    	
    	
    	
    	// decide which interrogative type to use
    	InterrogativeType interrogativeType;
    	if(hideSubject) {
    		interrogativeType = InterrogativeType.WHAT_SUBJECT;
    	} else {
    		interrogativeType = InterrogativeType.WHAT_OBJECT;
    	}
    	
    	// use 'who' instead of which if type is person
    	if(hideSubject) {
    		OWLClassExpression range = reasoner.getDomain(new OWLObjectPropertyImpl(IRI.create(t.getPredicate().getURI())));
        	if(isPersonType(range.asOWLClass().toStringID())) {
        		interrogativeType = InterrogativeType.WHO_SUBJECT;
        	}
    	} else {
    		OWLClassExpression domain = reasoner.getRange(new OWLObjectPropertyImpl(IRI.create(t.getPredicate().getURI())));
        	if(isPersonType(domain.asOWLClass().toStringID())) {
        		interrogativeType = InterrogativeType.WHO_OBJECT;
        	}
    	}
		
		p.setFeature(Feature.INTERROGATIVE_TYPE, interrogativeType);
		String question = realiser.realiseSentence(p);
        
        logger.info("Question:" + question);
        return question;
    }
    
	private Triple generateFocusTriple(Resource r, String property, boolean inSubjectPosition) {
		logger.info("Generating focus triple...");
		Set<RDFNode> correctAnswers = new TreeSet<RDFNode>(new RDFNodeComparator());

		Query query;
		if (inSubjectPosition) {
			query = QueryFactory.create(String.format("select distinct ?x where {<%s> <%s> ?x}", r.getURI(), property));
		} else {
			query = QueryFactory.create(String.format("select distinct ?x where {?x <%s> <%s>}", property, r.getURI()));
		}
		query.setLimit(500);

		Var var = Var.alloc("x");
		
		SPARQLQueryUtils.addRanking(endpointType, query, var);

		ResultSet rs = executeSelectQuery(query.toString());

		while (rs.hasNext()) {
			QuerySolution qs = rs.next();
			RDFNode node = qs.get(var.getName());
			if (node.isLiteral()) {
				correctAnswers.add(node.asLiteral());
			} else {
				correctAnswers.add(node.asResource());
			}
			return Triple.create(r.asNode(), NodeFactory.createURI(property), node.asNode());
		}
		logger.info("...got " + correctAnswers);

		return null;
	}
    
    private Set<RDFNode> generateCorrectAnswerCandidates(Triple focusTriple, boolean inSubjectPosition, boolean hideSubject) {
    	 logger.info("Generating correct answers...");
    	 Set<RDFNode> correctAnswers = new TreeSet<RDFNode>(new RDFNodeComparator());
    	 
    	// get values for property, i.e. the correct answers
    	String query;
    	if(hideSubject) {
    		query = String.format("select distinct ?x where {?x <%s> <%s>}", focusTriple.getPredicate().getURI(), focusTriple.getObject().getURI());
    	} else {
    		query = String.format("select distinct ?x where {<%s> <%s> ?x}", focusTriple.getSubject().getURI(), focusTriple.getPredicate().getURI());
    	}
        
        ResultSet rs = executeSelectQuery(query);
        
        while (rs.hasNext()) {
        	QuerySolution qs = rs.next();
            if (qs.get("x").isLiteral()) {
                correctAnswers.add(qs.get("x").asLiteral());
            } else {
                correctAnswers.add(qs.get("x").asResource());
            }
        }
        logger.info("...got " + correctAnswers);
        
        return correctAnswers;
    }
    
    private Set<RDFNode> generateWrongAnswerCandidates(Triple focusTriple, boolean inSubjectPosition, boolean hideSubject) {
        logger.info("Generating wrong answer candidates...");
        Set<RDFNode> wrongAnswers = new TreeSet<RDFNode>(new RDFNodeComparator());
   	 
    	Query query;
    	if(hideSubject) {
    		query = QueryFactory.create(
	    				String.format("select distinct ?x where {?x <%s> ?o . FILTER NOT EXISTS {?x <%s> <%s>}}", 
	    						focusTriple.getPredicate().getURI(), focusTriple.getPredicate().getURI(), focusTriple.getObject().getURI()));
    	} else {
    		query = QueryFactory.create(
    					String.format("select distinct ?x where {?s <%s> ?x . FILTER NOT EXISTS {<%s> <%s> ?x}}", 
    								focusTriple.getPredicate().getURI(), focusTriple.getSubject().getURI(), focusTriple.getPredicate().getURI()));
    	}
    	query.setLimit(1000);
    	
    	// take the most popular entities
    	SPARQLQueryUtils.addRanking(endpointType, query, Var.alloc("x"));
    	
        ResultSet rs = executeSelectQuery(query.toString());
        
        while (rs.hasNext()) {
        	QuerySolution qs = rs.next();
            if (qs.get("x").isLiteral()) {
            	wrongAnswers.add(qs.get("x").asLiteral());
            } else {
            	wrongAnswers.add(qs.get("x").asResource());
            }
        }
		logger.info("...got " + wrongAnswers);
		
		return wrongAnswers;
    }
    
	 protected Map<Resource, OWLClass> getMostProminentResources(Set<OWLClass> types) {
        logger.info("Getting possible resources for types " + types + " ranked by prominence...");
        if (types == null || types.isEmpty()) {
            return null;
        }
        
        Map<Resource, OWLClass> result = Maps.newLinkedHashMap();
        for (OWLClass type : types) {
        	StringBuilder query = new StringBuilder();
        	query.append("SELECT DISTINCT ?x WHERE{");
        	query.append("?x a <" + type.toStringID() + ">.");
        	query.append("?x ?p ?o . VALUES ?p {" + Joiner.on(" ").join(restrictions.get(type)) + "}");
        	org.aksw.sparqltools.util.SPARQLQueryUtils.addRankingConstraints(endpointType, query, "x");
        	query.append("}");
        	org.aksw.sparqltools.util.SPARQLQueryUtils.addRankingOrder(endpointType, query, "x");
            query.append(" LIMIT 500");
            ResultSet rs = executeSelectQuery(query.toString());
            QuerySolution qs;
            while (rs.hasNext()) {
                qs = rs.next();
                result.put(qs.getResource("x"), type);
            }
		}
//        String query = "SELECT distinct ?x (COUNT(?s) AS ?cnt) WHERE {?s ?p ?x. ";
//		for (OWLClass nc : types) {
//			query = query + "{?x a <" + nc.getURI() + ">} UNION ";
//		}
//		query = query.substring(0, query.lastIndexOf("UNION"));
//        query += "} ORDER BY DESC(?cnt) LIMIT 500";
//        Collections.shuffle(result);
        logger.info("...got " + result.size() + " resources, e.g. " + new ArrayList<Resource>(result.keySet()).subList(0, Math.min(10, result.size())));
        return result;
    }
	
    public static void main(String args[]) throws Exception{
    	
        Map<OWLClass, Set<OWLObjectProperty>> restrictions = Maps.newHashMap();
        restrictions.put(
//        		new OWLClassImpl(IRI.create("http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseasome/diseases")),
//        		new HashSet<OWLObjectProperty>()
        		new OWLClassImpl(IRI.create("http://dbpedia.org/ontology/Person")), 
        		Sets.<OWLObjectProperty>newHashSet(
        				new OWLObjectPropertyImpl(IRI.create("http://dbpedia.org/ontology/birthPlace")) 
//        				new OWLObjectPropertyImpl(IRI.create("http://dbpedia.org/ontology/birthDate")),
//        				new OWLObjectPropertyImpl(IRI.create("http://dbpedia.org/ontology/knownFor")),
//        				new OWLObjectPropertyImpl(IRI.create("http://dbpedia.org/ontology/influencedBy"))
        				)
        );
        SparqlEndpoint endpoint = SparqlEndpoint.create("http://sake.informatik.uni-leipzig.de:8890/sparql", "http://dbpedia.org");
        endpoint = SparqlEndpoint.getEndpointDBpedia();
        SparqlEndpointKS ks = new SparqlEndpointKS(endpoint);
        ks.setCacheDir("/tmp/cache");
        ks.init();
        QueryExecutionFactory qef = ks.getQueryExecutionFactory();
//        MultipleChoiceQuestionGenerator sqg = new MultipleChoiceQuestionGenerator(
//        		new SparqlEndpoint(new URL("http://vtentacle.techfak.uni-bielefeld.de:443/sparql"), "http://biomed.de/diseasome"), 
//        		"cache", 
//        		"http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseasome/", 
//        		restrictions,
//        		new HashSet<String>(), null);
        
		MultipleChoiceQuestionGenerator sqg = new MultipleChoiceQuestionGenerator(qef, "cache", restrictions);
		sqg.setPersonTypes(Sets.newHashSet("http://dbpedia.org/ontology/Person"));
		sqg.setLocationTypes(Sets.newHashSet("http://dbpedia.org/ontology/Place"));
		sqg.setEntityBlackList(new DBpediaPropertyBlackList());
		sqg.setNamespace("http://dbpedia.org/ontology/");

		long start = System.currentTimeMillis();
		Set<Question> questions = sqg.getQuestions(null, DIFFICULTY, 5);
		long end = System.currentTimeMillis();
		System.out.println("Operation took " + (end - start) + "ms");
        		
        for (Question q : questions) {
            if (q != null) {
                System.out.println(">>" + q.getText());
                List<Answer> correctAnswers = q.getCorrectAnswers();
                System.out.println(correctAnswers);
                List<Answer> wrongAnswers = q.getWrongAnswers();
                System.out.println(wrongAnswers);
            }
        }
    }
}
