/**
 * 
 */
package org.aksw.assessment.util;

import com.hp.hpl.jena.rdf.model.Resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class contains set of DBpedia properties that are meaningless for the generation of questions
 * in the ASSESS project. The list of properties is contained in the file
 * <code>src/main/resources/property_blacklist_dbpedia.txt</code>.
 * @author Lorenz Buehmann
 *
 */
public class DBpediaPropertyBlackList implements BlackList {

	private static final String FILE_NAME = "property_blacklist_dbpedia.txt";

	private final Set<String> blacklist;
	
	public static boolean onlyOntologyNamespace = true;

	public DBpediaPropertyBlackList() throws IOException {
		Stream<String> lines = Files.lines(Paths.get(getClass().getClassLoader().getResource(FILE_NAME).getPath()));
		blacklist = lines.collect(Collectors.toSet());
	}

	@Override
	public boolean contains(Resource resource){
		return contains(resource.getURI());
	}

	@Override
	public boolean contains(String uri) {
		return onlyOntologyNamespace && !uri.startsWith("http://dbpedia.org/ontology/") || blacklist.contains(uri);
	}
}
