/*
 * #%L
 * AVATAR
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
package org.aksw.avatar;

import java.util.Set;

import com.google.common.base.Joiner;

/**
 * This class represents a entity summarization model for a given knowledge base, i.e. it contains
 * summarization templates for each class in the knowledge base, if exists.
 * @author Lorenz Buehmann
 *
 */
public class EntitySummarizationModel {

	private Set<EntitySummarizationTemplate> templates;

	public EntitySummarizationModel(Set<EntitySummarizationTemplate> templates) {
		this.templates = templates;
	}
	
	/**
	 * @return the templates
	 */
	public Set<EntitySummarizationTemplate> getTemplates() {
		return templates;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return Joiner.on("\n").join(templates);
	}

}
