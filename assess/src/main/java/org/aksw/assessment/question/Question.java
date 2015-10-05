/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.assessment.question;

import java.util.List;

import org.aksw.assessment.answer.Answer;

import com.hp.hpl.jena.query.Query;

/**
 *
 * @author ngonga
 */
public interface Question {
    String getText();
    List<Answer> getCorrectAnswers();
    List<Answer> getWrongAnswers();    
    int getDifficulty();
    Query getQuery();
    QuestionType getType();
}
