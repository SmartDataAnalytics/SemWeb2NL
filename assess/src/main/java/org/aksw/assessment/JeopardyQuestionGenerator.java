/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.assessment;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

import org.aksw.assessment.answer.Answer;
import org.aksw.assessment.answer.SimpleAnswer;
import org.aksw.assessment.question.Question;
import org.aksw.assessment.question.QuestionType;
import org.aksw.assessment.question.SimpleQuestion;
import org.aksw.assessment.rest.RESTService;
import org.aksw.assessment.util.BlackList;
import org.aksw.assessment.util.DBpediaPropertyBlackList;
import org.aksw.assessment.util.DefaultPropertyBlackList;
import org.aksw.assessment.util.PermutationsOfN;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.sparqltools.util.SPARQLQueryUtils;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.RandomGeneratorFactory;
import org.apache.log4j.Logger;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Resource;

import simplenlg.framework.NLGElement;
import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectPropertyImpl;

/**
 * A generator for Jeopardy like questions.
 * @author Axel Ngonga
 */
public class JeopardyQuestionGenerator extends MultipleChoiceQuestionGenerator {

    private static final Logger logger = Logger.getLogger(JeopardyQuestionGenerator.class.getName());
   
    private Map<OWLClass, List<Resource>> wrongAnswersByType = new HashMap<>();
    
	private boolean preferPopularWrongAnswers = true;

	private boolean optimizedAnswerGeneration = true;

	private boolean useCompleteResourcesOnly = true;
	
    public JeopardyQuestionGenerator(QueryExecutionFactory qef, String cacheDirectory, Map<OWLClass, Set<OWLObjectProperty>> restrictions) {
        super(qef, cacheDirectory, restrictions);
        
        verbalizer = new JeopardyVerbalizer(qef, cacheDirectory, wordNetDir);
        verbalizer.setPersonTypes(personTypes);
        verbalizer.setMaxShownValuesPerProperty(maxShownValuesPerProperty);
        verbalizer.setOmitContentInBrackets(true);
    }
    
    /* (non-Javadoc)
     * @see org.aksw.assessment.question.MultipleChoiceQuestionGenerator#getMostProminentResources(java.util.Set)
     */
    @Override
    protected Map<Resource, OWLClass> getMostProminentResources(Set<OWLClass> types) {
    	//INFO for this type of questions it might makes sense to use only resources having all properties of the summary graph
        //as this makes the summary more fancy
    	if(useCompleteResourcesOnly ){
    		Map<Resource, OWLClass> result = Maps.newLinkedHashMap();
    		//we need the summarizing properties graph first
    		for (OWLClass type : types) {
				Set<org.aksw.avatar.clustering.Node> summaryProperties = verbalizer.getSummaryProperties(type, propertyFrequencyThreshold, namespace, cooccurrenceType);
				
				if(summaryProperties != null) {
					StringBuilder query = new StringBuilder();
		        	query.append("SELECT DISTINCT ?x WHERE{");
		        	query.append("?x a <" + type.toStringID() + ">.");
		        	//add triple pattern for each property in summary graph
		        	int i = 0;
					for (org.aksw.avatar.clustering.Node propertyNode : summaryProperties) {
						query.append((propertyNode.outgoing ? ("?x <" + propertyNode.label + "> ?o" + i++) :  ("?o" + i++ + " <" + propertyNode.label + "> ?x")) + ".");
					}
		        	SPARQLQueryUtils.addRankingConstraints(endpointType, query, "x");
		        	query.append("}");
		        	SPARQLQueryUtils.addRankingOrder(endpointType, query, "x");
		            query.append(" LIMIT 500");
		            ResultSet rs = executeSelectQuery(query.toString());
		            QuerySolution qs;
		            while (rs.hasNext()) {
		                qs = rs.next();
		                result.put(qs.getResource("x"), type);
		            }
				}
			}
    		return result;
    	} else {
    		return super.getMostProminentResources(types);
    	}
    }
    
