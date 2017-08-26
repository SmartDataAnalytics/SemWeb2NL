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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.avatar;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.aksw.avatar.clustering.BorderFlowX;
import org.aksw.avatar.clustering.Node;
import org.aksw.avatar.clustering.WeightedGraph;
import org.aksw.avatar.clustering.hardening.HardeningFactory;
import org.aksw.avatar.clustering.hardening.HardeningFactory.HardeningType;
import org.aksw.avatar.dataset.CachedDatasetBasedGraphGenerator;
import org.aksw.avatar.dataset.DatasetBasedGraphGenerator;
import org.aksw.avatar.dataset.DatasetBasedGraphGenerator.Cooccurrence;
import org.aksw.avatar.exceptions.NoGraphAvailableException;
import org.aksw.avatar.rules.*;
import org.aksw.avatar.util.DatasetConstraints;
import org.aksw.avatar.util.dbpedia.DBpediaDatasetConstraints;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.sparql2nl.naturallanguagegeneration.SimpleNLGwithPostprocessing;
import org.aksw.triple2nl.TripleConverter;
import org.aksw.triple2nl.gender.*;
import org.aksw.triple2nl.gender.Gender;
import org.apache.jena.datatypes.xsd.impl.XSDAbstractDateTimeType;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.log4j.Logger;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.utilities.MapUtils;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLIndividual;
import simplenlg.features.*;
import simplenlg.framework.CoordinatedPhraseElement;
import simplenlg.framework.NLGElement;
import simplenlg.phrasespec.NPPhraseSpec;
import simplenlg.phrasespec.SPhraseSpec;
import simplenlg.realiser.english.Realiser;
import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A verbalizer for triples without variables.
 *
 * @author ngonga
 */
public class Avatar {

	private static final Logger logger = Logger.getLogger(Avatar.class.getName());

	private static String DEFAULT_CACHE_BASE_DIR = System.getProperty("java.io.tmpdir");

	private static final double DEFAULT_THRESHOLD = 0.4;
	private static final int DEFAULT_MAX_NUMBER_OF_SHOWN_VALUES_PER_PROPERTY = 5;
	private static final Cooccurrence DEFAULT_COOCCURRENCE_TYPE = Cooccurrence.PROPERTIES;
	private static final HardeningType DEFAULT_HARDENING_TYPE = HardeningType.SMALLEST;

//    public SimpleNLGwithPostprocessing nlg;
    String language = "en";
    protected Realiser realiser;
    Map<Resource, String> labels;
    NumericLiteralFilter litFilter;
    TypeAwareGenderDetector gender;
    public Map<Resource, Collection<Triple>> resource2Triples;
    private QueryExecutionFactory qef;
    private String cacheDirectory = "cache/sparql";
    PredicateMergeRule pr;
    ObjectMergeRule or;
    SubjectMergeRule sr;
    public DatasetBasedGraphGenerator graphGenerator;
    int maxShownValuesPerProperty = DEFAULT_MAX_NUMBER_OF_SHOWN_VALUES_PER_PROPERTY;
    boolean omitContentInBrackets = true;

    private TripleConverter tripleConverter;

    private DatasetConstraints datasetConstraints = new DatasetConstraints();

    public Avatar(QueryExecutionFactory qef, String cacheDirectory) {
    	this.qef = qef;

		if(cacheDirectory == null) {
			cacheDirectory = DEFAULT_CACHE_BASE_DIR;
		}
		cacheDirectory = new File(cacheDirectory, "avatar-cache/sparql").getAbsolutePath();

		tripleConverter = new TripleConverter(qef, cacheDirectory);
        realiser = tripleConverter.realiser;

        labels = new HashMap<>();
        litFilter = new NumericLiteralFilter(qef, cacheDirectory);


        pr = new PredicateMergeRule(tripleConverter.lexicon, tripleConverter.nlgFactory, tripleConverter.realiser);
        or = new ObjectMergeRule(tripleConverter.lexicon, tripleConverter.nlgFactory, tripleConverter.realiser);
        sr = new SubjectMergeRule(tripleConverter.lexicon, tripleConverter.nlgFactory, tripleConverter.realiser);

        gender = new TypeAwareGenderDetector(qef, new DictionaryBasedGenderDetector());

        graphGenerator = new CachedDatasetBasedGraphGenerator(qef, cacheDirectory);
    }

