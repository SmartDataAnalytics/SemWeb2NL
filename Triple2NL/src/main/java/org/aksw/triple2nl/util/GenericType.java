package org.aksw.triple2nl.util;

/**
 * Generic types and its textual represnatation.
 * @author Lorenz Buehmann
 *
 */
public enum GenericType {
	ENTITY("entity"),
	VALUE("value"),
	RELATION("property"),
	TYPE("type"),
	UNKNOWN("UltimativeGenericEntity");

	private final String nlr;

	GenericType(String nlr) {
		this.nlr = nlr;
	}

	/**
	 * @return the textual representation
	 */
	public String getNlr() {
		return nlr;
	}
}
