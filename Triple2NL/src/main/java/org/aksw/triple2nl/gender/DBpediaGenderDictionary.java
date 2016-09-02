package org.aksw.triple2nl.gender;

import com.hp.hpl.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

/**
 * A gender dictionary for DBpedia based on a dataset provided at
 * http://wiki.dbpedia.org/services-resources/documentation/datasets#genders .
 * Example data:
 *
 * <http://dbpedia.org/resource/Algol> <http://xmlns.com/foaf/0.1/gender> "male"@en .
 * <http://dbpedia.org/resource/Abraham> <http://xmlns.com/foaf/0.1/gender> "male"@en .
 *
 * @author Lorenz Buehmann
 */
public class DBpediaGenderDictionary extends GenderDictionary{

	public static String GENDER_FILE_LOCATION = "gender/dbpedia/genders_en.ttl";

	private static final String GENDER_PROPERTY = "http://xmlns.com/foaf/0.1/gender";
	private static final String VALUE_MALE = "male";
	private static final String VALUE_FEMALE = "female";

	public DBpediaGenderDictionary() {
		Model model = ModelFactory.createDefaultModel();

		Literal maleLit = model.createLiteral(VALUE_MALE, "en");
		Literal femaleLit = model.createLiteral(VALUE_FEMALE, "en");

		RDFDataMgr.read(model, getClass().getClassLoader().getResourceAsStream(GENDER_FILE_LOCATION), Lang.TURTLE);
		StmtIterator iter = model.listStatements(null, model.createProperty(GENDER_PROPERTY), (RDFNode) null);
		while(iter.hasNext()) {
			Statement st = iter.next();
			Literal lit = st.getObject().asLiteral();
			if(lit.equals(maleLit)) {
				male.add(st.getSubject().getURI());
			} else if(lit.equals(femaleLit)){
				female.add(st.getSubject().getURI());
			}
		}
	}
}