/**
 * 
 */
package org.aksw.assessment.util;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * A blacklist of entities that are not allowed.
 * 
 * @author Lorenz Buehmann
 *
 */
public interface BlackList {

	/**
	 * Checks whether the given URI is contained in the black list.
	 * @param uri the entity URI
	 * @return <code>TRUE</code> if the entity is contained in the black list, i.e. not allowed, otherwise
	 *         <code>FALSE</code>
	 */
	boolean contains(String uri);

	/**
	 * Checks whether the given resource is contained in the black list.
	 * @param resource the resource
	 * @return <code>TRUE</code> if the resource is contained in the black list, i.e. not allowed, otherwise
	 *         <code>FALSE</code>
	 */
	boolean contains(Resource resource);

}
