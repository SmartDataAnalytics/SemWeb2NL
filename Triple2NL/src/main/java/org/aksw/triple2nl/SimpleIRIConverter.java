/**
 * 
 */
package org.aksw.triple2nl;

import org.apache.commons.lang3.StringUtils;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.util.IRIShortFormProvider;
import org.semanticweb.owlapi.util.SimpleIRIShortFormProvider;

/**
 * @author Lorenz Buehmann
 *
 */
public class SimpleIRIConverter implements IRIConverter {
	
	private IRIShortFormProvider sfp = new SimpleIRIShortFormProvider();
	
	private boolean splitCamelCase = true;
	private boolean replaceUnderScores = true;
	private boolean toLowerCase = false;
	private boolean omitContentInBrackets = true;

	/* (non-Javadoc)
	 * @see org.aksw.triple2nl.IRIConverter#convert(java.lang.String)
	 */
	@Override
	public String convert(String iri) {
		// get short form
		String shortForm = sfp.getShortForm(IRI.create(iri));
		
		// normalize
		shortForm = normalize(shortForm);
		
		return shortForm;
	}
	
	/* (non-Javadoc)
	 * @see org.aksw.triple2nl.IRIConverter#convert(java.lang.String, boolean)
	 */
	@Override
	public String convert(String iri, boolean dereferenceIRI) {
		return convert(iri);
	}
	
	 private String splitCamelCase(String s) {
	    	StringBuilder sb = new StringBuilder();
	    	for (String token : s.split(" ")) {
				sb.append(StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(token), ' ')).append(" ");
			}
	    	return sb.toString().trim();
//	    	return s.replaceAll(
//	    	      String.format("%s|%s|%s",
//	    	         "(?<=[A-Z])(?=[A-Z][a-z])",
//	    	         "(?<=[^A-Z])(?=[A-Z])",
//	    	         "(?<=[A-Za-z])(?=[^A-Za-z])"
//	    	      ),
//	    	      " "
//	    	   );
	    	}
	
	private String normalize(String s){
		if(replaceUnderScores){
			s = s.replace("_", " ");
		}
        if(splitCamelCase){
        	s = splitCamelCase(s);
        }
        if(toLowerCase){
        	s = s.toLowerCase();
        }
        if(omitContentInBrackets){
        	s = s.replaceAll("\\(.+?\\)", "").trim();
        }
        return s;
	}
}