    @Override
    public Set<Question> getQuestions(Map<Triple, Double> informativenessMap, int difficulty, int numberOfQuestions) {
    	 usedWrongAnswers = new HashSet<>();
        Set<Question> questions = new HashSet<>();
        
        // 1. we generate possible resources
        Map<Resource, OWLClass> resources = getMostProminentResources(restrictions.keySet());
        
        //  2. we generate question(s) as long as we have resources or we got the maximum number of questions
        Iterator<Entry<Resource, OWLClass>> iterator = resources.entrySet().iterator();
        
        while(questions.size() < numberOfQuestions && iterator.hasNext()){
        	Entry<Resource, OWLClass> entry = iterator.next();
        	
        	Resource focusEntity = entry.getKey();
        	OWLClass cls = entry.getValue();
        	
            // generate a question
        	Question q = generateQuestion(focusEntity, cls);
            if (q != null) {
                questions.add(q);
            } else {
            	logger.warn("Could not generate question.");
            }
        }
        
        return questions;
    }

    private Question generateQuestion(Resource r, OWLClass type) {
        
        // generate the question in forms of a summary describing the resource
        String summary = getEntitySummary(r.getURI());
        
        if(summary == null){
        	return null;
        }
        
        // get properties of the resource
        String query = "DESCRIBE <" + r.getURI() + ">";
        
        // the correct answer is just the resource itself
        List<Answer> correctAnswers = new ArrayList<>();
        Answer correctAnswer = new SimpleAnswer(getTextualRepresentation(r));
        correctAnswers.add(correctAnswer);
        
        // generate the wrong answers
		List<Answer> wrongAnswers = generateWrongAnswers(r, type);

		NLGElement npPhrase = nlg.getNPPhrase(type.toStringID(), false);
		npPhrase = nlg.realiser.realise(npPhrase);
		String className = npPhrase.getRealisation();
		className = className.toLowerCase().replaceAll(Pattern.quote("."), "");

		return new SimpleQuestion("Which " + className + " matches the following description:\n" + summary,
				correctAnswers, wrongAnswers, DIFFICULTY, QueryFactory.create(query), QuestionType.JEOPARDY);
	}
    
    private List<Answer> generateWrongAnswers(Resource r, OWLClass type){
    	List<Answer> wrongAnswers = new ArrayList<>();
		logger.info("Generating wrong answers...");
		
		//get the triples used in the summary of the resource
		List<Triple> summaryTriples = new ArrayList<>(getSummaryTriples(r.getURI()));
		logger.info("Summary triples:" + summaryTriples);
		
		//build a SPARQL query to get wrong answers that are as similar as possible
		//different strategies are possible here:
		//1. bottom-up: add as many triples(p-o) as possible as long not only the resource itself is returned
		//2. top-down:
		List<Resource> wrongAnswerCandidates = new LinkedList<>();
		Collection<List<Triple>> emptyPermutations = new HashSet<>();
		for(int size = 1; size <= Math.min(3, summaryTriples.size()); size++){
			//compute permutations of size n
			Collection<List<Triple>> permutations = PermutationsOfN.getPermutationsOfSizeN(summaryTriples, size);
			
			//we can handle permutations with the same predicate in a single SPARQL query
			//this kind of query should be handled more efficiently than several queries
			//TODO what can be done for permutations of size > 1?
			if(optimizedAnswerGeneration  && size == 1){
				HashMultimap<Node, Triple> partitions = HashMultimap.create();
				for (List<Triple> permutation : permutations) {
					partitions.put(permutation.get(0).getPredicate(), permutation.get(0));
				}
				
				for (Entry<Node, Collection<Triple>> entry : partitions.asMap().entrySet()) {
					Collection<Triple> triples = entry.getValue();
					
					String query = asFilterInSPARQLQuery(triples, type);
					
					Set<Resource> wrongAnswerCandidatesTmp = new HashSet<Resource>();
					ResultSet rs = executeSelectQuery(query);
					QuerySolution qs;
					while (rs.hasNext()) {
						qs = rs.next();
						if(!qs.getResource("s").equals(r)){
							wrongAnswerCandidatesTmp.add(qs.getResource("s"));
						}
					}
					//we remove the correct resource
					wrongAnswerCandidatesTmp.remove(r);
					
					wrongAnswerCandidates.addAll(wrongAnswerCandidatesTmp);
					
					if(wrongAnswerCandidatesTmp.isEmpty()){
						// we can add a permutation for each triple
						for (Triple triple : triples) {
							emptyPermutations.add(Lists.newArrayList(triple));
						}
					}
				}
			} else {
				//for each permutation
				for (List<Triple> permutation : permutations) {
					
					//if a permutation is a superset of an empty permutation we can of course skip it 
					//because a query is just the intersection of triple patterns, thus, more triples patterns
					//make the resultset smaller
					boolean skip = false;
					for (List<Triple> list : emptyPermutations) {
						if(permutation.containsAll(list)){
							skip = true;
						}
					}
					if(!skip){
						String query = asSPARQLQuery(permutation, type);
						
						//check if it returns some other resources
						boolean empty = true;
						ResultSet rs = executeSelectQuery(query);
						QuerySolution qs;
						while (rs.hasNext()) {
							qs = rs.next();
							if(!qs.getResource("s").equals(r)){
								empty = false;
								wrongAnswerCandidates.add(qs.getResource("s"));
							}
						}
						if(empty){
							emptyPermutations.add(permutation);
						}
					}
				}
			}
		}
		//reverse the candidate list such that we get the most similar first
		Collections.reverse(wrongAnswerCandidates);
		Iterator<Resource> iter = wrongAnswerCandidates.iterator();
		Set<Resource> tmp = new HashSet<>();
		while(iter.hasNext() && tmp.size()<maxNrOfAnswersPerQuestion-1){
			tmp.add(iter.next());
		}
		for (Resource resource : tmp) {
			wrongAnswers.add(new SimpleAnswer(getTextualRepresentation(resource)));
		}
		
		logger.info("...done.");
		return wrongAnswers;
    }
    
