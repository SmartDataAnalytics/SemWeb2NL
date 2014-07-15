/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.assessment;

import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.Triple;

/**
 *
 * @author ngonga
 */
public interface QuestionGenerator {
    Set<Question> getQuestions(Map<Triple, Double> informativenessMap, int difficulty, int number);
    
}
