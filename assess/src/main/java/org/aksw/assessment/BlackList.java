/**
 * 
 */
package org.aksw.assessment;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * A blacklist of entities that are not allowed.
 * 
 * @author Lorenz Buehmann
 *
 */
public interface BlackList {

	/**
	 * 
	 * @param uri the entity URI
	 * @return <code>true</code> if the entity is not allowed, otherwise
	 *         <code>FALSE</code>
	 */
	boolean contains(String uri);

	boolean contains(Resource uri);

}
