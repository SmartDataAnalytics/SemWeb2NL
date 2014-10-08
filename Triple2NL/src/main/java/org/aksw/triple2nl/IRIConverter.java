/**
 * 
 */
package org.aksw.triple2nl;

/**
 * @author Lorenz Buehmann
 *
 */
public interface IRIConverter {
	String convert(String iri);
	String convert(String iri, boolean dereferenceIRI);
}
