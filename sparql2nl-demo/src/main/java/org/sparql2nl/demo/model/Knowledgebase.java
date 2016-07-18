package org.sparql2nl.demo.model;

import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.vaadin.server.Resource;
import org.dllearner.kb.sparql.SparqlEndpoint;

public class Knowledgebase {
	
	private String label;
	private SparqlEndpoint endpoint;
	private Resource icon;
	private String exampleQuery;
	private String description;
	
	public Knowledgebase(SparqlEndpoint endpoint, String label, Resource icon, String exampleQuery, String description) {
		this.label = label;
		this.endpoint = endpoint;
		this.icon = icon;
		this.exampleQuery = exampleQuery;
		this.description = description;
	}
	
	public Knowledgebase(SparqlEndpoint endpoint, String label, Resource icon, String exampleQuery) {
		this(endpoint, label, icon, exampleQuery, null);
	}
	
	public Knowledgebase(SparqlEndpoint endpoint, String label, Resource icon) {
		this(endpoint, label, icon, null);
	}
	
	public Knowledgebase(SparqlEndpoint endpoint, String label) {
		this(endpoint, label, null);
	}

	public String getLabel() {
		return label;
	}

	public SparqlEndpoint getEndpoint() {
		return endpoint;
	}

	public Resource getIcon() {
		return icon;
	}
	
	public String getExampleQuery() {
		return exampleQuery;
	}
	
	public String getDescription() {
		return description;
	}
	
	@Override
	public String toString() {
		return label;
	}
	
	public boolean isOnline(){
		try {
			ResultSet rs = QueryExecutionFactory.create("SELECT * WHERE {?s ?p ?o.} LIMIT 1").execSelect();
			return rs.hasNext();
		} catch (Exception e) {
			return false;
		}
	}
	
	

}
