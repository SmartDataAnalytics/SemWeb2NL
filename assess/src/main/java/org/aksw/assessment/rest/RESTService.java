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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.aksw.assessment.*;
import org.aksw.assessment.answer.Answer;
import org.aksw.assessment.question.Question;
import org.aksw.assessment.question.QuestionType;
import org.aksw.assessment.util.BlackList;
import org.aksw.assessment.util.DefaultPropertyBlackList;
import org.aksw.avatar.clustering.WeightedGraph;
import org.aksw.avatar.dataset.CachedDatasetBasedGraphGenerator;
import org.aksw.avatar.dataset.DatasetBasedGraphGenerator;
import org.aksw.avatar.dataset.DatasetBasedGraphGenerator.Cooccurrence;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.dllearner.kb.LocalModelBasedSparqlEndpointKS;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.reasoning.SPARQLReasoner;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectPropertyImpl;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;

//import org.apache.log4j.Logger;

/**
 * @author Lorenz Buehmann
 *
 */
@Path("/rest")
public class RESTService extends Application{
	
	private static final Logger logger = LoggerFactory.getLogger(RESTService.class);
	
	static SparqlEndpointKS ks;
	static String namespace;
	static String cacheDirectory = "cache";
	static Set<String> personTypes;
	static BlackList blackList;
	
	static Map<SparqlEndpointKS, List<String>> classesCache = new HashMap<>();
	static Map<String, List<String>> propertiesCache = new HashMap<>();
	static Map<SparqlEndpointKS, Map<String, List<String>>> applicableEntitiesCache = new HashMap<>();

	private static double propertyFrequencyThreshold;
	private static Cooccurrence cooccurrenceType;

	private static SPARQLReasoner reasoner;

	public static QueryExecutionFactory qef;

	public RESTService() {}


	/**
	 * Precompute all applicable classes and for each class its applicable properties.
	 * @param context
	 */
	private void precomputeApplicableEntities(ServletContext context){
		//get the classes
		List<String> classes = getClasses(context);
		//for each class get the properties
		for (String cls : classes) {
			List<String> properties = getApplicableProperties(context, cls);
			
		}
	}
	
	public static void loadConfig(HierarchicalINIConfiguration config) throws Exception {
		loadConfig(config, null);
	}
	
	public static void loadConfig(HierarchicalINIConfiguration config, ServletContext context) throws Exception {
		logger.info("Loading config...");
		
		// endpoint settings
		SubnodeConfiguration section = config.getSection("endpoint");
		URL url = new URL(section.getString("url"));
		
		SparqlEndpointKS ks;
		if(url.getProtocol().equals("file")) {
			Model model = ModelFactory.createOntologyModel(OntModelSpec.RDFS_MEM_RDFS_INF);
			RDFDataMgr.read(model, url.toString());
			ks = new LocalModelBasedSparqlEndpointKS(model);
		} else {
			String defaultGraph = section.getString("defaultGraph");
			SparqlEndpoint endpoint = new SparqlEndpoint(url, defaultGraph);
			ks = new SparqlEndpointKS(endpoint);
		}
		RESTService.ks = ks;
		
		String namespace = section.getString("namespace");
		RESTService.namespace = namespace;
		
		String cacheDirectory = section.getString("cacheDirectory", "cache");
		if(Paths.get(cacheDirectory).isAbsolute()){
			RESTService.cacheDirectory = cacheDirectory;
		} else {
			RESTService.cacheDirectory = context != null ? context.getRealPath(cacheDirectory) : cacheDirectory;
		}
		ks.setCacheDir(cacheDirectory);
		ks.init();
		logger.info("Dataset:" + ks);
		logger.info("Namespace:" + namespace);
		logger.info("Cache directory: " + RESTService.cacheDirectory);

		// summarization settings
		section = config.getSection("summarization");
		String propertyBlacklistPath = section.getString("propertyBlacklist", null);
		if(propertyBlacklistPath != null && !propertyBlacklistPath.isEmpty()) {
			propertyBlacklistPath = context == null ? 
					RESTService.class.getClassLoader().getResource(propertyBlacklistPath).getPath() :
						context.getRealPath(propertyBlacklistPath);

			RESTService.blackList = new DefaultPropertyBlackList(new File(propertyBlacklistPath));
		} else {
			RESTService.blackList = new DefaultPropertyBlackList();
		}
		
		RESTService.personTypes = Sets.newHashSet(Arrays.asList(section.getStringArray("personTypes")));
		RESTService.propertyFrequencyThreshold = section.getDouble("propertyFrequencyThreshold");
		RESTService.cooccurrenceType = Cooccurrence.valueOf(Cooccurrence.class, section.getString("cooccurrenceType").toUpperCase());
		logger.info("Summarization properties:"
				+ "\nProperty blacklist:" + propertyBlacklistPath
				+ "\nPerson types:" + personTypes
				+ "\nProperty frequency threshold:" + propertyFrequencyThreshold
				+ "\nProperty cooccurence type:" + cooccurrenceType
				);

		qef = ks.getQueryExecutionFactory();
		
		reasoner = new SPARQLReasoner(qef);
	}