    private String asSPARQLQuery(List<Triple> triples, OWLClass type){
    	StringBuilder query = new StringBuilder();
    	query.append("SELECT DISTINCT ?s WHERE{");
    	//we should keep the type as constraint to get more similar wrong answers
    	query.append("?s a <" + type.toStringID() + ">.");
		for (Triple triple : triples) {
			//add triple pattern
			query.append(asTriplePattern("s", triple, type));
		}
		if(preferPopularWrongAnswers){
			SPARQLQueryUtils.addRankingConstraints(endpointType, query, "s");
		}
		query.append("}");
		if(preferPopularWrongAnswers){
	    	SPARQLQueryUtils.addRankingOrder(endpointType, query, "s");
		}
		query.append(" LIMIT " + (maxNrOfAnswersPerQuestion-1));
		return query.toString();
    }
    
    /**
     * Given a list of triples all having the same predicate a SPARQL query pattern using FILTER IN is returned.
     * @param property
     * @param resources
     * @return
     */
    private String asFilterInSPARQLQuery(Collection<Triple> triples, OWLClass type){
    	Iterator<Triple> iterator = triples.iterator();
    	Triple firstTriple = iterator.next();
    	StringBuilder query = new StringBuilder();
    	query.append("SELECT DISTINCT ?s WHERE{");
    	//we should keep the type as constraint to get more similar wrong answers
    	query.append("?s a <" + type.toStringID() + ">.");
    	if(triples.size() == 1){
    		query.append(asTriplePattern("s", firstTriple, type));
    	} else {
    		OWLObjectProperty property = new OWLObjectPropertyImpl(IRI.create(firstTriple.getPredicate().getURI()));
    		boolean outgoingProperty = verbalizer.graphGenerator.isOutgoingProperty(type, property);
    		String subject = outgoingProperty ? "?s" : "?o";
        	String predicate = asTriplePatternComponent(firstTriple.getPredicate());
        	String object = outgoingProperty ? "?o" : "?s";
        	query.append(subject + " " + predicate + " " + object + ".");
    		String filter = "FILTER (?o IN (";
    		filter += asTriplePatternComponent(firstTriple.getObject());
    		while(iterator.hasNext()){
    			filter += "," + asTriplePatternComponent(iterator.next().getObject());
    		}
    		filter += "))";
    		query.append(filter);
    	}
    	if(preferPopularWrongAnswers){
			SPARQLQueryUtils.addRankingConstraints(endpointType, query, "s");
		}
		query.append("}");
		if(preferPopularWrongAnswers){
	    	SPARQLQueryUtils.addRankingOrder(endpointType, query, "s");
		}
		query.append(" LIMIT " + (triples.size() * maxNrOfAnswersPerQuestion));
    	return query.toString();
    }
    
