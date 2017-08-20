package org.aksw.avatar.util;

import com.google.common.collect.Sets;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;

import java.util.Set;

/**
 * @author Lorenz Buehmann
 */
public class DBpediaEntityDataDownloader extends EntityDataDownloader{

    private static final Set<String> expansionClasses = Sets.newHashSet(
            "http://dbpedia.org/ontology/PersonFunction",
            "http://dbpedia.org/ontology/TimePeriod"
            );

    private static final Set<String> ignoredProperties = Sets.newHashSet(
            OWL.sameAs.getURI(),
            RDFS.seeAlso.getURI(),
            "http://dbpedia.org/ontology/wikiPageExternalLink",
            "http://dbpedia.org/ontology/wikiPageID",
            "http://dbpedia.org/ontology/wikiPageRevisionID",
            "http://www.w3.org/ns/prov#wasDerivedFrom");

    public DBpediaEntityDataDownloader(QueryExecutionFactory qef) {
        super(qef, expansionClasses, ignoredProperties);
    }
}
