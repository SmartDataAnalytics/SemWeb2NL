package org.sparql2nl.demo;

import com.vaadin.server.Resource;
import com.vaadin.server.ThemeResource;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sparql2nl.demo.model.Knowledgebase;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class KnowledgebaseLoader {

	private static final Logger LOGGER = LoggerFactory.getLogger(KnowledgebaseLoader.class);
	
	public static List<Knowledgebase> loadDatasets(String path){
		LOGGER.info("loading knowledge bases from {}", path);
		List<Knowledgebase> datasets = new ArrayList<Knowledgebase>();
		HierarchicalConfiguration.setDefaultListDelimiter('\0');
		try {
			XMLConfiguration config = new XMLConfiguration(new File(path));
			
			List datasetConfigurations = config.configurationsAt("knowledgebase");
			for(Iterator iter = datasetConfigurations.iterator();iter.hasNext();){
				HierarchicalConfiguration datasetConf = (HierarchicalConfiguration) iter.next();
				datasets.add(createKnowledgebase(datasetConf));
			}
		} catch (ConfigurationException e) {
			LOGGER.error("loading knowledge bases failed", e);
		}
		
		return datasets;
	}
	
	private static Knowledgebase createKnowledgebase(HierarchicalConfiguration conf){
		HierarchicalConfiguration endpointConf = conf.configurationAt("endpoint");
		SparqlEndpoint endpoint = createEndpoint(endpointConf);
		String label = conf.getString("name");
		Resource icon = null;
		if(conf.getString("icon") != null){
			icon = new ThemeResource(conf.getString("icon"));
		}
		String exampleQuery = conf.getString("exampleQuery");
		String description = conf.getString("description");
		
		return new Knowledgebase(endpoint, label, icon, exampleQuery, description);
	}
	
	private static SparqlEndpoint createEndpoint(HierarchicalConfiguration conf){
		try {
			URL url = new URL(conf.getString("url"));
			String defaultGraphURI = conf.getString("defaultGraphURI");
			
			return new SparqlEndpoint(url, Collections.singletonList(defaultGraphURI), Collections.<String>emptyList());
		} catch (MalformedURLException e) {
			LOGGER.error("Could not parse URL from SPARQL endpoint.", e);
		}
		return null;
	}
	
	
	public static void main(String[] args) {
		KnowledgebaseLoader.loadDatasets(KnowledgebaseLoader.class.getClassLoader().getResource("knowledgebases.xml").getPath());
	}

}