	public static void init(ServletContext context){
		try {
			System.err.println(context.getServletContextName());
			String path = (context == null) ? "assess_config_dbpedia.ini" : context.getRealPath(context.getInitParameter("configFile"));
			logger.info("Loading config from " + path + "...");
			HierarchicalINIConfiguration config = new HierarchicalINIConfiguration();
			config.load(path);

			loadConfig(config, context);
		} catch (ConfigurationException e) {
			logger.error("Could not load config file.", e);
		} catch (Exception e) {
			logger.error("Illegal endpoint URL.", e);
		}
	}
	
	@GET
	@Context
	@Path("/questionsold")
	@Produces(MediaType.APPLICATION_JSON)
	public RESTQuestions getQuestionsJSON(@Context ServletContext context, @QueryParam("domain") String domain, @QueryParam("type") List<String> questionTypes, @QueryParam("limit") int maxNrOfQuestions) {
		logger.info("REST Request - Get questions\nDomain:" + domain + "\nQuestionTypes:" + questionTypes + "\n#Questions:" + maxNrOfQuestions);
		
		Map<QuestionType, QuestionGenerator> generators = Maps.newLinkedHashMap();
		
		Map<OWLEntity, Set<OWLObjectProperty>> domains = new HashMap<>();
		domains.put(new OWLClassImpl(IRI.create(domain)), new HashSet<>());
		
		// set up the question generators
		for (String type : questionTypes) {
			AbstractQuestionGenerator generator = null;
			if(type.equals(QuestionType.MC.getName())){
				generator = new MultipleChoiceQuestionGenerator(qef, cacheDirectory, domains);
			} else if(type.equals(QuestionType.JEOPARDY.getName())){
				generator = new JeopardyQuestionGenerator(qef, cacheDirectory, domains);
			} else if(type.equals(QuestionType.TRUEFALSE.getName())){
				generator = new TrueFalseQuestionGenerator(qef, cacheDirectory, domains);
			}
			generator.setPersonTypes(personTypes);
			generator.setEntityBlackList(blackList);
			generator.setNamespace(namespace);
			generators.put(generator.getQuestionType(), generator);
		}
		List<RESTQuestion> restQuestions = new ArrayList<>();
		
		// get random numbers for max. computed questions per type
		List<Integer> randomNumbers = getRandomNumbers(maxNrOfQuestions, questionTypes.size());
		
		int i = 0;
		for (Entry<QuestionType, QuestionGenerator> entry : generators.entrySet()) {
			QuestionType questionType = entry.getKey();
			QuestionGenerator generator = entry.getValue();
		
			//randomly set the max number of questions
			int max = randomNumbers.get(i);
			
			Set<Question> questions = generator.getQuestions(null, 1, max);
			
			for (Question question : questions) {
				RESTQuestion q = new RESTQuestion();
				q.setQuestion(question.getText());
				q.setQuestionType(questionType.getName());
				List<RESTAnswer> correctAnswers = new ArrayList<>();
				for (Answer answer : question.getCorrectAnswers()) {
					RESTAnswer a = new RESTAnswer();
					a.setAnswer(answer.getText());
					if(questionType == QuestionType.MC){
						a.setAnswerHint(answer.getHint());
					}
					correctAnswers.add(a);
				}
				q.setCorrectAnswers(correctAnswers);
				List<RESTAnswer> wrongAnswers = new ArrayList<>();
				for (Answer answer : question.getWrongAnswers()) {
					RESTAnswer a = new RESTAnswer();
					a.setAnswer(answer.getText());
					a.setAnswerHint("NO HINT");
					wrongAnswers.add(a);
				}
				q.setWrongAnswers(wrongAnswers);
				restQuestions.add(q);
			}
		}
		
		RESTQuestions result = new RESTQuestions();
		result.setQuestions(restQuestions);
		logger.info("Done.");
		return result;
 
	}
	
