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