    /**
     * Convert a JENA API triple into a triple pattern string.
     * @param subjectVar
     * @param t
     * @return
     */
    private String asTriplePattern(String subjectVar, Triple t, OWLClass cls){
    	boolean outgoingProperty = verbalizer.graphGenerator.isOutgoingProperty(cls, new OWLObjectPropertyImpl(IRI.create(t.getPredicate().getURI())));
    	//we have to reverse the triple pattern if the property is not an outgoing property
    	String subject = outgoingProperty ? "?" + subjectVar : asTriplePatternComponent(t.getObject());
    	String predicate = asTriplePatternComponent(t.getPredicate());
    	String object = outgoingProperty ? asTriplePatternComponent(t.getObject()) : "?" + subjectVar;
    	return subject + " " + predicate + " " + object + ".";
    }
    
    private String asTriplePatternComponent(Node node){
    	String s;
    	if(node.isURI()){
    		s = "<" + node + ">";
    	} else {
    		s = "\"" + node.getLiteralLexicalForm().replace("\"", "\\\"") + "\"";
    		if(node.getLiteralDatatypeURI() != null){
    			s += "^^<" + node.getLiteralDatatypeURI() + ">";
    		} else if(node.getLiteralLanguage() != null && !node.getLiteralLanguage().isEmpty()){
    			s += "@" + node.getLiteralLanguage();
    		}
    	}
    	return s;
    }
    
    /**
     * Generates wrong answers by just returning instances of the same class as the correct answer.
     * @param r
     * @return
     */
    private List<Answer> generateWrongAnswersSimple(Resource r, OWLClass type){
    	List<Answer> wrongAnswers = new ArrayList<>();
		logger.info("Generating wrong answers...");
		
		List<Resource> wrongAnswerCandidates;
		if(wrongAnswersByType.containsKey(type)){
			wrongAnswerCandidates = wrongAnswersByType.get(type);
		} else {
			wrongAnswerCandidates = new ArrayList<Resource>(getMostProminentResources(restrictions.keySet()).keySet());
			wrongAnswerCandidates.remove(r);
			Collections.shuffle(wrongAnswerCandidates, new Random(123));
			wrongAnswersByType.put(type, wrongAnswerCandidates);
		}
		Iterator<Resource> iter = wrongAnswerCandidates.iterator();
		Resource candidate;
		while(wrongAnswers.size() < maxNrOfAnswersPerQuestion-1 && iter.hasNext()){
			candidate = iter.next();
			wrongAnswers.add(new SimpleAnswer(nlg.realiser.realiseSentence(nlg.getNPPhrase(candidate.getURI(), false,
					false))));
			iter.remove();
		}
		logger.info("...done.");
		return wrongAnswers;
    }

	public static void main(String args[]) throws Exception{
		HierarchicalINIConfiguration config = new HierarchicalINIConfiguration();
		try(InputStream is = RESTService.class.getClassLoader().getResourceAsStream("assess_config_dbpedia.ini")){
			config.load(is);
		}
		RESTService.loadConfig(config);

		RESTService rest = new RESTService();
		List<String> classes = rest.getClasses(null);
		classes = Lists.newArrayList("http://dbpedia.org/ontology/Actor");
		for (String cls : classes) {
			System.out.println("Class:" + cls);
			try {
				Map<OWLClass, Set<OWLObjectProperty>> restrictions = Maps.newHashMap();
				restrictions.put(new OWLClassImpl(IRI.create(cls)), new HashSet<OWLObjectProperty>());
				
				
				SparqlEndpoint endpoint = SparqlEndpoint.create("http://sake.informatik.uni-leipzig.de:8890/sparql", "http://dbpedia.org");
		        endpoint = SparqlEndpoint.getEndpointDBpedia();
		        SparqlEndpointKS ks = new SparqlEndpointKS(endpoint);
		        ks.setCacheDir("/tmp/cache");
		        ks.init();
		        QueryExecutionFactory qef = ks.getQueryExecutionFactory();//RESTService.qef;

				JeopardyQuestionGenerator sqg = new JeopardyQuestionGenerator(qef, "cache", restrictions);
				sqg.setPersonTypes(Sets.newHashSet("http://dbpedia.org/ontology/Person"));
				sqg.setEntityBlackList(new DBpediaPropertyBlackList());
				sqg.setNamespace("http://dbpedia.org/ontology/");
				Set<Question> questions = sqg.getQuestions(null, DIFFICULTY, 10);
				if (questions.size() == 0) {
					System.err.println("EMTPY");
				}
				for (Question q : questions) {
					System.out.println(q.getText());
					for (Answer a : q.getCorrectAnswers()) {
						System.out.println(a.getText());
					}
					for (Answer a : q.getWrongAnswers()) {
						System.out.println(a.getText());
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	    
    }
}