    public Avatar(SparqlEndpoint endpoint, String cacheDirectory) {
    	this(new QueryExecutionFactoryHttp(endpoint.getURL().toString(), endpoint.getDefaultGraphURIs()), cacheDirectory);
    }

	public Avatar(SparqlEndpoint endpoint) {
		this(endpoint, DEFAULT_CACHE_BASE_DIR);
	}

    /**
     * @param personTypes the personTypes to set
     */
    public void setPersonTypes(Set<String> personTypes) {
        gender.setPersonTypes(personTypes);
    }

    /**
     * @param omitContentInBrackets the omitContentInBrackets to set
     */
    public void setOmitContentInBrackets(boolean omitContentInBrackets) {
        this.omitContentInBrackets = omitContentInBrackets;
    }

    public void setGenderDetector(TypeAwareGenderDetector genderDetector) {
        this.gender = genderDetector;
    }

    /**
     * Gets all triples for resource r and property p.
     * If outgoing is true it returns all triples with <r,p,o>, else <s,p,r>
     *
     * @param r the resource
     * @param p the property
     * @param outgoing whether to get outgoing or ingoing triples
     * @return A set of triples
     */
    private Set<Triple> getTriples(Resource r, Property p, boolean outgoing) {
        Set<Triple> result = new HashSet<>();
        try {
        	String q;
        	if(outgoing){
        		q = "SELECT ?o where { <" + r.getURI() + "> <" + p.getURI() + "> ?o.}";
        	} else {
        		q = "SELECT ?o where { ?o <" + p.getURI() + "> <" + r.getURI() + ">.}";
        	}
        	q += " LIMIT " + maxShownValuesPerProperty+1;

			try(QueryExecution qe = qef.createQueryExecution(q)) {
				ResultSet results = qe.execSelect();
				if (results.hasNext()) {
					while (results.hasNext()) {
						RDFNode n = results.next().get("o");
						result.add(Triple.create(r.asNode(), p.asNode(), n.asNode()));
					}
				}
			}
        } catch (Exception e) {
            e.printStackTrace();
        }

        // merge triple with redundant objects modulo syntactic difference
		merge(result);

        return result;
    }

	Function<LiteralLabel, Optional<DateTime>> funcLitToDatetime = (LiteralLabel lit) -> {
		DateTime time = null;

		try {
			time = ISODateTimeFormat.dateTimeParser().parseDateTime(lit.getLexicalForm());
		} catch (Exception e1) {
            logger.warn("Can't parse date time from literal " + lit + " ." + e1.getMessage());
			try {
				time = ISODateTimeFormat.localDateParser().parseDateTime(lit.getLexicalForm());
			} catch (Exception e2) {
                logger.warn("Can't parse local date from literal " + lit + " ." + e2.getMessage());
				try{
				    time = ISODateTimeFormat.dateParser().parseDateTime(lit.getLexicalForm());
                } catch (Exception e3) {
                    logger.warn("Can't parse date from literal " + lit + " ." + e3.getMessage());
                }
			}
		}

		return Optional.ofNullable(time);
	};


