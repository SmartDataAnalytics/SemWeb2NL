/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.assessment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aksw.avatar.Verbalizer;
import org.aksw.avatar.clustering.BorderFlowX;
import org.aksw.avatar.clustering.Node;
import org.aksw.avatar.clustering.WeightedGraph;
import org.aksw.avatar.clustering.hardening.HardeningFactory;
import org.aksw.avatar.dataset.DatasetBasedGraphGenerator;
import org.aksw.avatar.exceptions.NoGraphAvailableException;
import org.aksw.avatar.gender.Gender;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.apache.log4j.Logger;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLIndividual;

import simplenlg.features.Feature;
import simplenlg.features.InternalFeature;
import simplenlg.framework.CoordinatedPhraseElement;
import simplenlg.framework.NLGElement;
import simplenlg.phrasespec.NPPhraseSpec;
import simplenlg.phrasespec.SPhraseSpec;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

/**
 * Extension of Avatar for verbalizing jeopardy questions.
 * @author ngonga
 */
public class JeopardyVerbalizer extends Verbalizer {
	
	private static final Logger logger = Logger.getLogger(JeopardyVerbalizer.class.getName());
    
	public JeopardyVerbalizer(SparqlEndpoint endpoint, String cacheDirectory, String wordnetDirectory) {
		super(endpoint, cacheDirectory, wordnetDirectory);
	}
    
	public JeopardyVerbalizer(QueryExecutionFactory qef, String cacheDirectory, String wordnetDirectory) {
		super(qef, cacheDirectory, wordnetDirectory);
	}
    
     public Map<OWLIndividual, List<NLGElement>> verbalize(Set<OWLIndividual> individuals, OWLClass nc, double threshold, DatasetBasedGraphGenerator.Cooccurrence cooccurrence, HardeningFactory.HardeningType hType) {
        resource2Triples = new HashMap<>();
        
        // first get graph for class
        try {
			WeightedGraph wg = graphGenerator.generateGraph(nc, threshold, "http://dbpedia.org/ontology/", cooccurrence);

			//then cluster the graph
			BorderFlowX bf = new BorderFlowX(wg);
			Set<Set<Node>> clusters = bf.cluster();
			//then harden the results
			List<Set<Node>> sortedPropertyClusters = HardeningFactory.getHardening(hType).harden(clusters, wg);
			logger.info("Clusters:");
			for (Set<Node> cluster : sortedPropertyClusters) {
				logger.info(cluster);
			}

			Map<OWLIndividual, List<NLGElement>> verbalizations = new HashMap<>();

			for (OWLIndividual ind : individuals) {
			    //finally generateSentencesFromClusters
			    List<NLGElement> result = generateSentencesFromClusters(sortedPropertyClusters, ResourceFactory.createResource(ind.toStringID()), nc, true);
//            Triple t = Triple.create(ResourceFactory.createResource(ind.getName()).asNode(), ResourceFactory.createProperty(RDF.TYPE.toString()).asNode(),
//                    ResourceFactory.createResource(nc.getName()).asNode());
//            result = Lists.reverse(result);
//            result.add(generateSimplePhraseFromTriple(t));
//            result = Lists.reverse(result);            
			    verbalizations.put(ind, result);
//
//            resource2Triples.get(ResourceFactory.createResource(ind.getName())).add(t);
			}

			return verbalizations;
		} catch (NoGraphAvailableException e) {
			e.printStackTrace();
		}
        return null;
    }
     
     
    @Override
    public List<NPPhraseSpec> generateSubjects(Resource resource, OWLClass nc, Gender g) {
        List<NPPhraseSpec> result = new ArrayList<>();
        NPPhraseSpec np = nlg.getNPPhrase(nc.toStringID(), false);
        np.addPreModifier("This");
        result.add(np);
        if (g.equals(Gender.MALE)) {
            result.add(nlg.nlgFactory.createNounPhrase("he"));
        } else if (g.equals(Gender.FEMALE)) {
            result.add(nlg.nlgFactory.createNounPhrase("she"));
        } else {
            result.add(nlg.nlgFactory.createNounPhrase("it"));
        }
        return result;
    }
    
    @Override
    protected NLGElement replaceSubject(NLGElement phrase, List<NPPhraseSpec> subjects, Gender g) {
        SPhraseSpec sphrase;
        if (phrase instanceof SPhraseSpec) {
            sphrase = (SPhraseSpec) phrase;
        } else if (phrase instanceof CoordinatedPhraseElement) {
            sphrase = (SPhraseSpec) ((CoordinatedPhraseElement) phrase).getChildren().get(0);
        } else {
            return phrase;
        }
        int index = (int) Math.floor(Math.random() * subjects.size());
//        index = 2;
        
        boolean isPossessive = isPossessive(sphrase);
        
        if (isPossessive) //possessive subject
        {

            NPPhraseSpec subject = nlg.nlgFactory.createNounPhrase(((NPPhraseSpec) sphrase.getSubject()).getHead());
            NPPhraseSpec modifier;
            if (index < subjects.size() - 1) {
                modifier = nlg.nlgFactory.createNounPhrase(subjects.get(index));
                modifier.setFeature(Feature.POSSESSIVE, true);
                subject.setPreModifier(modifier);
                modifier.setFeature(Feature.POSSESSIVE, true);
            } else {
                if (g.equals(Gender.MALE)) {
                    subject.setPreModifier("his");
                } else if (g.equals(Gender.FEMALE)) {
                    subject.setPreModifier("her");
                } else {
                    subject.setPreModifier("its");
                }
            }
            if (sphrase.getSubject().isPlural()) {

//                subject.getSpecifier().setPlural(false);
                subject.setPlural(true);
            }
            sphrase.setSubject(subject);

        } else {
// does not fully work due to bug in SimpleNLG code      
            if (g.equals(Gender.MALE)) {
                sphrase.setSubject("He");
            } else if (g.equals(Gender.FEMALE)) {
                sphrase.setSubject("She");
            } else {
                sphrase.setSubject("It");
            }
        }
        return phrase;
    }

	/**
	 * @param sphrase
	 * @return
	 */
	private boolean isPossessive(SPhraseSpec phrase) {
		NLGElement subject = phrase.getSubject();
		NLGElement specifier = subject.getFeatureAsElement(InternalFeature.SPECIFIER);
		if(specifier != null) {
			return specifier.getFeatureAsBoolean(Feature.POSSESSIVE);
		}
		return false;
	}
    
}
