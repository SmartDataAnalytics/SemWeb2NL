package org.sparql2nl.demo.model;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Model;

public class Example {
	
	private String id;
	private QuerySolution data;
	private Model explanation;
	private String explanationNlr;
	private boolean positive;
	
	public Example(String id, QuerySolution data, Model explanation, boolean positive) {
		this.id = id;
		this.data = data;
		this.explanation = explanation;
		this.positive = positive;
	}

	public QuerySolution getData() {
		return data;
	}
	
	public Model getExplanation() {
		return explanation;
	}
	
	public boolean isPositive() {
		return positive;
	}
	
	public void setExplanationNlr(String explanationNlr) {
		this.explanationNlr = explanationNlr;
	}
	
	public String getExplanationNlr() {
		return explanationNlr;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((data == null) ? 0 : id.hashCode());
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
		Example other = (Example) obj;
		if (data == null) {
			if (other.data != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	
	
}
