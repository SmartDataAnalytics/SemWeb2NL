/**
 * 
 */
package org.aksw.assessment;

import java.io.File;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * This class contains set of DBpedia properties that are meaningless for the generation of questions
 * in the ASSESS project. 
 * @author Lorenz Buehmann
 *
 */
public class DBpediaPropertyBlackList implements BlackList{

	public static Set<String> blacklist = Sets.newHashSet(
	    "http://dbpedia.org/ontology/wikiPageRedirects", 
	    "http://dbpedia.org/ontology/wikiPageExternalLink",
	    "http://dbpedia.org/ontology/wikiPageDisambiguates",
	    "http://dbpedia.org/ontology/wikiPageRevisionID",
	    "http://dbpedia.org/ontology/wikiPageOutLinkCount",
	    "http://dbpedia.org/ontology/wikiPageInLinkCount",
		"http://dbpedia.org/ontology/wikiPageID",
		"http://dbpedia.org/ontology/wikiPageEditLink",
		"http://dbpedia.org/ontology/wikiPageExtracted",
		"http://dbpedia.org/ontology/wikiPageHistoryLink",
		"http://dbpedia.org/ontology/wikiPageLength",
		"http://dbpedia.org/ontology/wikiPageModified",
		"http://dbpedia.org/ontology/wikiPageOutDegree",
		"http://dbpedia.org/ontology/wikiPageRevisionLink",
	    "http://dbpedia.org/ontology/thumbnail",
	    "http://dbpedia.org/ontology/abstract",
	    "http://dbpedia.org/ontology/termPeriod",
	    "http://dbpedia.org/ontology/individualisedPnd",
	    "http://dbpedia.org/ontology/position",
	    "http://dbpedia.org/property/hasPhotoCollection", 
	    "http://dbpedia.org/property/link",
	    "http://dbpedia.org/property/url",
	    "http://dbpedia.org/property/website",
	    "http://dbpedia.org/property/wordnet_type",
	    "http://dbpedia.org/property/report",
	    "http://dbpedia.org/property/id",
	    "http://dbpedia.org/property/pnd",
	    "http://dbpedia.org/property/signature",
	    "http://dbpedia.org/property/refs",
		"http://www.w3.org/2007/05/powder-s#describedby",
		"http://dbpedia.org/ontology/viafId",
		"http://www.w3.org/2003/01/geo/wgs84_pos#lat",
		"http://www.w3.org/2003/01/geo/wgs84_pos#long",
		"http://www.georss.org/georss/point"
	    );
	
	final static boolean onlyOntologyNamespace = true;
	
	public boolean contains(Resource resource){
		return contains(resource.getURI());
	}
	
	public boolean contains(String uri){
		if(onlyOntologyNamespace && !uri.startsWith("http://dbpedia.org/ontology/")){
			return true;
		}
		return blacklist.contains(uri);
	}
	
	public static void main(String[] args) throws Exception {
		// write blacklist to file
		String content = Joiner.on("\n").join(new TreeSet<>(blacklist));
		
		Files.write(content, new File("src/main/resources/property_blacklist_dbpedia.txt"), Charsets.UTF_8);
	}
}