	public void merge(Set<Triple> triples) {
		//
		Map<LiteralLabel, Triple> nodeTripleMap = triples.stream()
				.filter(t -> t.getObject().isLiteral())
				.collect(Collectors.toMap(t -> t.getObject().getLiteral(),
										  Function.identity()));

		// 1. process date literals
        // filter for date literals
		Map<LiteralLabel, Triple> dateLiteralsMap = nodeTripleMap.entrySet().stream()
				.filter((e) -> e.getKey().getDatatype() != null && e.getKey().getDatatype() instanceof XSDAbstractDateTimeType)
				.collect(Collectors.toMap(e -> e.getKey(),
										  e -> e.getValue()));
		// convert to DateTime objects
		Map<Optional<DateTime>, Triple> dateTimeEntryMap = dateLiteralsMap.entrySet().stream()
				.collect(Collectors.toMap(e -> funcLitToDatetime.apply(e.getKey()), // convert date literal to DateTime object
										  e -> e.getValue(), // the triple as object
										 (t1, t2) -> { // merge triples with same date
											 System.out.println("duplicate date found!");
											 return t1;
										 }));
		dateTimeEntryMap = dateTimeEntryMap.entrySet().stream().filter(e -> e.getKey().isPresent()).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

		triples.removeAll(Sets.difference(Sets.newHashSet(dateLiteralsMap.values()), Sets.newHashSet(dateTimeEntryMap.values())));
	}

    public String realize(List<NLGElement> elts) {
        if(elts.isEmpty()) return null;

        return elts.stream()
                .map(elt -> realiser.realiseSentence(elt))
                .collect(Collectors.joining(" ")).trim();
    }

    /**
     * Takes the output of the clustering for a given class and a resource.
     * Returns the verbalization for the resource
     *
     * @param clusters Output of the clustering
     * @param resource Resource to summarize
     * @return List of NLGElement
     */
    private List<NLGElement> generateSentencesFromClusters(List<Set<Node>> clusters,
            Resource resource, OWLClass namedClass, boolean replaceSubjects) {
        //compute the gender of the resource
        Gender g = getGender(resource);

        //get a list of possible subject replacements
        List<NPPhraseSpec> subjects = generateSubjects(resource, namedClass, g);
        
        List<NLGElement> result = new ArrayList<>();
        Collection<Triple> allTriples = new ArrayList<>();
        DateLiteralFilter dateFilter = new DateLiteralFilter();
//      
        for (Set<Node> propertySet : clusters) {
            //add up all triples for the given set of properties
            List<SPhraseSpec> buffer = new ArrayList<>();
            for (Node property : propertySet) {
                Set<Triple> triples = getTriples(resource, ResourceFactory.createProperty(property.label), property.outgoing);
                litFilter.filter(triples);
                dateFilter.filter(triples);
                //restrict the number of shown values for the same property
                boolean subsetShown = false;
                if (triples.size() > maxShownValuesPerProperty) {
                    triples = getSubsetToShow(triples);
                    subsetShown = true;
                }
                //all share the same property, thus they can be merged
                List<SPhraseSpec> phraseSpecs = getPhraseSpecsFromTriples(triples, property.outgoing);
				buffer.addAll(or.apply(phraseSpecs, subsetShown));
                allTriples.addAll(triples);
            }
            List<NLGElement> mergedElement = sr.apply(or.apply(buffer), g);
			result.addAll(mergedElement);
        }

        resource2Triples.put(resource, allTriples);

        List<NLGElement> phrases = new ArrayList<>();
        if (replaceSubjects) {
			for (NLGElement phrase : result) {
				NLGElement replacedPhrase = replaceSubject(phrase, subjects, g);
				phrases.add(replacedPhrase);
			}
            return phrases;
        } else {
            return result;
        }

    }

    private Set<Triple> getSubsetToShow(Set<Triple> triples) {
        Set<Triple> triplesToShow = new HashSet<>(maxShownValuesPerProperty);
        for (Triple triple : sortByObjectPopularity(triples)) {
            if (triplesToShow.size() < maxShownValuesPerProperty) {
                triplesToShow.add(triple);
            }
        }

        return triplesToShow;
    }

