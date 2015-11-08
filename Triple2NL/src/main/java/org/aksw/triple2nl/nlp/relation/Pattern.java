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
package org.aksw.triple2nl.nlp.relation;

import java.util.HashMap;
import java.util.Map;

/**
 * Only used inside this class to encapsulate the Solr query results.
 */
public class Pattern {
    
    public Map<String,Double> features = new HashMap<>();
    public String naturalLanguageRepresentationWithoutVariables = "";
    public String naturalLanguageRepresentation = "";
    public Double boaScore = 0D;
    public Double naturalLanguageScore = 0D;
    public String posTags = "";
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
	@Override
	public String toString() {
		return "Pattern [features=" + features + ", naturalLanguageRepresentation=" + naturalLanguageRepresentation
				+ ", boaScore=" + boaScore + ", naturalLanguageScore=" + naturalLanguageScore + ", POS=" + posTags
				+ "]";
	}
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {

        final int prime = 31;
        int result = 1;
        result = prime * result + ((naturalLanguageRepresentation == null) ? 0 : naturalLanguageRepresentation.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {

        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Pattern other = (Pattern) obj;
        if (naturalLanguageRepresentation == null) {
            if (other.naturalLanguageRepresentation != null)
                return false;
        }
        else
            if (!naturalLanguageRepresentation.equals(other.naturalLanguageRepresentation))
                return false;
        return true;
    }
}