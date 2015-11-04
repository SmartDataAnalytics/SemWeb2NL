package org.aksw.owl2nl.exception;

import org.semanticweb.owlapi.model.OWLAxiom;

/**
 * An exception which is thrown if the conversion of an OWL axiom into
 * natural language failed.
 * 
 * @author Lorenz Buehmann
 */
public class OWLAxiomConversionException extends Exception {
	
	private final OWLAxiom axiom;

	public OWLAxiomConversionException(OWLAxiom axiom, Exception e) {
		super(e);
		this.axiom = axiom;
	}
	
	/**
	 * @return the OWL axiom for which the conversion failed
	 */
	public OWLAxiom getAxiom() {
		return axiom;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Throwable#toString()
	 */
	@Override
	public String toString() {
		return "The conversion of the axiom " + axiom + " failed.";
	}
}
