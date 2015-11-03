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
import com.hp.hpl.jena.rdf.model.Resource;

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