    /**
     * Sorts the given triples by the popularity of the triple objects.
     * @param triples the triples
     * @return a list of sorted triples
     */
    private List<Triple> sortByObjectPopularity(Set<Triple> triples) {
        List<Triple> orderedTriples = new ArrayList<>();

        //if one of the objects is a literal we do not sort 
        if (triples.iterator().next().getObject().isLiteral()) {
            orderedTriples.addAll(triples);
        } else {
            //we get the popularity of the object
            Map<Triple, Integer> triple2ObjectPopularity = new HashMap<>();
            for (Triple triple : triples) {
                if (triple.getObject().isURI()) {
                    String query = "SELECT (COUNT(*) AS ?cnt) WHERE {<" + triple.getObject().getURI() + "> ?p ?o.}";
                    QueryExecution qe = qef.createQueryExecution(query);
                    try {
						ResultSet rs = qe.execSelect();
						int popularity = rs.next().getLiteral("cnt").getInt();
						triple2ObjectPopularity.put(triple, popularity);
						qe.close();
					} catch (Exception e) {
						logger.warn("Execution of SPARQL query failed: " + e.getMessage() + "\n" + query);
					}
                }
            }
            List<Entry<Triple, Integer>> sortedByValues = MapUtils.sortByValues(triple2ObjectPopularity);

            for (Entry<Triple, Integer> entry : sortedByValues) {
                Triple triple = entry.getKey();
                orderedTriples.add(triple);
            }
        }

        return orderedTriples;
    }

    /**
     * Returns the triples of the summary for the given resource.
     * @param resource the resource of the summary
     * @return a set of triples
     */
    public Collection<Triple> getSummaryTriples(Resource resource) {
        return resource2Triples.get(resource);
    }

    /**
     * Returns the triples of the summary for the given individual.
     * @param individual the individual of the summary
     * @return a set of triples
     */
    public Collection<Triple> getSummaryTriples(OWLIndividual individual) {
        return getSummaryTriples(ResourceFactory.createResource(individual.toStringID()));
    }

    /**
     * Generates sentence for a given set of triples
     *
     * @param triples A set of triples
     * @return A set of sentences representing these triples
     */
    public List<NLGElement> generateSentencesFromTriples(Set<Triple> triples, boolean outgoing, Gender g) {
        return applyMergeRules(getPhraseSpecsFromTriples(triples, outgoing), g);
    }

    /**
     * Generates sentence for a given set of triples
     *
     * @param triples A set of triples
     * @return A set of sentences representing these triples
     */
    public List<SPhraseSpec> getPhraseSpecsFromTriples(Set<Triple> triples, boolean outgoing) {
        return triples.stream()
                .map(t -> generateSimplePhraseFromTriple(t, outgoing))
                .collect(Collectors.toList());
    }

    /**
     * Generates a set of sentences by merging the sentences in the list as well
     * as possible
     *
     * @param triples List of triples
     * @return List of sentences
     */
    public List<NLGElement> applyMergeRules(List<SPhraseSpec> triples, Gender g) {
        List<SPhraseSpec> phrases = new ArrayList<>();
        phrases.addAll(triples);

        int newSize = phrases.size(), oldSize = phrases.size() + 1;

        //apply merging rules if more than one sentence to merge
        if (newSize > 1) {
            //fix point iteration for object and predicate merging
            while (newSize < oldSize) {
                oldSize = newSize;
                int orCount = or.isApplicable(phrases);
                int prCount = pr.isApplicable(phrases);
                if (prCount > 0 || orCount > 0) {
                    if (prCount > orCount) {
                        phrases = pr.apply(phrases);
                    } else {
                        phrases = or.apply(phrases);
                    }
                }
                newSize = phrases.size();
            }
        }
        return sr.apply(phrases, g);
    }

    /**
     * Generates a simple phrase for a triple
     *
     * @param triple A triple
     * @return A simple phrase
     */
    private SPhraseSpec generateSimplePhraseFromTriple(Triple triple) {
        return generateSimplePhraseFromTriple(triple, true);
    }
    
    /**
     * Generates a simple phrase for a triple
     *
     * @param triple A triple
     * @return A simple phrase
     */
	private SPhraseSpec generateSimplePhraseFromTriple(Triple triple, boolean outgoing) {
        return tripleConverter.convertToPhrase(triple, false, !outgoing);
    }

    public List<NLGElement> verbalize(OWLIndividual ind, OWLClass nc, String namespace, double threshold, Cooccurrence cooccurrence, HardeningType hType) {
        return verbalize(Sets.newHashSet(ind), nc, namespace, threshold, cooccurrence, hType).get(ind);
    }

