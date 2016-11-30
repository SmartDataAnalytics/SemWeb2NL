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
package org.aksw.assessment.question;

import java.util.List;

import org.aksw.assessment.answer.Answer;

import org.apache.jena.query.Query;

/**
 *
 * @author ngonga
 */
public class SimpleQuestion implements Question {
	String text;
	List<Answer> correctAnswers;
	List<Answer> wrongAnswers;
	int difficulty;
	Query query;
	QuestionType type;

	public SimpleQuestion(String text, List<Answer> correctAnswers, List<Answer> wrongAnswers, int difficulty, Query q,
			QuestionType type) {
		this.text = text;
		this.correctAnswers = correctAnswers;
		this.wrongAnswers = wrongAnswers;
		this.difficulty = difficulty;
		this.query = q;
		this.type = type;
	}

	public String getText() {
		return text;
	}

	public QuestionType getType() {
		return type;
	}

	public List<Answer> getCorrectAnswers() {
		return correctAnswers;
	}

	public List<Answer> getWrongAnswers() {
		return wrongAnswers;
	}

	public int getDifficulty() {
		return difficulty;
	}

	public Query getQuery() {
		return query;
	}
	
	@Override
	public String toString() {
		return text;
	}
}
