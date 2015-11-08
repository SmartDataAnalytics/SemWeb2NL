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
package org.aksw.triple2nl.converter;

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
		// we only split if it contains a vowel
		if(!(s.matches(".*[aeiou].*"))){
			return s;
		}
		
		StringBuilder sb = new StringBuilder();
		for (String token : s.split(" ")) {
			String[] tokenSplit = StringUtils.splitByCharacterTypeCamelCase(token);
			
			String noVowels = "";
			for (String t : tokenSplit) {
				if(t.matches(".*[aeiou].*") || !StringUtils.isAllUpperCase(t)){
					if(!noVowels.isEmpty()){
						sb.append(noVowels).append(" ");
						noVowels = "";
					}
					sb.append(t).append(" ");
				} else {
					noVowels += t;
				}
//				sb = new StringBuilder(sb.toString().trim());
			}
			sb.append(noVowels);
//			sb.append(" ");
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