    public Map<OWLIndividual, List<NLGElement>> verbalize(Set<OWLIndividual> individuals, OWLClass nc, String namespace, double threshold, Cooccurrence cooccurrence, HardeningType hType) {
        resource2Triples = new HashMap<>();
        
        // first get graph for nc
        try {
			WeightedGraph wg = graphGenerator.generateGraph(nc, threshold, namespace, cooccurrence);

			// then cluster the graph
			BorderFlowX bf = new BorderFlowX(wg);
			Set<Set<Node>> clusters = bf.cluster();
			//then harden the results
			List<Set<Node>> sortedPropertyClusters = HardeningFactory.getHardening(hType).harden(clusters, wg);
			logger.info("Cluster = " + sortedPropertyClusters);

			Map<OWLIndividual, List<NLGElement>> verbalizations = new HashMap<>();

			for (OWLIndividual ind : individuals) {
			    //finally generate sentences from clusters
			    List<NLGElement> phrases = generateSentencesFromClusters(
			            sortedPropertyClusters,
                        ResourceFactory.createResource(ind.toStringID()), nc, true);

			    Triple t = Triple.create(
			            ResourceFactory.createResource(ind.toStringID()).asNode(),
                        ResourceFactory.createProperty(RDF.type.getURI()).asNode(),
			            ResourceFactory.createResource(nc.toStringID()).asNode());

			    // put the sentence in first position
			    phrases.add(0, generateSimplePhraseFromTriple(t));

			    verbalizations.put(ind, phrases);

			    resource2Triples.get(ResourceFactory.createResource(ind.toStringID())).add(t);
			}

			return verbalizations;
		} catch (NoGraphAvailableException e) {
			e.printStackTrace();
		}
        
        return null;
    }



    /**
     * Returns a textual summary of the given entity or <code>null</code> if we couldn't find any data.
     *
     * @return
     */
    public String summarize(OWLIndividual individual) {
    	// compute the most specific type first
    	Optional<OWLClass> cls = getMostSpecificType(individual, datasetConstraints);

    	// if there is no such type, return null
        if(!cls.isPresent()) {
			logger.warn(String.format("Could not find a class for the individual %s. Currently, this is necessary to compute a summary.", individual));
			return null;
        }

        return summarize(individual, cls.get());
    }
    
    /**
     * Returns a textual summary of the given entity.
     *
     * @return
     */
    public String summarize(OWLIndividual individual, OWLClass nc) {
       return getSummary(individual, nc, DEFAULT_THRESHOLD, DEFAULT_COOCCURRENCE_TYPE, DEFAULT_HARDENING_TYPE);
    }

    /**
     * Returns a textual summary of the given entity.
     *
     * @return
     */
    public String getSummary(OWLIndividual individual, OWLClass nc, double threshold, Cooccurrence cooccurrence, HardeningType hType) {
        assert individual != null;
        assert nc != null;

        logger.info(String.format("Computing summary for entity <%s> and the type <%s>...", individual.toStringID(), nc.toStringID()));

        List<NLGElement> elements = verbalize(individual, nc, null, threshold, cooccurrence, hType);

        // check for tense
        if(isPastTense(individual)) {
            elements.forEach(elt -> elt.setFeature(Feature.TENSE, Tense.PAST));
        }

        String summary = realize(elements);
        summary = summary.replaceAll("\\s?\\((.*?)\\)", "");
        summary = summary.replace(" , among others,", ", among others,");

        return summary;
    }

    private boolean isPastTense(OWLIndividual ind) {
        String query = String.format("ASK {%s dbo:deathDate ?o }", ind);
        try(QueryExecution qe = qef.createQueryExecution(query)) {
            return qe.execAsk();
        }
    }

