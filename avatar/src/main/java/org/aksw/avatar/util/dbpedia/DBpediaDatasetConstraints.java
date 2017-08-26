package org.aksw.avatar.util.dbpedia;

import com.google.common.collect.Sets;
import org.aksw.avatar.util.DatasetConstraints;

/**
 * A utility class for the DBpedia dataset that contains some base restrictions on the schema entites:
 *
 *
 * @author Lorenz Buehmann
 */
public class DBpediaDatasetConstraints extends DatasetConstraints {

	public static String DBO  = "http://dbpedia.org/ontology/";
	public static String DBP  = "http://dbpedia.org/property/";
	public static String DBR  = "http://dbpedia.org/resource/";
	public static String DBCAT  = "http://dbpedia.org/resource/Category:";


	private static DBpediaDatasetConstraints instance;

	public static DBpediaDatasetConstraints getInstance() {
		return instance == null ? instance = new DBpediaDatasetConstraints() : instance;
	}

	private DBpediaDatasetConstraints(){
		setAllowedNamespaces(Sets.newHashSet(DBO));

		setIgnoredProperties(new DBpediaPropertyBlackList().getProperties());

	}
}
