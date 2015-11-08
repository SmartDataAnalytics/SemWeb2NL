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

/**
 * @author Lorenz Buehmann
 *
 */
public enum PredicateAsNounConversionType {
	
	/**
	 * Predicate is combined with possessive form of subject.
	 */
	POSSESSIVE,
	/**
	 * Relative clause is bound by relative pronoun which.
	 */
	RELATIVE_CLAUSE_PRONOUN,
	/**
	 * Relative clause is bound by complementizer that.
	 */
	RELATIVE_CLAUSE_COMPLEMENTIZER,
	/**
	 * Relative clause that is not marked by an explicit relative pronoun or complementizer such as who, which or that.
	 */
	REDUCED_RELATIVE_CLAUSE,
	
	

}
