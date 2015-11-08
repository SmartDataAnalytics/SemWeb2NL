/*
 * #%L
 * OWL2NL
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
