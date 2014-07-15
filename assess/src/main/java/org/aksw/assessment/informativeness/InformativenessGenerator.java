/**
 * 
 */
package org.aksw.assessment.informativeness;

import com.hp.hpl.jena.graph.Triple;

/**
 * @author Lorenz Buehmann
 *
 */
public interface InformativenessGenerator {

	double computeInformativeness(Triple triple);
}
