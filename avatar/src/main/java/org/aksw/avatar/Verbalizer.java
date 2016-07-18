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
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.RDF;
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
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.sparql2nl.naturallanguagegeneration.SimpleNLGwithPostprocessing;
import org.aksw.triple2nl.gender.Gender;
import org.aksw.triple2nl.gender.LexiconBasedGenderDetector;
import org.aksw.triple2nl.gender.TypeAwareGenderDetector;
import org.apache.log4j.Logger;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.utilities.MapUtils;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLIndividual;
import simplenlg.features.Feature;
import simplenlg.features.InternalFeature;
import simplenlg.features.LexicalFeature;
import simplenlg.features.NumberAgreement;
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
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;

/**
 * A verbalizer for triples without variables.
 *
 * @author ngonga
 */
public class Verbalizer {
	
	private static final Logger logger = Logger.getLogger(Verbalizer.class.getName());

	private static String DEFAULT_CACHE_BASE_DIR = System.getProperty("java.io.tmpdir");

	private static final double DEFAULT_THRESHOLD = 0.4;
	private static final int DEFAULT_MAX_NUMBER_OF_SHOWN_VALUES_PER_PROPERTY = 5;
	private static final Cooccurrence DEFAULT_COOCCURRENCE_TYPE = Cooccurrence.PROPERTIES;
	private static final HardeningType DEFAULT_HARDENING_TYPE = HardeningType.SMALLEST;

    public SimpleNLGwithPostprocessing nlg;
    SparqlEndpoint endpoint;
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
    
    public Verbalizer(QueryExecutionFactory qef, String cacheDirectory) {
    	this.qef = qef;

		if(cacheDirectory == null) {
			cacheDirectory = DEFAULT_CACHE_BASE_DIR;
		}
		cacheDirectory = new File(cacheDirectory, "avatar-cache/sparql").getAbsolutePath();
    	
        nlg = new SimpleNLGwithPostprocessing(qef, cacheDirectory, null);
        labels = new HashMap<>();
        litFilter = new NumericLiteralFilter(qef, cacheDirectory);
        realiser = nlg.realiser;

        pr = new PredicateMergeRule(nlg.lexicon, nlg.nlgFactory, nlg.realiser);
        or = new ObjectMergeRule(nlg.lexicon, nlg.nlgFactory, nlg.realiser);
        sr = new SubjectMergeRule(nlg.lexicon, nlg.nlgFactory, nlg.realiser);

        gender = new TypeAwareGenderDetector(qef, new LexiconBasedGenderDetector());

        graphGenerator = new CachedDatasetBasedGraphGenerator(qef, cacheDirectory);
    }
    
    public Verbalizer(SparqlEndpoint endpoint, String cacheDirectory) {
    	this(new QueryExecutionFactoryHttp(endpoint.getURL().toString(), endpoint.getDefaultGraphURIs()), cacheDirectory);
    }

	public Verbalizer(SparqlEndpoint endpoint) {
		this(endpoint, DEFAULT_CACHE_BASE_DIR);
	}
    
