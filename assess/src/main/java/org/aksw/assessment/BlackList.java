/**
 * 
 */
package org.aksw.assessment;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * @author Lorenz Buehmann
 *
 */
public interface BlackList {
	
	boolean contains(String uri);
	boolean contains(Resource uri);

}
