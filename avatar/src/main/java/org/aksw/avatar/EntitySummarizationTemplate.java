/**
 * 
 */
package org.aksw.avatar;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLProperty;


/**
 * This class basically represents a summarization of a class by containing a set of properties which
 * are most frequently used in a specific knowledge base.
 * @author Lorenz Buehmann
 *
 */
public class EntitySummarizationTemplate {

	private OWLClass cls;
	private Set<OWLProperty> properties;
	
	public EntitySummarizationTemplate(OWLClass cls, Set<OWLProperty> properties) {
		this.cls = cls;
		this.properties = properties;
	}
	
	/**
	 * @return the class
	 */
	public OWLClass getTemplateClass() {
		return cls;
	}
	
	/**
	 * @return the properties
	 */
	public Set<OWLProperty> getProperties() {
		return properties;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Class: " + cls + "\nProperties: " + properties;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((cls == null) ? 0 : cls.hashCode());
		result = prime * result + ((properties == null) ? 0 : properties.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EntitySummarizationTemplate other = (EntitySummarizationTemplate) obj;
		if (cls == null) {
			if (other.cls != null)
				return false;
		} else if (!cls.equals(other.cls))
			return false;
		if (properties == null) {
			if (other.properties != null)
				return false;
		} else if (!properties.equals(other.properties))
			return false;
		return true;
	}
	

}