    /**
     * @param blacklist a blacklist of properties that are omitted when building the summary
     */
    public void setPropertiesBlacklist(Set<String> blacklist) {
        graphGenerator.setPropertiesBlacklist(blacklist);
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

    /**
     * Gets all triples for resource r and property p.
     * If outgoing is true it returns all triples with <r,p,o>, else <s,p,r>
     *
     * @param r the resource 
     * @param p the property 
     * @param outgoing whether to get outgoing or ingoing triples
     * @return A set of triples
     */
    public Set<Triple> getTriples(Resource r, Property p, boolean outgoing) {
        Set<Triple> result = new HashSet<>();
        try {
        	String q;
        	if(outgoing){
        		q = "SELECT ?o where { <" + r.getURI() + "> <" + p.getURI() + "> ?o.}";
        	} else {
        		q = "SELECT ?o where { ?o <" + p.getURI() + "> <" + r.getURI() + ">.}";
        	}
        	q += " LIMIT " + maxShownValuesPerProperty+1;
            QueryExecution qe = qef.createQueryExecution(q);
            ResultSet results = qe.execSelect();
            if (results.hasNext()) {
                while (results.hasNext()) {
                    RDFNode n = results.next().get("o");
                    result.add(Triple.create(r.asNode(), p.asNode(), n.asNode()));
                }
            }
            qe.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
    
	public Set<Node> getSummaryProperties(OWLClass cls, double threshold,
			String namespace,
			DatasetBasedGraphGenerator.Cooccurrence cooccurrence) {
		Set<Node> properties = new HashSet<>();
		WeightedGraph wg;
		try {
			wg = graphGenerator.generateGraph(cls, threshold, namespace,
					cooccurrence);
			return wg.getNodes().keySet();
		} catch (NoGraphAvailableException e) {
			logger.error(e.getMessage());
		}
		return null;
	}

    /**
     * Generates the string representation of a verbalization
     *
     * @param properties List of property clusters to be used for verbalization
     * @param resource Resource to summarize
     * @return Textual representation
     */
    public String realize(List<Set<Node>> properties, Resource resource, OWLClass nc) {
        List<NLGElement> elts = generateSentencesFromClusters(properties, resource, nc);
        return realize(elts);
    }

    public String realize(List<NLGElement> elts) {
        if(elts.isEmpty()) return null;
        String realization = "";
        for (NLGElement elt : elts) {
            realization = realization + realiser.realiseSentence(elt) + " ";
        }
        return realization.substring(0, realization.length() - 1);
    }

    public List<NLGElement> generateSentencesFromClusters(List<Set<Node>> clusters,
            Resource resource, OWLClass OWLClass) {
        return generateSentencesFromClusters(clusters, resource, OWLClass, false);
    }

    /**
     * Takes the output of the clustering for a given class and a resource.
     * Returns the verbalization for the resource
     *
     * @param clusters Output of the clustering
     * @param resource Resource to summarize
     * @return List of NLGElement
     */
    public List<NLGElement> generateSentencesFromClusters(List<Set<Node>> clusters,
            Resource resource, OWLClass namedClass, boolean replaceSubjects) {
        List<SPhraseSpec> buffer;

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
            Set<Triple> triples = new HashSet<>();
            buffer = new ArrayList<>();
            for (Node property : propertySet) {
                triples = getTriples(resource, ResourceFactory.createProperty(property.label), property.outgoing);
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
        List<SPhraseSpec> phrases = new ArrayList<>();
        SPhraseSpec phrase;
        for (Triple t : triples) {
            phrase = generateSimplePhraseFromTriple(t, outgoing);
			phrases.add(phrase);
        }
        return phrases;
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
    public SPhraseSpec generateSimplePhraseFromTriple(Triple triple) {
        return nlg.getNLForTriple(triple);
    }
    
    /**
     * Generates a simple phrase for a triple
     *
     * @param triple A triple
     * @return A simple phrase
     */
    public SPhraseSpec generateSimplePhraseFromTriple(Triple triple, boolean outgoing) {
        return nlg.getNLForTriple(triple, outgoing);
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
			    //finally generateSentencesFromClusters
			    List<NLGElement> result = generateSentencesFromClusters(sortedPropertyClusters, ResourceFactory.createResource(ind.toStringID()), nc, true);

			    Triple t = Triple.create(ResourceFactory.createResource(ind.toStringID()).asNode(), ResourceFactory.createProperty(RDF.type.getURI()).asNode(),
			            ResourceFactory.createResource(nc.toStringID()).asNode());
			    Collections.reverse(result);
			    result.add(generateSimplePhraseFromTriple(t));
			    Collections.reverse(result);

			    verbalizations.put(ind, result);

			    resource2Triples.get(ResourceFactory.createResource(ind.toStringID())).add(t);
			}

			return verbalizations;
		} catch (NoGraphAvailableException e) {
			e.printStackTrace();
		}
        
        return null;
    }
    
    /**
     * Returns a textual summary of the given entity.
     *
     * @return
     */
    public String summarize(OWLIndividual individual) {
    	//compute the most specific type first
    	OWLClass cls = getMostSpecificType(individual);
    	
        return summarize(individual, cls);
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
        List<NLGElement> elements = verbalize(individual, nc, null, threshold, cooccurrence, hType);
        String summary = realize(elements);
        summary = summary.replaceAll("\\s?\\((.*?)\\)", "");
        summary = summary.replace(" , among others,", ", among others,");
        return summary;
    }

    /**
     * Returns a textual summary of the given entity.
     *
     * @return
     */
    public Map<OWLIndividual, String> getSummaries(Set<OWLIndividual> individuals, OWLClass nc, String namespace, double threshold, Cooccurrence cooccurrence, HardeningType hType) {
        Map<OWLIndividual, String> entity2Summaries = new HashMap<>();

        Map<OWLIndividual, List<NLGElement>> verbalize = verbalize(individuals, nc, namespace, threshold, cooccurrence, hType);
        for (Entry<OWLIndividual, List<NLGElement>> entry : verbalize.entrySet()) {
        	OWLIndividual individual = entry.getKey();
            List<NLGElement> elements = entry.getValue();
            String summary = realize(elements);
            summary = summary.replaceAll("\\s?\\((.*?)\\)", "");
            summary = summary.replace(" , among others,", ", among others,");
            entity2Summaries.put(individual, summary);
        }

        return entity2Summaries;
    }
    
    /**
     * Returns the most specific type of a given individual.
     * @param ind
     * @return
     */
    private OWLClass getMostSpecificType(OWLIndividual ind){
    	logger.debug("Getting the most specific type of " + ind);
    	String query = String.format("select distinct ?type where {"
    			+ " <%s> a ?type ."
//    			+ "?type a owl:Class ." // too strict, thus currently omitted
    			+ "filter not exists {?subtype ^a <%s> ; rdfs:subClassOf ?type .filter(?subtype != ?type)}}",
    			ind.toStringID(), ind.toStringID());
		SortedSet<OWLClass> types = new TreeSet<>();
    	
    	QueryExecution qe = qef.createQueryExecution(query);
    	ResultSet rs = qe.execSelect();
    	while (rs.hasNext()) {
			QuerySolution qs = rs.next();
			if(qs.get("type").isURIResource()){
				types.add(new OWLClassImpl(IRI.create(qs.getResource("type").getURI())));
			}
		}
    	qe.close();
    	
    	//of more than one type exists, we have to choose one
    	//TODO
    	
    	return types.first();
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
        //the textual representation of the resource itself
        result.add(nlg.getNPPhrase(resource.getURI(), false, false));
        //the class, e.g. 'this book'
        NPPhraseSpec np = nlg.getNPPhrase(resourceType.toStringID(), false);
        np.addPreModifier("This");
        result.add(np);
        //the pronoun depending on the gender of the resource
        if (resourceGender.equals(Gender.MALE)) {
            result.add(nlg.nlgFactory.createNounPhrase("he"));
        } else if (resourceGender.equals(Gender.FEMALE)) {
            result.add(nlg.nlgFactory.createNounPhrase("she"));
        } else {
            result.add(nlg.nlgFactory.createNounPhrase("it"));
        }
        return result;
    }
    
    /**
     * Returns the gender of the given resource.
     * @param resource
     * @return the gender
     */
    public Gender getGender(Resource resource){
    	//get a textual representation of the resource
    	String label = realiser.realiseSentence(nlg.getNPPhrase(resource.getURI(), false, false));
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
            NPPhraseSpec newSubject = nlg.nlgFactory.createNounPhrase(((NPPhraseSpec) currentSubject).getHead());
            
            NPPhraseSpec newSpecifier = nlg.nlgFactory.createNounPhrase(subjects.get(index));
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
    
    public static void main(String args[]) throws IOException {
    	OptionParser parser = new OptionParser() {
			{
				acceptsAll(Lists.newArrayList("e", "endpoint"), "SPARQL endpoint URL to be used.").withRequiredArg().ofType(URL.class).required();
				acceptsAll(Lists.newArrayList("g", "graph"), "URI of default graph for queries on SPARQL endpoint.").withRequiredArg().ofType(String.class);
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

		parser.printHelpOn(System.out);

		// parse options and display a message for the user in case of problems
		OptionSet options = null;
		try {
			options = parser.parse(args);
		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage() + ". Use -? to get help.");
			System.exit(0);
		}
		
		// print help screen
		if (options.has("?")) {
			parser.printHelpOn(System.out);
		} else {

		}
		URL endpointURL = null;
		try {
			endpointURL = (URL) options.valueOf("endpoint");
		} catch(OptionException e) {
			System.out.println("The specified endpoint appears not be a proper URL.");
			System.exit(0);
		}
		String defaultGraphURI = null;
		if(options.has("g")){
			try {
				defaultGraphURI = (String) options.valueOf("graph");
				URI.create(defaultGraphURI);
			} catch(OptionException e) {
				System.out.println("The specified graph appears not be a proper URI.");
				System.exit(0);
			}
		}
		SparqlEndpoint endpoint = new SparqlEndpoint(endpointURL, defaultGraphURI);

        String cacheDirectory = (String) options.valueOf("cache");

        Verbalizer v = new Verbalizer(endpoint, cacheDirectory);
        
        OWLIndividual ind = new OWLNamedIndividualImpl(IRI.create(options.valueOf("i").toString()));

		OWLClass cls = new OWLClassImpl(IRI.create((URI)options.valueOf("c")));

		// optionally, set the person types
		if(options.has("p")) {
			List<String> personTypes = Splitter.on(',').trimResults().omitEmptyStrings().splitToList((String) options.valueOf("p"));
			v.setPersonTypes(new HashSet(personTypes));
		}

		String summary = v.summarize(ind, cls);

        summary = summary.replaceAll("\\s?\\((.*?)\\)", "");
        summary = summary.replace(" , among others,", ", among others,");

        System.out.println(summary);
    }
}
