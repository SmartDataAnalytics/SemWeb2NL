package org.aksw.assessment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

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
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.utilities.OwlApiJenaUtils;
import org.semanticweb.owlapi.model.EntityType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.sparql.util.FmtUtils;
import com.hp.hpl.jena.vocabulary.RDF;

import simplenlg.features.Feature;
import simplenlg.features.InternalFeature;
import simplenlg.features.InterrogativeType;
import simplenlg.framework.LexicalCategory;
import simplenlg.framework.NLGElement;
import simplenlg.phrasespec.NPPhraseSpec;
import simplenlg.phrasespec.PPPhraseSpec;
import simplenlg.phrasespec.SPhraseSpec;
import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectPropertyImpl;

/**
 * A generator for multiple choice questions.
 * @author Lorenz Buehmann
 * @author Axel Ngonga
 */
public class MultipleChoiceQuestionGenerator extends AbstractQuestionGenerator {

	private static final Logger logger = LoggerFactory.getLogger(MultipleChoiceQuestionGenerator.class);
	private RandomGenerator rndGen;
	
    public MultipleChoiceQuestionGenerator(QueryExecutionFactory qef, String cacheDirectory,
			Map<OWLEntity, Set<OWLObjectProperty>> restrictions) {
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
        DIFFICULTY = difficulty;
        
        rndGen = RandomGeneratorFactory.createRandomGenerator(new Random(123));
        
        // 1. we generate possible resources
        Map<Resource, OWLEntity> resources = getMostProminentResources(restrictions.keySet());
        
        // 2. we generate question(s) as long as we have resources or we got the maximum number of questions
        Iterator<Entry<Resource, OWLEntity>> iterator = resources.entrySet().iterator();
        
        while(questions.size() < numberOfQuestions && iterator.hasNext()){
        	Entry<Resource, OWLEntity> entry = iterator.next();
        	
        	Resource focusEntity = entry.getKey();
        	OWLEntity domain = entry.getValue();
        	
        	// whether target entity is in subject or object position
            boolean inSubjectPosition = rndGen.nextBoolean();
            
            // hide subject or object in question
            boolean hideSubject = domain.isOWLNamedIndividual() ? false : rndGen.nextBoolean();
        	
            try {
				// generate a question
				Question q = generateQuestion(focusEntity, domain, inSubjectPosition, hideSubject);
				if (q != null) {
				    questions.add(q);
				} else {
					logger.warn("Could not generate question.");
				}
			} catch (Exception e) {
				logger.error("Question generation for resource " + focusEntity + " failed.", e);
			}
        }
        
        return questions;
    }
	
	private Set<Question> generateQuestionsForEntity(Resource entity, OWLClass type, int nrOfQuestions) {
		Set<Question> questions = new HashSet<>();

		while (questions.size() < nrOfQuestions) {

			// whether target entity is in subject or object position
			boolean inSubjectPosition = rndGen.nextBoolean();
			
			// get a random property
			String property = selectQuestionProperty(entity, type, inSubjectPosition);

			// hide subject or object in question
			boolean hideSubject = rndGen.nextBoolean();

			try {

				// generate a question
				Question q = generateQuestion(entity, type, inSubjectPosition, hideSubject);
				if (q != null) {
					questions.add(q);
				} else {
					logger.warn("Could not generate question.");
				}
			} catch (Exception e) {
				logger.error("Question generation for resource " + entity
						+ " failed.", e);
			}
		}

		return questions;
	}
	
	public Question generateQuestion(Resource r, OWLEntity domain, boolean inSubjectPosition, boolean hideSubject) {
        logger.info("Generating question for resource " + r + "...");
        
        // select the property used in the question
        String property = selectQuestionProperty(r, domain, inSubjectPosition);
        
        // generate a focus triple
        Triple focusTriple = generateFocusTriple(r, property, inSubjectPosition);
        
        if(focusTriple == null) { // TODO this should be avoided beforehand
        	return null;
        }
        // generate the BGP
        
        // generate the question text
        String questionText = generateQuestionText2(focusTriple, inSubjectPosition, hideSubject);
        
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
        
        return new SimpleQuestion(
        		questionText, 
        		generateAnswers(correctAnswerList, generateHints), 
        		generateAnswers(wrongAnswerList, false), 
        		DIFFICULTY, 
        		null, 
        		QuestionType.MC);
    }
    