    private Optional<OWLClass> getMostSpecificType(OWLIndividual ind,
												   DatasetConstraints c){

        logger.info("Getting the most specific type of " + ind);
        String query = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"
                        + "select distinct ?type where {"
                        + " ?ind a ?type ."
//    			+ "?type a owl:Class ." // too strict, thus currently omitted
                        + "filter not exists {?subtype ^a ?ind ; (rdfs:subClassOf|owl:equivalentClass)* ?type .filter(?subtype != ?type)}\n" +
                        "#1 filter(%NS_ALLOWED)\n" +
                        "#2 filter(%NS_IGNORED)\n" +
                        "#3 filter(%CLS_ALLOWED)\n" +
                        "#4 filter(%CLS_IGNORED)\n" +
                        "}";

        if(!c.getAllowedNamespaces().isEmpty()) {
            query = query.replace("#1", "");
            query = query.replace("%NS_ALLOWED",
								  c.getAllowedNamespaces().stream().map(ns -> "STRSTARTS(STR(?type), '" + ns + "')").collect(Collectors.joining(" && ")));
        } else {
            query = query.replaceAll( ".*#1.*(\\r?\\n|\\r)?", "" );
        }

        if(!c.getIgnoredNamespaces().isEmpty()) {
            query = query.replace("#2", "");
            query = query.replace("%NS_IGNORED",
                    c.getIgnoredNamespaces().stream().map(ns -> "!STRSTARTS(STR(?type), '" + ns + "')").collect(Collectors.joining(" && ")));
        } else {
            query = query.replaceAll( ".*#2.*(\\r?\\n|\\r)?", "" );
        }

        if(!c.getAllowedClasses().isEmpty()) {
            query = query.replace("#3", "");
            query = query.replace("%CLS_ALLOWED",
                    c.getAllowedClasses().stream().map(cls -> "?type = <" + cls + ">").collect(Collectors.joining(" || ")));
        } else {
            query = query.replaceAll( ".*#3.*(\\r?\\n|\\r)?", "" );
        }

        if(!c.getIgnoredClasses().isEmpty()) {
            query = query.replace("#4", "");
            query = query.replace("%CLS_IGNORED",
                    c.getIgnoredClasses().stream().map(cls -> "?type != <" + cls + ">").collect(Collectors.joining(" && ")));
        } else {
            query = query.replaceAll( ".*#4.*(\\r?\\n|\\r)?", "" );
        }

		query = query.replace("?ind", "<" + ind.toStringID() + ">");

        logger.debug(query);

        SortedSet<OWLClass> types = new TreeSet<>();

        try(QueryExecution qe = qef.createQueryExecution(query)) {
            ResultSet rs = qe.execSelect();
            while (rs.hasNext()) {
                QuerySolution qs = rs.next();
                if(qs.get("type").isURIResource()){
                    types.add(new OWLClassImpl(IRI.create(qs.getResource("type").getURI())));
                }
            }
        }

        if(types.isEmpty()) {
            logger.warn("Could not determine most specific type for individual " + ind + ". Using random type.");
            query = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"
                    + "select distinct ?type where {"
                    + " ?ind a ?type .}";

            try(QueryExecution qe = qef.createQueryExecution(query)) {
                ResultSet rs = qe.execSelect();
                while (rs.hasNext()) {
                    QuerySolution qs = rs.next();
                    if(qs.get("type").isURIResource()){
                        types.add(new OWLClassImpl(IRI.create(qs.getResource("type").getURI())));
                    }
                }
            }
        }

        if(types.isEmpty()) {
            logger.warn("Individual " + ind + " doesn't have any directly asserted type in the dataset.");
            return Optional.empty();
        }

        //of more than one type exists, we have to choose one
        //TODO
        OWLClass msc = types.first();

        logger.info("Most specific type for " + ind + " being used is " + msc);
        return Optional.of(msc);
    }
    
