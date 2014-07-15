package org.aksw.triple2nl;

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

	public String getNlr() {
		return nlr;
	}
}
