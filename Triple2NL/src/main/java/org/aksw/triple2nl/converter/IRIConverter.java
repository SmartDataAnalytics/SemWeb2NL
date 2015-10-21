/**
 * 
 */
package org.aksw.triple2nl.converter;

/**
 * Converts IRIs into natural language.
 * @author Lorenz Buehmann
 *
 */
public interface IRIConverter {
	
	/**
	 * Convert the IRI into natural language.
	 * @param iri the IRI
	 * @return a natural language representation
	 */
	String convert(String iri);
	
	/**
	 * Convert the IRI into a natural language.
	 * @param iri the IRI to convert
	 * @param useDereferencing whether to try Linked Data dereferencing of the IRI
	 * @return a natural language representation
	 */
	String convert(String iri, boolean useDereferencing);
}
