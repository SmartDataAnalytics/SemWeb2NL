/*
 * #%L
 * SPARQL2NL
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
package org.aksw.sparql2nl.queryprocessing;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.vocabulary.RDF;
import org.dllearner.kb.sparql.SparqlEndpoint;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class TypeExtractor2 {
	
	private static final Node TYPE_NODE = RDF.type.asNode();

	private Map<String, Set<String>> var2AssertedTypesMap;
	private Map<String, Set<String>> var2InferredTypesMap;
	
	private DomainExtractor domainExtractor;
	private RangeExtractor rangeExtractor;
	
	private boolean inferTypes = false;
	private boolean preferAssertedTypes = true;
	
	private SparqlEndpoint endpoint;
	
	public TypeExtractor2(SparqlEndpoint endpoint) {
		this.endpoint = endpoint;
	}

       
	public Map<String, Set<String>> extractTypes(Query query) {
		var2AssertedTypesMap = new HashMap<>();
		var2InferredTypesMap = new HashMap<>();
		
		TriplePatternExtractor extr = new TriplePatternExtractor();
		Set<Triple> triples = extr.extractTriplePattern((ElementGroup)query.getQueryPattern());
		
		for(Triple t : triples){
			processTriple(t);
		}
		
		Map<String, Set<String>> var2TypesMap = new HashMap<>();
		if(preferAssertedTypes){
			var2TypesMap.putAll(var2AssertedTypesMap);
			for(Entry<String, Set<String>> entry : var2InferredTypesMap.entrySet()){
				if(!var2AssertedTypesMap.containsKey(entry.getKey())){
					var2TypesMap.put(entry.getKey(), entry.getValue());
				}
			}
		} else {
			var2TypesMap.putAll(var2AssertedTypesMap);
			for(Entry<String, Set<String>> entry : var2InferredTypesMap.entrySet()){
				String key = entry.getKey();
				Set<String> additionalTypes = entry.getValue();
				if(!var2TypesMap.containsKey(key)){
					var2TypesMap.put(key, additionalTypes);
				} else {
					Set<String> existingTypes = var2TypesMap.get(entry.getKey());
					existingTypes.addAll(additionalTypes);
				}
			}
		}
		
		return var2TypesMap;
	}
	
	public void setDomainExtractor(DomainExtractor domainExtractor) {
		this.domainExtractor = domainExtractor;
	}
	
	public void setRangeExtractor(RangeExtractor rangeExtractor) {
		this.rangeExtractor = rangeExtractor;
	}
	
	public void setInferTypes(boolean inferTypes) {
		this.inferTypes = inferTypes;
	}
	
	private void processTriple(Triple triple){
		Node subject = triple.getSubject();
		Node object = triple.getObject();
		
		if (triple.predicateMatches(TYPE_NODE)) {//process rdf:type triples	
			if(subject.isVariable()){
				if(object.isURI()){
					addAssertedType(subject.getName(), object.getURI());
				} 
			}
		} else if(inferTypes && triple.getPredicate().isURI()){//process triples where predicate is not rdf:type, i.e. use rdfs:domain and rdfs:range for inferencing the type
			Node predicate = triple.getPredicate();
			if(subject.isVariable()){
				if(domainExtractor != null){
					String domain = domainExtractor.getDomain(predicate.getURI());
					if(domain != null){
						addInferredType(subject.getName(), domain);
					}
				}
			} 
			if(object.isVariable()){
				if(rangeExtractor != null){
					String range = rangeExtractor.getRange(predicate.getURI());
					if(range != null){
						addInferredType(object.getName(), range);
					}
				}
			}
		} 
	}
	
	private void addAssertedType(String variable, String type){
		Set<String> types = var2AssertedTypesMap.get(variable);
		if(types == null){
			types = new HashSet<>();
			var2AssertedTypesMap.put(variable, types);
		}
		types.add(type);
	}
	
	private void addInferredType(String variable, String type){
		Set<String> types = var2InferredTypesMap.get(variable);
		if(types == null){
			types = new HashSet<>();
			var2InferredTypesMap.put(variable, types);
		}
		types.add(type);
	}
	
		
}