    /**
     * Returns a list of synonymous expressions as subject for the given resource.
     * @param resource the resource
     * @param resourceType the type of the resource
     * @param resourceGender the gender of the resource
     * @return list of synonymous expressions
     */
    public List<NPPhraseSpec> generateSubjects(Resource resource, OWLClass resourceType, Gender resourceGender) {
        List<NPPhraseSpec> result = new ArrayList<>();

        // the textual representation of the resource itself
        result.add(tripleConverter.getNPPhrase(resource.getURI(), false, false));

        // the class, e.g. 'this book'
        NPPhraseSpec np = tripleConverter.getNPPhrase(resourceType.toStringID(), false, true);
        np.addPreModifier("This");
        result.add(np);

        // the pronoun depending on the gender of the resource
        if (resourceGender.equals(Gender.MALE)) {
            result.add(tripleConverter.nlgFactory.createNounPhrase("he"));
        } else if (resourceGender.equals(Gender.FEMALE)) {
            result.add(tripleConverter.nlgFactory.createNounPhrase("she"));
        } else {
            result.add(tripleConverter.nlgFactory.createNounPhrase("it"));
        }
        return result;
    }
    
    /**
     * Returns the gender of the given resource.
     * @param resource
     * @return the gender
     */
    private Gender getGender(Resource resource){
    	//get a textual representation of the resource
    	String label = realiser.realiseSentence(tripleConverter.getNPPhrase(resource.getURI(), false, false));
    	//we take the first token because we assume this is the first name
        String firstToken = label.split(" ")[0];
        //lookup the gender
        Gender g = gender.getGender(resource.getURI(), firstToken);
        return g;
    }

    /**
     * @param maxShownValuesPerProperty the max. number of shown values per properties for multi-value properties
     */
    public void setMaxShownValuesPerProperty(int maxShownValuesPerProperty) {
        this.maxShownValuesPerProperty = maxShownValuesPerProperty;
    }

    /**
     * Replaces the subject of a coordinated phrase or simple phrase with a
     * subject from a list of precomputed subjects
     *
     * @param phrase
     * @param subjects
     * @return Phrase with replaced subject
     */
    protected NLGElement replaceSubject(NLGElement phrase, List<NPPhraseSpec> subjects, Gender g) {
        SPhraseSpec sphrase;
        if (phrase instanceof SPhraseSpec) {
            sphrase = (SPhraseSpec) phrase;
        } else if (phrase instanceof CoordinatedPhraseElement) {
            sphrase = (SPhraseSpec) phrase.getChildren().get(0);
        } else {
            return phrase;
        }
        int index = (int) Math.floor(Math.random() * subjects.size());
//        index = 2;

        // get the gender feature
        simplenlg.features.Gender genderFeature;
        if (g.equals(Gender.MALE)) {
            genderFeature = simplenlg.features.Gender.MASCULINE;
        } else if (g.equals(Gender.FEMALE)) {
            genderFeature = simplenlg.features.Gender.FEMININE;
        } else {
            genderFeature = simplenlg.features.Gender.NEUTER;
        }
        
        // possessive as specifier of the NP 
        NLGElement currentSubject = sphrase.getSubject();
        if (currentSubject.hasFeature(InternalFeature.SPECIFIER) && currentSubject.getFeatureAsElement(InternalFeature.SPECIFIER).getFeatureAsBoolean(Feature.POSSESSIVE)) //possessive subject
        {
            NPPhraseSpec newSubject = tripleConverter.nlgFactory.createNounPhrase(((NPPhraseSpec) currentSubject).getHead());
            
            NPPhraseSpec newSpecifier = tripleConverter.nlgFactory.createNounPhrase(subjects.get(index));
            newSpecifier.setFeature(Feature.POSSESSIVE, true);
            newSubject.setSpecifier(newSpecifier);

            if (index >= subjects.size() - 1) {
                newSpecifier.setFeature(LexicalFeature.GENDER, genderFeature);
                newSpecifier.setFeature(Feature.PRONOMINAL, true);
            }

            if (currentSubject.isPlural()) {
                newSubject.setPlural(true);
                newSpecifier.setFeature(Feature.NUMBER, NumberAgreement.SINGULAR);
            }
            sphrase.setSubject(newSubject);
        } else {
        	currentSubject.setFeature(Feature.PRONOMINAL, true);
            currentSubject.setFeature(LexicalFeature.GENDER, genderFeature);
        }
        return phrase;
    }

	public void setDatasetConstraints(DatasetConstraints datasetConstraints) {
		this.datasetConstraints = datasetConstraints;
		graphGenerator.setPropertiesBlacklist(datasetConstraints.getIgnoredProperties());
	}

