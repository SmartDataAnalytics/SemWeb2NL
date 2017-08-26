package org.aksw.avatar.util;

import com.google.common.collect.Sets;

import java.util.HashSet;
import java.util.Set;

/**
 * Comprises a set of constraints regarding the schema entities when working on an RDF dataset.
 *
 * @author Lorenz Buehmann
 */
public class DatasetConstraints {

	private Set<String> allowedNamespaces = new HashSet<>();
	private Set<String> ignoredNamespaces = new HashSet<>();

	private Set<String> allowedClasses = new HashSet<>();
	private Set<String> ignoredClasses = new HashSet<>();

	private Set<String> allowedProperties = new HashSet<>();
	private Set<String> ignoredProperties = new HashSet<>();


	public void setAllowedNamespaces(Set<String> allowedNamespaces) {
		if(!Sets.intersection(allowedNamespaces, ignoredNamespaces).isEmpty()) {
			throw new RuntimeException("Ignored and allowed namespaces overlap!");
		}
		this.allowedNamespaces = allowedNamespaces;
	}

	public Set<String> getAllowedNamespaces() {
		return allowedNamespaces;
	}

	public void setIgnoredNamespaces(Set<String> ignoredNamespaces) {
		if(!Sets.intersection(allowedNamespaces, ignoredNamespaces).isEmpty()) {
			throw new RuntimeException("Ignored and allowed namespaces overlap!");
		}
		this.ignoredNamespaces = ignoredNamespaces;
	}

	public Set<String> getIgnoredNamespaces() {
		return ignoredNamespaces;
	}

	public void setAllowedClasses(Set<String> allowedClasses) {
		if(!Sets.intersection(allowedClasses, ignoredClasses).isEmpty()) {
			throw new RuntimeException("Ignored and allowed classes overlap!");
		}
		this.allowedClasses = allowedClasses;
	}

	public Set<String> getAllowedClasses() {
		return allowedClasses;
	}

	public void setIgnoredClasses(Set<String> ignoredClasses) {
		if(!Sets.intersection(allowedClasses, ignoredClasses).isEmpty()) {
			throw new RuntimeException("Ignored and allowed classes overlap!");
		}
		this.ignoredClasses = ignoredClasses;
	}

	public Set<String> getIgnoredClasses() {
		return ignoredClasses;
	}

	public void setAllowedProperties(Set<String> allowedProperties) {
		if(!Sets.intersection(allowedProperties, ignoredProperties).isEmpty()) {
			throw new RuntimeException("Ignored and allowed properties overlap!");
		}
		this.allowedProperties = allowedProperties;
	}

	public Set<String> getAllowedProperties() {
		return allowedProperties;
	}

	public void setIgnoredProperties(Set<String> ignoredProperties) {
		if(!Sets.intersection(allowedProperties, ignoredProperties).isEmpty()) {
			throw new RuntimeException("Ignored and allowed properties overlap!");
		}
		this.ignoredProperties = ignoredProperties;
	}

	public Set<String> getIgnoredProperties() {
		return ignoredProperties;
	}

	public boolean isClassAllowed(String cls) {
		return (allowedClasses.isEmpty() && !ignoredClasses.contains(cls)) ||
						allowedClasses.contains(cls);
	}

	public boolean isPropertyAllowed(String property) {
		return (allowedProperties.isEmpty() && !ignoredProperties.contains(property)) ||
				allowedProperties.contains(property);

	}
}
