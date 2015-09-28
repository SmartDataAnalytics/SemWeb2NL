package org.aksw.avatar.exceptions;

import org.semanticweb.owlapi.model.OWLClass;

public class NoGraphAvailableException extends Exception {

	public NoGraphAvailableException(OWLClass cls) {
		super("No graph available for class " + cls);
	}

}
