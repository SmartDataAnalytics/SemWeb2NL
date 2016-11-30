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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import org.apache.jena.rdf.model.Resource;

/**
 * This class contains set of properties that are meaningless for the generation of questions
 * in the ASSESS project. 
 * @author Lorenz Buehmann
 *
 */
public class DefaultPropertyBlackList implements BlackList{

	private final Set<String> blacklist;
	
	public DefaultPropertyBlackList() {
		this(Collections.EMPTY_SET);
	}
	
	public DefaultPropertyBlackList(File file) throws IOException {
		this(Files.readLines(file, Charsets.UTF_8));
	}
	
	public DefaultPropertyBlackList(Collection<String> blacklist) {
		this.blacklist = Sets.newHashSet(blacklist);
	}
	
	public boolean contains(Resource resource){
		return contains(resource.getURI());
	}
	
	public boolean contains(String uri){
		return blacklist.contains(uri);
	}
}
