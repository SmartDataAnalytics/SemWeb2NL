/*
 * #%L
 * Triple2NL
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
package org.aksw.triple2nl.util;

import java.util.Comparator;

import com.google.common.collect.ComparisonChain;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.util.NodeComparator;

/**
 * Comparator to sort a list of triples by subject, predicate, and object to
 * ensure a consistent order for human-readable output
 * 
 * @author Lorenz Buehmann
 *
 */
public class TripleComparator implements Comparator<Triple>{
	
	private final NodeComparator nodeComparator = new NodeComparator();

	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(Triple t1, Triple t2) {
		return ComparisonChain.start()
		.compare(t1.getSubject(), t2.getSubject(), nodeComparator)
		.compare(t1.getPredicate(), t2.getPredicate(), nodeComparator)
		.compare(t1.getObject(), t2.getObject(), nodeComparator)
		.result();
	}

}
