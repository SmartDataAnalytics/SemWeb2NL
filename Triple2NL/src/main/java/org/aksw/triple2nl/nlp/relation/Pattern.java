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