    private String selectQuestionProperty(Resource r, OWLEntity domain, boolean subjectPosition) {
    	// generate the property candidates
    	Set<String> propertyCandidates = new HashSet<String>();
    	
		// first of all, we check if there exists any meaningful information about the given resource, 
		// i.e. whether there are interesting triples
		// this is done by getting all properties for the given resource and filter out the black listed ones
		logger.info("Getting property candidates...");
		String query;
		if (subjectPosition) {
			query = String.format("select distinct ?p where {<%s> ?p [] . }", r.getURI());
		} else {
			query = String.format("select distinct ?p where { [] ?p <%s> . }", r.getURI());
		}
		logger.debug(query);

		ResultSet rs = executeSelectQuery(query);

		while (rs.hasNext()) {
			QuerySolution qs = rs.next();
			String property = qs.getResource("p").getURI();
			// filter by black list
			if (!GeneralPropertyBlackList.getInstance().contains(property)) {
				propertyCandidates.add(property);
			}
		}
    	            
    	 // apply the restrictions
		if(domain != null) {
			Set<String> tmp = new HashSet<>();
	    	for (OWLObjectProperty p : restrictions.get(domain)) {
				tmp.add(p.toStringID());
			}
	    	propertyCandidates.retainAll(tmp);
		}
		
        logger.info("Property candidates: " + propertyCandidates);
        
        // early termination if resource has no meaningful properties
        if (propertyCandidates.isEmpty()) {
            return null;
        }
        
        // random selection
        String property = propertyCandidates.toArray(new String[propertyCandidates.size()])[rndGen.nextInt(propertyCandidates.size())];
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
    	
    	String question;
    	
    	// convert to phrase
    	SPhraseSpec p = tripleConverter.convertTriple(t);
    	System.err.println(realiser.realise(p));
    	
    	// get the type of the hidden entity
    	OWLObjectProperty focusPredicate = OwlApiJenaUtils.asOWLEntity(t.getPredicate(), EntityType.OBJECT_PROPERTY);
    	OWLClass type;
    	if(hideSubject) {
    		type = reasoner.getDomain(focusPredicate).asOWLClass();
    	} else {
    		type = reasoner.getRange(focusPredicate).asOWLClass();
    	}
    	
    	boolean useOfWhich = rndGen.nextBoolean();
    	
    	if(useOfWhich && hideSubject) {
    		NLGElement subjectElt = p.getSubject();
    		
    		SPhraseSpec questionPhrase;
        	
        	if(subjectElt.hasFeature(InternalFeature.SPECIFIER)) {// NNP's NP 'be' 'NP'
        		subjectElt.removeFeature(InternalFeature.SPECIFIER);
        		
        		//
        		boolean functional = reasoner.isFunctional(focusPredicate);
        		subjectElt.setFeature(InternalFeature.SPECIFIER, functional ? "the" : "a");
        		
        		NPPhraseSpec npPhrase = nlg.getNPPhrase(type.toStringID(), false);
        		npPhrase.setSpecifier(nlgFactory.createWord("which", LexicalCategory.PRONOUN));
        		PPPhraseSpec pp = nlgFactory.createPrepositionPhrase("of");
        		pp.setComplement(npPhrase);
        		
        		subjectElt.setFeature(InternalFeature.COMPLEMENTS, Lists.newArrayList(pp));
        		
        		questionPhrase = nlgFactory.createClause(subjectElt, p.getVerbPhrase(), p.getObject());
        		
        	} else {// NNP VP NP
        		NPPhraseSpec subject = nlg.getNPPhrase(type.toStringID(), false);
        		subject.setSpecifier(nlgFactory.createWord("which", LexicalCategory.PRONOUN));
        		
        		questionPhrase = nlgFactory.createClause(subject, p.getVerbPhrase(), p.getObject());
        	}

    		p = questionPhrase;
    	} else {
    		// decide which interrogative type to use
        	InterrogativeType interrogativeType;
        	if(hideSubject) {
        		interrogativeType = InterrogativeType.WHAT_SUBJECT;
        	} else {
        		interrogativeType = InterrogativeType.WHAT_OBJECT;
        	}
        	
        	// use 'who' instead of which if type is person
        	if(hideSubject) {
        		OWLClassExpression range = reasoner.getDomain(focusPredicate);
            	if(isPersonType(range.asOWLClass().toStringID())) {
            		interrogativeType = InterrogativeType.WHO_SUBJECT;
            	}
        	} else {
        		OWLClassExpression range = reasoner.getRange(focusPredicate);
            	if(isPersonType(range.asOWLClass().toStringID())) {
            		interrogativeType = InterrogativeType.WHO_OBJECT;
            	} else if(isLocationType(range.asOWLClass().toStringID())) {
            		interrogativeType = InterrogativeType.WHERE_OBJECT;
            	}
        	}
    		
    		p.setFeature(Feature.INTERROGATIVE_TYPE, interrogativeType);
    	}
    	
    	// 
    	if(DIFFICULTY == 2 && hideSubject) {
    		Node object = t.getObject();
    		if(object.isURI()) {
    			// we need the type of the object
    			OWLClass objectType = reasoner.getRange(focusPredicate).asOWLClass();
    			
    			// we pick a fact about the object
    			// TODO which fact to select?
    			String query = String.format("SELECT ?p ?o WHERE {%s ?p ?o .} LIMIT 1000", 
    					FmtUtils.stringForNode(object));
    			ResultSet rs = executeSelectQuery(query);
    			while(rs.hasNext()) {
    				QuerySolution qs = rs.next();
    				
    				String property = qs.getResource("p").getURI();
    				// filter by black list
    				if (!GeneralPropertyBlackList.getInstance().contains(property)) {
    					Node predicate = qs.get("p").asNode();
    					if(!predicate.equals(RDF.type.asNode()) && (namespace == null || predicate.getURI().startsWith(namespace))) {
    						Triple t2 = Triple.create(object, qs.get("p").asNode(), qs.get("o").asNode());
        					
        					NPPhraseSpec objectNP = nlg.getNPPhrase(objectType.toStringID(), false);
            				objectNP.setDeterminer("a");
            				
            				SPhraseSpec triplePhrase = tripleConverter.convertTriple(t2);
            				NLGElement subjectElt = triplePhrase.getSubject();
            				
            				SPhraseSpec complementClause;
            				if(subjectElt.hasFeature(InternalFeature.SPECIFIER)) {// NNP's NP 'be' 'NP'
            	        		subjectElt.removeFeature(InternalFeature.SPECIFIER);
            	        		
//            	        		boolean functional = reasoner.isFunctional(focusPredicate);
//            	        		subjectElt.setFeature(InternalFeature.SPECIFIER, functional ? "the" : "a");
//            	        		
//            	        		NPPhraseSpec npPhrase = nlg.getNPPhrase(type.toStringID(), false);
//            	        		npPhrase.setSpecifier(nlgFactory.createWord("which", LexicalCategory.PRONOUN));
//            	        		PPPhraseSpec pp = nlgFactory.createPrepositionPhrase("of which");
////            	        		pp.setComplement(npPhrase);
//            	        		
//            	        		subjectElt.setFeature(InternalFeature.COMPLEMENTS, Lists.newArrayList(pp));
////            	        		subjectElt.setFeature(Feature.COMPLEMENTISER, "");
            	        		
            	        		complementClause = nlgFactory.createClause(subjectElt, triplePhrase.getVerbPhrase(), triplePhrase.getObject());
            	        		complementClause.setFeature(Feature.COMPLEMENTISER, "whose");
            				} else {
            					complementClause = nlgFactory.createClause(null, triplePhrase.getVerbPhrase(), triplePhrase.getObject());
            				}
            				
            				objectNP.setComplement(complementClause);
            				p.setObject(objectNP);
    					}
    				}
    			}
    		}
    	}
    	
    	// realise the phrase
    	question = realiser.realiseSentence(p);
        
        logger.info("Question:" + question);
        return question;
    }
    
