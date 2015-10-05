package org.aksw.assessment;

import java.util.Map;
import java.util.Set;

import org.aksw.assessment.question.Question;

import com.hp.hpl.jena.graph.Triple;

/**
 * A generator for questions.
 * @author Axel Ngonga
 */
public interface QuestionGenerator {
    Set<Question> getQuestions(Map<Triple, Double> informativenessMap, int difficulty, int number);
}
