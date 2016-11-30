/*
 * #%L
 * AVATAR
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
package org.aksw.avatar;

import java.io.File;

import org.aksw.avatar.clustering.WeightedGraph;
import org.aksw.avatar.dataset.CachedDatasetBasedGraphGenerator;
import org.aksw.avatar.dataset.DatasetBasedGraphGenerator;
import org.aksw.avatar.exceptions.NoGraphAvailableException;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.semanticweb.owlapi.model.OWLClass;

import org.apache.jena.rdf.model.Resource;

/**
 * @author Lorenz Buehmann
 *
 */
public class EntitySummarizationGenerator {
	
	private File cacheDirectory = new File("cache");
	private DatasetBasedGraphGenerator graphGenerator;
	
	private double propertyFrequencyThreshold;

	public EntitySummarizationGenerator(SparqlEndpoint endpoint, File cacheDirectory, double propertyFrequencyThreshold) {
		this.cacheDirectory  = cacheDirectory;
		this.propertyFrequencyThreshold = propertyFrequencyThreshold;
		
		graphGenerator = new CachedDatasetBasedGraphGenerator(endpoint, cacheDirectory);
	}
	
	public EntitySummarizationGenerator(QueryExecutionFactory qef, File cacheDirectory) {
		this.cacheDirectory  = cacheDirectory;
		graphGenerator = new CachedDatasetBasedGraphGenerator(qef, cacheDirectory);
	}
	
	public EntitySummarization generateEntitySummarization(Resource entity){
		//determine the most specific class of the entity
		OWLClass cls = null;
		
		return generateEntitySummarization(entity, cls);
	}
	
	public EntitySummarization generateEntitySummarization(Resource entity, OWLClass cls){
		//generate a graph with the most interesting properties
		try {
			WeightedGraph graph = graphGenerator.generateGraph(cls, propertyFrequencyThreshold);
		} catch (NoGraphAvailableException e) {
			e.printStackTrace();
		}
		
		return null;
	}

}