	public static void main(String args[]) throws IOException {
    	OptionParser parser = new OptionParser() {
			{
				acceptsAll(Lists.newArrayList("e", "endpoint"), "SPARQL endpoint URL to be used.").withRequiredArg().ofType(URL.class).required();
				acceptsAll(Lists.newArrayList("g", "graph"), "URI of default graph for queries on SPARQL endpoint.").withRequiredArg().ofType(URI.class);
				acceptsAll(Lists.newArrayList("i", "individual"), "The URI of the entity to summarize.")
						.withRequiredArg()
						.ofType(URI.class)
						.required();
				acceptsAll(Lists.newArrayList("c", "class"), "Optionally, you can specify a class which is supposed to be the most common type of the entity to summarize.")
						.withRequiredArg().
						ofType(URI.class);
				acceptsAll(Lists.newArrayList("p", "persontypes"), "Optionally, you can specify the classes that denote persons in your knowledge base.")
						.withRequiredArg().
						ofType(String.class);
				acceptsAll(Lists.newArrayList("cache"),
						   "Path to cache directory. If not set, the operating system temporary directory will be used.")
						.withRequiredArg().defaultsTo(System.getProperty("java.io.tmpdir"));
				acceptsAll(Lists.newArrayList("h", "?"), "show help").forHelp();

			}
		};

		// parse options and display a message for the user in case of problems
		OptionSet options = null;
		try {
			options = parser.parse(args);
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage() + ". Use -? to get help.");
			System.exit(0);
		}
		
		// print help screen
		if (options.has("?")) {
			parser.printHelpOn(System.out);
		}

		// parse the SPARQL endpoint
		URL endpointURL = null;
		try {
			endpointURL = (URL) options.valueOf("endpoint");
		} catch(OptionException e) {
			System.out.println(String.format("The specified endpoint <%s> appears not to be a proper URL.", endpointURL));
			System.exit(0);
		}
		URI defaultGraphURI = null;
		if(options.has("g")){
			try {
				defaultGraphURI = (URI) options.valueOf("graph");
			} catch (IllegalArgumentException | OptionException e) {
				System.err.println(String.format("The specified graph <%s> appears not to be a proper URI.", defaultGraphURI));
				System.exit(0);
			}
		}
		SparqlEndpoint endpoint = new SparqlEndpoint(endpointURL, defaultGraphURI.getPath());

		// parse the cache directory
        String cacheDirectory = (String) options.valueOf("cache");

        Avatar avatar = new Avatar(endpoint, cacheDirectory);

        // get the entity to summarize
        OWLIndividual ind = new OWLNamedIndividualImpl(IRI.create(options.valueOf("i").toString()));

        // optionally, we can get the class of the entity used for summarization
		Optional<OWLClass> cls = Optional.ofNullable(
				options.has("c")
						? new OWLClassImpl(IRI.create((URI) options.valueOf("c")))
						: null
		);

		// optionally, set the person types
		if(options.has("p")) {
			List<String> personTypes = Splitter.on(',').trimResults().omitEmptyStrings().splitToList((String) options.valueOf("p"));
            TypeAwareGenderDetector genderDetector = new TypeAwareGenderDetector(avatar.qef, new DelegateGenderDetector(Lists.newArrayList(
                    new PropertyBasedGenderDetector(avatar.qef, Lists.newArrayList("http://xmlns.com/foaf/0.1/gender")),
                    new DictionaryBasedGenderDetector())));
            genderDetector.setPersonTypes(Sets.newHashSet(personTypes));
            avatar.setGenderDetector(genderDetector);
		}

		avatar.setDatasetConstraints(DBpediaDatasetConstraints.getInstance());

		// generate the summary
		String summary = cls.isPresent() ? avatar.summarize(ind, cls.get()) : avatar.summarize(ind);

        summary = summary.replaceAll("\\s?\\((.*?)\\)", "");
        summary = summary.replace(" , among others,", ", among others,");

        System.out.println(summary);
    }
}