	private Triple generateFocusTriple(Resource r, String property, boolean inSubjectPosition) {
		logger.info("Generating focus triple...");
		List<RDFNode> correctAnswers = new ArrayList<RDFNode>();

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
			if (!node.isAnon()) {
				correctAnswers.add(node);
			}
		}

		if(correctAnswers.isEmpty()) {
			logger.warn("got no focus triple. query:\n" + query);
			return null;
		}

		// random selection
		Collections.sort(correctAnswers, new RDFNodeComparator());
		RDFNode node = correctAnswers.get(rndGen.nextInt());
		return Triple.create(r.asNode(), NodeFactory.createURI(property), node.asNode());
	}
	
	private ElementGroup generateFocusBGP(Resource r, String property, boolean inSubjectPosition, boolean hideSubject, int difficulty) {
		ElementGroup eg = new ElementGroup();
		
		Triple focusTriple = generateFocusTriple(r, property, inSubjectPosition);
		eg.addTriplePattern(focusTriple);
		
		if(difficulty == 2) {
			Node node;
			String query = "SELECT ?p ?o WHERE {%s ?p ?o .} LIMIT 1000";
			if(inSubjectPosition) { // get fact about the object
				node = focusTriple.getObject();
			} else { // get fact about the subject
				node = focusTriple.getSubject();
			}
			query = String.format(query, FmtUtils.stringForNode(node));
			
			ResultSet rs = executeSelectQuery(query);
			while(rs.hasNext()) {
				QuerySolution qs = rs.next();
				
				String p = qs.getResource("p").getURI();
				// filter by black list
				if (!GeneralPropertyBlackList.getInstance().contains(p)) {
					Node predicate = qs.get("p").asNode();
					if(!predicate.equals(RDF.type.asNode()) && (namespace == null || predicate.getURI().startsWith(namespace))) {
						Triple t2 = Triple.create(node, qs.get("p").asNode(), qs.get("o").asNode());
						eg.addTriplePattern(t2);
					}
				}
			}
		}
		
		return eg;
	}
    
    private Set<RDFNode> generateCorrectAnswerCandidates(Triple focusTriple, boolean inSubjectPosition, boolean hideSubject) {
    	 logger.info("Generating correct answers...");
    	 Set<RDFNode> correctAnswers = new TreeSet<RDFNode>(new RDFNodeComparator());
    	 
    	// get values for property, i.e. the correct answers
    	String query;
    	if(hideSubject) {
    		query = String.format("select distinct ?x where {?x <%s> %s}", 
    				focusTriple.getPredicate().getURI(), 
    				FmtUtils.stringForNode(focusTriple.getObject()));
    	} else {
    		query = String.format("select distinct ?x where {<%s> <%s> ?x}", focusTriple.getSubject().getURI(), focusTriple.getPredicate().getURI());
    	}
        query = "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#> " + query;
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
	    				String.format("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#> select distinct ?x where {?x <%s> ?o . FILTER NOT EXISTS {?x <%s> %s}}", 
	    						focusTriple.getPredicate().getURI(), focusTriple.getPredicate().getURI(), FmtUtils.stringForNode(focusTriple.getObject())));
    	} else {
    		query = QueryFactory.create(
    					String.format("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#> select distinct ?x where {?s <%s> ?x . FILTER NOT EXISTS {<%s> <%s> ?x}}", 
    								focusTriple.getPredicate().getURI(), focusTriple.getSubject().getURI(), focusTriple.getPredicate().getURI()));
    	}
    	query.setLimit(1000);
    	System.out.println(query);
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
    
	 protected Map<Resource, OWLEntity> getMostProminentResources(Set<OWLEntity> domain) {
        logger.info("Getting possible resources for domain " + domain + " ranked by prominence...");
        
        if (domain == null || domain.isEmpty()) {
            return null;
        }
        
        Map<Resource, OWLEntity> result = Maps.newLinkedHashMap();
        
        for (OWLEntity entity : domain) {
        	
        	if(entity.isOWLClass()) { // entity is class, i.e. lookup for instances of this class
        		Set<OWLObjectProperty> properties = restrictions.get(entity);
        		
        		String query;
        		if(properties.isEmpty()) {
        			query = String.format("SELECT DISTINCT ?x WHERE {?x a <%s> . }", entity.toStringID());
        		} else {
        			query = String.format("SELECT DISTINCT ?x WHERE {?x a <%s> . ?x ?p ?o . VALUES ?p {%s} }",
        					entity.toStringID(), Joiner.on(" ").join(properties));
        		}
        		
        		Query q = QueryFactory.create(query);
            	SPARQLQueryUtils.addRanking(endpointType, q, Var.alloc("x"));
            	q.setLimit(500);
            	
                ResultSet rs = executeSelectQuery(q.toString());
                QuerySolution qs;
                while (rs.hasNext()) {
                    qs = rs.next();
                    result.put(qs.getResource("x"), entity.asOWLClass());
                }
        	} else if(entity.isOWLNamedIndividual()) { // entity itself is an instance, i.e. just use it
        		result.put(ResourceFactory.createResource(entity.toStringID()), null);
        	}
		}
        
//        Collections.shuffle(result);
        logger.info("...got " + result.size() + " resources, e.g. " + new ArrayList<Resource>(result.keySet()).subList(0, Math.min(10, result.size())));
        return result;
    }
	
    public static void main(String args[]) throws Exception{
    	
        Map<OWLEntity, Set<OWLObjectProperty>> restrictions = Maps.newHashMap();
//        restrictions.put(
//        		new OWLNamedIndividualImpl(IRI.create("http://dbpedia.org/resource/London")),
//        		new HashSet<OWLObjectProperty>());
        
        restrictions.put(
        		new OWLClassImpl(IRI.create("http://dbpedia.org/ontology/Person")), 
        		Sets.<OWLObjectProperty>newHashSet(
//        				new OWLObjectPropertyImpl(IRI.create("http://dbpedia.org/ontology/birthPlace"))
        				new OWLObjectPropertyImpl(IRI.create("http://dbpedia.org/ontology/birthDate"))
//        				,new OWLObjectPropertyImpl(IRI.create("http://dbpedia.org/ontology/knownFor"))
//        				,new OWLObjectPropertyImpl(IRI.create("http://dbpedia.org/ontology/influencedBy"))
//        				,new OWLObjectPropertyImpl(IRI.create("http://dbpedia.org/ontology/influenced"))
//        				,new OWLObjectPropertyImpl(IRI.create("http://dbpedia.org/ontology/award"))
//        				,new OWLObjectPropertyImpl(IRI.create("http://dbpedia.org/ontology/college"))
//        				,new OWLObjectPropertyImpl(IRI.create("http://dbpedia.org/ontology/killedBy"))
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
		Set<Question> questions = sqg.getQuestions(null, 2, 10);
		long end = System.currentTimeMillis();
		System.out.println("Operation took " + (end - start) + "ms");
        		
        for (Question q : questions) {
            if (q != null) {
                System.out.println(q.getText());
//                List<Answer> correctAnswers = q.getCorrectAnswers();
//                System.out.println(correctAnswers);
//                List<Answer> wrongAnswers = q.getWrongAnswers();
//                System.out.println(wrongAnswers);
            }
        }
    }
}
