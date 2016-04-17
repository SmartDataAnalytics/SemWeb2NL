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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.assessment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aksw.assessment.answer.Answer;
import org.aksw.assessment.answer.SimpleAnswer;
import org.aksw.assessment.question.Question;
import org.aksw.assessment.question.QuestionType;
import org.aksw.assessment.question.SimpleQuestion;
import org.aksw.assessment.util.DBpediaPropertyBlackList;
import org.aksw.assessment.util.GeneralPropertyBlackList;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.apache.log4j.Logger;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Resource;

import simplenlg.features.Feature;
import simplenlg.features.InterrogativeType;
import simplenlg.phrasespec.SPhraseSpec;
import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectPropertyImpl;

/**
 * A generator for Yes/No questions.
 * @author Axel Ngonga
 */
public class TrueFalseQuestionGenerator extends MultipleChoiceQuestionGenerator {
	
	private static final Logger logger = Logger.getLogger(TrueFalseQuestionGenerator.class.getName());

	public TrueFalseQuestionGenerator(QueryExecutionFactory qef, String cacheDirectory, Map<OWLEntity, Set<OWLObjectProperty>> restrictions) {
		super(qef, cacheDirectory, restrictions);
	}

    /* (non-Javadoc)
	* @see org.aksw.assessment.AbstractQuestionGenerator#getQuestionType()
	*/
    @Override
    public QuestionType getQuestionType() {
        return QuestionType.TRUEFALSE;
    }

    public Question generateQuestion(Resource r, OWLEntity domain, boolean inSubjectPosition, boolean hideSubject) {
        logger.info("Generating question for resource " + r + "...");
        //get properties
        logger.info("Getting statement for resource");
        String query = "select ?p ?o where {<" + r.getURI() + "> ?p ?o. FILTER(isURI(?o))}";
        boolean result = false;
        ResultSet rs = executeSelectQuery(query);
        QuerySolution qs;
        Resource property = null, object = null;
        while (rs.hasNext() && !result) {
            qs = rs.next();
            property = qs.getResource("p");
            object = qs.getResource("o");
            if (!GeneralPropertyBlackList.getInstance().contains(property)){
            	if(blackList != null && !blackList.contains(property)) {
	                if (Math.random() >= 0.5) {
	                    result = true;
	                }
            	}
            }
        }
        logger.info("...got result " + result);
        //early termination if resource has no meaningful properties
        if (!result) {
            return null;
        }

        //pick random property
        logger.info("Chosen (property, object) = (" + property + "," + object + ")");
        query = "ASK {<" + r.getURI() + "> <" + property.getURI() + "> <" + object.getURI() + ">}";
        Query questionQuery = QueryFactory.create(query);
        List<Answer> trueAsAnswer = new ArrayList<>();
        trueAsAnswer.add(new SimpleAnswer("True"));
        List<Answer> falseAsAnswer = new ArrayList<>();
        falseAsAnswer.add(new SimpleAnswer("False"));
        // generate wrong object is answer should be false
        if (Math.random() <= 0.5) {
            //true answer
            Triple t = new Triple(r.asNode(), property.asNode(), object.asNode());
            SPhraseSpec p = tripleConverter.convertToPhrase(t);
            p.setFeature(Feature.INTERROGATIVE_TYPE, InterrogativeType.YES_NO);
            System.err.println(realiser.realiseSentence(p));
            return new SimpleQuestion("Is the following statement correct:\n"+nlg.realiser.realiseSentence(nlg.getNLForTriple(t)), trueAsAnswer, falseAsAnswer, DIFFICULTY, questionQuery, QuestionType.TRUEFALSE);
        } else {
            //get values for property, i.e. the correct answers
            logger.info("Generating wrong answers...");
            query = "select distinct ?o where {?x <" + property.getURI() + "> ?o. FILTER(isURI(?o))}";
            Query sparqlQuery = QueryFactory.create(query);
            rs = executeSelectQuery(query);
            Resource wrongAnswer = null;
            while (rs.hasNext()) {
                qs = rs.next();
                wrongAnswer = qs.get("o").asResource();
                if (!wrongAnswer.getURI().equals(object.getURI())) {
                    break;
                }
            }
            if(wrongAnswer == null) return null;
            Triple t = new Triple(r.asNode(), property.asNode(), wrongAnswer.asNode());
            return new SimpleQuestion("Is the following statement correct:\n"+nlg.realiser.realiseSentence(nlg.getNLForTriple(t)), falseAsAnswer, trueAsAnswer, DIFFICULTY, questionQuery, QuestionType.TRUEFALSE);
        }
    }

	public static void main(String args[]) throws Exception{
		Map<OWLEntity, Set<OWLObjectProperty>> restrictions = Maps.newHashMap();
		restrictions.put(new OWLClassImpl(IRI.create("http://dbpedia.org/ontology/Writer")),
				Sets.<OWLObjectProperty> newHashSet(
						new OWLObjectPropertyImpl(IRI.create("http://dbpedia.org/ontology/birthPlace"))));
		SparqlEndpoint endpoint = SparqlEndpoint.getEndpointDBpedia();
		QueryExecutionFactory qef = new QueryExecutionFactoryHttp(endpoint.getURL().toString(),
				endpoint.getDefaultGraphURIs());
		
		TrueFalseQuestionGenerator sqg = new TrueFalseQuestionGenerator(qef, "cache", restrictions);
		sqg.setPersonTypes(Sets.newHashSet("http://dbpedia.org/ontology/Person"));
		sqg.setEntityBlackList(new DBpediaPropertyBlackList());
		sqg.setNamespace("http://dbpedia.org/ontology/");
		
		Set<Question> questions = sqg.getQuestions(null, 1, 10);
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
