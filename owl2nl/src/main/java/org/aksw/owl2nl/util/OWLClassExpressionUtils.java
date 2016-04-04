package org.aksw.owl2nl.util;

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;

import java.util.Set;

/**
 * @author Lorenz Buehmann
 */
public class OWLClassExpressionUtils {
	
	public static boolean hasNamedClassOnTopLevel(OWLClassExpression ce) {
		if(!ce.isAnonymous()) {
			return true;
		} else if(ce instanceof OWLObjectIntersectionOf) {
			Set<OWLClassExpression> operands = ((OWLObjectIntersectionOf) ce).getOperands();

			for (OWLClassExpression operand : operands) {
				if(!operand.isAnonymous()) {
					return true;
				}
			}
		}
		return false;
	}
}
