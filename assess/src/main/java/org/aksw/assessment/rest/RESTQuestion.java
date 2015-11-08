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
/**
 * 
 */
package org.aksw.assessment.rest;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Lorenz Buehmann
 *
 */
@XmlRootElement
public class RESTQuestion {

	private String question;
	private String questionType;
	private List<RESTAnswer> correctAnswers;
	private List<RESTAnswer> wrongAnswers;
	
	public String getQuestion() {
		return question;
	}

	public void setQuestion(String question) {
		this.question = question;
	}

	public List<RESTAnswer> getCorrectAnswers() {
		return correctAnswers;
	}

	public void setCorrectAnswers(List<RESTAnswer> correctAnswers) {
		this.correctAnswers = correctAnswers;
	}

	public List<RESTAnswer> getWrongAnswers() {
		return wrongAnswers;
	}

	public void setWrongAnswers(List<RESTAnswer> wrongAnswers) {
		this.wrongAnswers = wrongAnswers;
	}

	public String getQuestionType() {
		return questionType;
	}

	public void setQuestionType(String questionType) {
		this.questionType = questionType;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Question: " + question + "\nCorrect answers: " + correctAnswers + "\nWrong answers: " + wrongAnswers;
	}
}