	@POST
	@Context
	@Path("/questions")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public RESTQuestions getQuestionsJSON2(@Context ServletContext context, JSONArray domain, @QueryParam("type") List<String> questionTypes, @QueryParam("limit") int maxNrOfQuestions) {
		logger.info("REST Request - Get questions\nQuestionTypes:" + questionTypes + "\n#Questions:" + maxNrOfQuestions);
		
		Map<QuestionType, QuestionGenerator> generators = Maps.newLinkedHashMap();
		
		// extract the domain from the JSON array
		Map<OWLEntity, Set<OWLObjectProperty>> domains = new HashMap<>();
		try {
			for(int i = 0; i < domain.length(); i++){
				JSONObject entry = domain.getJSONObject(i);
				OWLClass cls = new OWLClassImpl(IRI.create(entry.getString("className")));
				JSONArray propertiesArray = entry.getJSONArray("properties");
				Set<OWLObjectProperty> properties = new HashSet<>();
				for (int j = 0; j < propertiesArray.length(); j++) {
					properties.add(new OWLObjectPropertyImpl(IRI.create(propertiesArray.getString(j))));
				}
				domains.put(cls, properties);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		logger.info("Domain:" + domains);
		
		// set up the question generators
		long start = System.currentTimeMillis();
		// set up the question generators
		for (String type : questionTypes) {
			AbstractQuestionGenerator generator = null;
			if (type.equals(QuestionType.MC.getName())) {
				generator = new MultipleChoiceQuestionGenerator(qef, cacheDirectory, domains);
			} else if (type.equals(QuestionType.JEOPARDY.getName())) {
				generator = new JeopardyQuestionGenerator(qef, cacheDirectory, domains);
			} else if (type.equals(QuestionType.TRUEFALSE.getName())) {
				generator = new TrueFalseQuestionGenerator(qef, cacheDirectory, domains);
			}
			generator.setPersonTypes(personTypes);
			generator.setEntityBlackList(blackList);
			generator.setNamespace(namespace);
			generators.put(generator.getQuestionType(), generator);
		}
		long end = System.currentTimeMillis();
		System.out.println("Operation took " + (end - start) + "ms");
		final List<RESTQuestion> restQuestions = Collections.synchronizedList(new ArrayList<>(maxNrOfQuestions));
		
		// get random numbers for max. computed questions per type
		final List<Integer> partitionSizes = getRandomNumbers(maxNrOfQuestions, questionTypes.size());
		System.err.println(generators);
		ExecutorService tp = Executors.newFixedThreadPool(1);//generators.entrySet().size()
		// submit a task for each question type
        List<Future<List<RESTQuestion>>> list = new ArrayList<>();
		int i = 0;
		for (final Entry<QuestionType, QuestionGenerator> entry : generators.entrySet()) {
			QuestionType questionType = entry.getKey();
			QuestionGenerator questionGenerator = entry.getValue();
			list.add(tp.submit(new QuestionGenerationTask(questionType, questionGenerator, partitionSizes.get(i++))));
		}
		for(Future<List<RESTQuestion>> fut : list){
			try {
				List<RESTQuestion> partialRestQuestions = fut.get();
				restQuestions.addAll(partialRestQuestions);
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
        }
		tp.shutdown();
		
		RESTQuestions result = new RESTQuestions();
		result.setQuestions(restQuestions);
		
		return result;
	}
	
	@GET
	@Context
	@Path("/properties")
	@Produces(MediaType.APPLICATION_JSON)
	public List<String> getApplicableProperties(@Context ServletContext context, @QueryParam("class") String classURI) {
		logger.info("REST Request - Get all properties for class " + classURI);
		
		List<String> properties = propertiesCache.get(classURI);
		
		if(properties == null){
			properties = new ArrayList<>();
			for (OWLObjectProperty p : reasoner.getObjectProperties(new OWLClassImpl(IRI.create(classURI)))) {
				if(!blackList.contains(p.toStringID())){
					properties.add(p.toStringID());
				}
			}
			Collections.sort(properties); 
			propertiesCache.put(classURI, properties);
		}
		
		logger.info("Done.");
		return properties;
	}
	
	@GET
	@Context
	@Path("/classes")
	@Produces(MediaType.APPLICATION_JSON)
	public List<String> getClasses(@Context ServletContext context) {
		logger.info("REST Request - Get all classes");
		
		List<String> classes = classesCache.get(ks);
		
		if(classes == null){
			classes = new ArrayList<>();
			for (OWLClass cls : reasoner.getNonEmptyOWLClasses()) {
				if ((namespace != null && cls.toStringID().startsWith(namespace)) && 
						!blackList.contains(cls.toStringID())) {
					classes.add(cls.toStringID());
				}
			}
			Collections.sort(classes); 
			classesCache.put(ks, classes);
		}
		logger.info("Done.");
		return classes;
	}
	
	@GET
	@Context
	@Path("/entities")
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, List<String>> getEntities(@Context ServletContext context) {
		logger.info("REST Request - Get all applicable entities");

		Map<String, List<String>> entities = applicableEntitiesCache.get(ks);

		if(entities == null){
			entities = new LinkedHashMap<>();
			// get the classes
			List<String> classes = getClasses(context);
			
			// for each class get the properties
			for (String cls : classes) {
				List<String> properties = getApplicableProperties(context, cls);
				if (!properties.isEmpty()) {
					entities.put(cls, properties);
				}
			}
			applicableEntitiesCache.put(ks, entities);
		}
		logger.info("Done.");
		return entities;
	}

	@POST
	@Path("/precompute-graphs")
	public void precomputeGraphs(ServletContext context){
		logger.info("Precomputing graphs...");
		
		DatasetBasedGraphGenerator graphGenerator = new CachedDatasetBasedGraphGenerator(qef, cacheDirectory);
		
		Map<String, List<String>> entities = getEntities(null);
		for (String cls : entities.keySet()) {
			try {
				logger.info(cls);
				graphGenerator.generateGraph(new OWLClassImpl(IRI.create(cls)), propertyFrequencyThreshold, namespace, cooccurrenceType);
			} catch (Exception e) {
				logger.error(e.getLocalizedMessage(), e);
			}
		}

		logger.info("Precomputing graphs finished.");
	}

	@GET
	@Path("/compute-graph")
	public Response computeGraph(@QueryParam("class") String cls){
		logger.info("Computing graph for {} ...", cls);

		DatasetBasedGraphGenerator graphGenerator = new CachedDatasetBasedGraphGenerator(qef, cacheDirectory);

		try {
			WeightedGraph g = graphGenerator.generateGraph(new OWLClassImpl(IRI.create(cls)),
																	   propertyFrequencyThreshold, namespace,
																	   cooccurrenceType);
			logger.info("Computing graph for {} finished.", cls);

			return Response.ok(g.toString(), MediaType.TEXT_PLAIN_TYPE).build();
		} catch (Exception e) {
			logger.error("Computing graph for {} failed.", cls, e);
			return Response.status(Response.Status.BAD_REQUEST).entity(e).build();
		}
	}
	
	private List<Integer> getRandomNumbers(int total, int groups){
		Random rnd = new Random(123);
		List<Integer> partitionSizes = new ArrayList<>(groups);
		for (int i = 0; i < groups - 1; i++) {
			int number = rnd.nextInt(total-(groups - i)) + 1;
			total -= number;
			partitionSizes.add(number);
		}
		partitionSizes.add(groups-1, total);
		return partitionSizes;
	}
	
	class QuestionGenerationTask implements Callable<List<RESTQuestion>>{
		
		private QuestionType questionType;
		private QuestionGenerator questionGenerator;
		private int maxNrOfQuestions;

		public QuestionGenerationTask(QuestionType questionType, QuestionGenerator questionGenerator, int maxNrOfQuestions) {
			this.questionType = questionType;
			this.questionGenerator = questionGenerator;
			this.maxNrOfQuestions = maxNrOfQuestions;
		}

		/* (non-Javadoc)
		 * @see java.util.concurrent.Callable#call()
		 */
		@Override
		public List<RESTQuestion> call() throws Exception {
			logger.info("Get " + maxNrOfQuestions + " questions of type " + questionType.getName() + "...");
			Set<Question> questions = questionGenerator.getQuestions(null, 1, maxNrOfQuestions);
			
			// convert to REST format
			List<RESTQuestion> restQuestions = new ArrayList<>(questions.size());
			for (Question question : questions) {
				// convert question
				RESTQuestion q = new RESTQuestion();
				q.setQuestion(question.getText());
				q.setQuestionType(questionType.getName());
				
				// convert correct answers
				List<RESTAnswer> correctAnswers = new ArrayList<>();
				for (Answer answer : question.getCorrectAnswers()) {
					RESTAnswer a = new RESTAnswer();
					a.setAnswer(answer.getText());
					if(questionType == QuestionType.MC){
						a.setAnswerHint(answer.getHint());
					}
					correctAnswers.add(a);
				}
				q.setCorrectAnswers(correctAnswers);
				
				// convert wrong answers
				List<RESTAnswer> wrongAnswers = new ArrayList<>();
				for (Answer answer : question.getWrongAnswers()) {
					RESTAnswer a = new RESTAnswer();
					a.setAnswer(answer.getText());
					a.setAnswerHint("NO HINT");
					wrongAnswers.add(a);
				}
				q.setWrongAnswers(wrongAnswers);
				restQuestions.add(q);
			}
			return restQuestions;
		}
	}
}
