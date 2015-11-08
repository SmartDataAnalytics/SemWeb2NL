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

import com.google.common.base.Joiner;

/**
 * @author Lorenz Buehmann
 * 
 */
@XmlRootElement(name = "questions")
public class RESTQuestions {

	// @XmlElement(name = "question", type = RESTQuestions.class)
	private List<RESTQuestion> questions;

	public List<RESTQuestion> getQuestions() {
		return questions;
	}

	public void setQuestions(List<RESTQuestion> questions) {
		this.questions = questions;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return Joiner.on("\n##################\n").join(questions);
	}

}
