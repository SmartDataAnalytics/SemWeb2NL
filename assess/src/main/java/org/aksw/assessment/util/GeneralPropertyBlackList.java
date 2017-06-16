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

import java.util.Set;

import com.google.common.collect.Sets;
import org.apache.jena.rdf.model.Resource;

/**
 * This class contains basically a set of defined properties that are meaningless for the generation of questions
 * in the ASSESS project. 
 * @author Lorenz Buehmann
 *
 */
public class GeneralPropertyBlackList implements BlackList{

	public static Set<String> blacklist = Sets.newHashSet(
		"http://www.w3.org/ns/prov#was", 
	    "http://www.w3.org/2002/07/owl#sameAs", 
	    "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", 
	    "http://www.w3.org/2000/01/rdf-schema#label",
	    "http://www.w3.org/2000/01/rdf-schema#comment",
	    "http://www.w3.org/ns/prov#wasDerivedFrom", 
	    "http://xmlns.com/foaf/0.1/isPrimaryTopicOf", 
	    "http://xmlns.com/foaf/0.1/depiction", 
	    "http://xmlns.com/foaf/0.1/homepage", 
	    "http://purl.org/dc/terms/subject",
	    "http://xmlns.com/foaf/0.1/givenName",
	    "http://xmlns.com/foaf/0.1/name",
	    "http://xmlns.com/foaf/0.1/surname"
	    );
	
	private static final BlackList instance = new GeneralPropertyBlackList();
	
	private GeneralPropertyBlackList(){}
	
	public static BlackList getInstance(){
		return instance;
	}
	
	public boolean contains(Resource resource){
		return blacklist.contains(resource.getURI());
	}
	
	public boolean contains(String uri){
		return blacklist.contains(uri);
	}
}
