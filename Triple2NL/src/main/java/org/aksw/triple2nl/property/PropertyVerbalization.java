/*
 * #%L
 * Triple2NL
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
package org.aksw.triple2nl.property;

import simplenlg.features.Tense;

/**
 * @author Lorenz Buehmann
 *
 */
public class PropertyVerbalization {
	
	private PropertyVerbalizationType verbalizationType;
	private String propertyURI;
	private String propertyText;
	private String expandedVerbalization;
	private String posTags;
	private Tense tense = Tense.PRESENT;
	
	
	public PropertyVerbalization(String propertyURI, String propertyText, PropertyVerbalizationType verbalizationType) {
		this(propertyURI, propertyText, null, verbalizationType);
	}
	
	public PropertyVerbalization(String propertyURI, String propertyText, String posTags, PropertyVerbalizationType verbalizationType) {
		this.propertyURI = propertyURI;
		this.propertyText = propertyText;
		this.posTags = posTags;
		this.verbalizationType = verbalizationType;
		this.expandedVerbalization = propertyText;
	}
	
	/**
	 * @return the property URI
	 */
	public String getProperty() {
		return propertyURI;
	}
	
	/**
	 * @return the propertyText
	 */
	public String getVerbalizationText() {
		return propertyText;
	}
	
	/**
	 * @return the expanded verbalization text
	 */
	public String getExpandedVerbalizationText() {
		return expandedVerbalization;
	}
	
	/**
	 * @return the verbalizationType
	 */
	public PropertyVerbalizationType getVerbalizationType() {
		return verbalizationType;
	}

	public Tense getTense() {
		return tense;
	}

	public void setTense(Tense tense) {
		this.tense = tense;
	}

	/**
	 * @param verbalizationType the verbalizationType to set
	 */
	public void setVerbalizationType(PropertyVerbalizationType verbalizationType) {
		this.verbalizationType = verbalizationType;
	}
	
	/**
	 * @return the POS tags
	 */
	public String getPOSTags() {
		return posTags;
	}
	
	/**
	 * Set the expanded verbalization text.
	 * @param expandedVerbalization the expanded verbalization text
	 */
	public void setExpandedVerbalizationText(String expandedVerbalization) {
		this.expandedVerbalization = expandedVerbalization;
	}
	
	public boolean isNounType(){
		return verbalizationType == PropertyVerbalizationType.NOUN;
	}
	
	public boolean isVerbType(){
		return verbalizationType == PropertyVerbalizationType.VERB;
	}
	
	public boolean isUnspecifiedType(){
		return !(isVerbType() || isNounType());
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "URI:" + propertyURI + 
				"\nText:" + propertyText + 
				"\nExpanded Text:" + expandedVerbalization + 
				"\nType: " + verbalizationType;
	}

}
