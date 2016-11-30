/*
 * #%L
 * ASSESS
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
package org.aksw.assessment.rest;

import com.google.common.collect.Lists;
import org.aksw.assessment.question.QuestionType;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

import javax.servlet.ServletContext;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.fail;

/**
 * @author Lorenz Buehmann
 *
 */
public class RESTServiceTest {

private RESTService restService = new RESTService();
	
	public RESTServiceTest() throws Exception {
		HierarchicalINIConfiguration config = new HierarchicalINIConfiguration();
		try(InputStream is = RESTServiceTest.class.getClassLoader().getResourceAsStream("assess_test_config_dbpedia.ini")){
			config.load(is);
		}
		RESTService.loadConfig(config);
	}

	/**
	 * Test method for {@link org.aksw.assessment.rest.RESTService#getQuestionsJSON(ServletContext, String, List, int)}.
	 */
	//@Test
	public void testGetQuestionsJSON() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.aksw.assessment.rest.RESTService#getQuestionsJSON2(javax.servlet.ServletContext, org.codehaus.jettison.json.JSONArray, java.util.List, int)}.
	 * @throws JSONException 
	 */
	@Test
	public void testGetQuestionsJSON2() throws JSONException {
		JSONArray domain = new JSONArray();
		JSONObject entry = new JSONObject();
		entry.put("className", "http://dbpedia.org/ontology/Airport");
		JSONArray properties = new JSONArray();
		properties.put("http://dbpedia.org/ontology/owner");
		entry.put("properties", properties);
		domain.put(entry);
		List<String> questionTypes = Lists.newArrayList(QuestionType.MC.getName(), QuestionType.JEOPARDY.getName());
		RESTQuestions restQuestions = restService.getQuestionsJSON2(null, domain, questionTypes, 3);
		System.out.println(restQuestions);
	}

	/**
	 * Test method for {@link org.aksw.assessment.rest.RESTService#getApplicableProperties(javax.servlet.ServletContext, java.lang.String)}.
	 */
	@Test
	public void testGetApplicableProperties() {
		restService.getApplicableProperties(null, "http://dbpedia.org/ontology/SoccerClub");
	}

	/**
	 * Test method for {@link org.aksw.assessment.rest.RESTService#getClasses(javax.servlet.ServletContext)}.
	 */
	@Test
	public void testGetClasses() {
		List<String> classes = restService.getClasses(null);
		System.out.println(classes.size());
	}

	/**
	 * Test method for {@link org.aksw.assessment.rest.RESTService#getEntities(javax.servlet.ServletContext)}.
	 */
	@Test
	public void testGetEntities() {
		restService.getEntities(null);
	}

	/**
	 * Test method for {@link org.aksw.assessment.rest.RESTService#precomputeGraphs(ServletContext)}.
	 */
	@Test
	public void testPrecomputeGraphs() {
		restService.precomputeGraphs(null);
	}

}
