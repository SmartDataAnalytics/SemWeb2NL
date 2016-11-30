/*
 * #%L
 * ASSESS
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
/**
 * 
 */
package org.aksw.assessment.util;

import org.apache.jena.rdf.model.Resource;

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
