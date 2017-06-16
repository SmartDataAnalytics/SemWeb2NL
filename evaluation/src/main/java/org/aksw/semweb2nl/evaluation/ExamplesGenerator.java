/*
 * #%L
 * Evaluation
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
package org.aksw.semweb2nl.evaluation;

import java.util.*;
import java.util.Map.Entry;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.dlsyntax.renderer.DLSyntaxObjectRenderer;
import org.semanticweb.owlapi.io.ToStringRenderer;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLIndividualAxiom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import org.semanticweb.owlapi.search.EntitySearcher;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

/**
 * @author Lorenz Buehmann
 *
 */
public class ExamplesGenerator {
	
	private OWLOntology ontology;
	private OWLDataFactory df = new OWLDataFactoryImpl();
	private IndividualGenerator individualGenerator = new IndividualGenerator();
	private ABoxGenerator aboxGenerator = new ABoxGenerator(individualGenerator);

	public ExamplesGenerator(OWLOntology ontology) {
		this.ontology = ontology;
	}
	
	/**
	 * Generate some synthetic instances of the given class and the corresponding
	 * Abox data.
	 * @param cls
	 */
	public Map<OWLIndividual, Set<OWLIndividualAxiom>> generatePositiveExamples(OWLClass cls, int nrOfExamples) {
		Map<OWLIndividual, Set<OWLIndividualAxiom>> result = new HashMap<OWLIndividual, Set<OWLIndividualAxiom>>();
		
		individualGenerator.reset();
		
		// get the class expressions that describe the given class
		Collection<OWLClassExpression> superClassExpressions = EntitySearcher.getSuperClasses(cls, ontology);
		superClassExpressions.addAll(EntitySearcher.getEquivalentClasses(cls, ontology));
//		superClassExpressions.addAll(cls.getSubClasses(ontology));

		// rewrite as intersection for simplicity
		Set<OWLClassExpression> conjuncts = new TreeSet<OWLClassExpression>();
		for (OWLClassExpression supExpr : superClassExpressions) { // avoid A and (A and B)
			if(supExpr instanceof OWLObjectIntersectionOf){
				conjuncts.addAll(((OWLObjectIntersectionOf) supExpr).getOperands());
			} else {
				conjuncts.add(supExpr);
			}
		}
		superClassExpressions = Collections.<OWLClassExpression>singleton(df.getOWLObjectIntersectionOf(conjuncts));
		
		// generate ABox data based on contained information in the class expressions
		for (OWLClassExpression supExpr : superClassExpressions) {
			System.out.println(supExpr);
			for (int i = 0; i < nrOfExamples; i++) {
				OWLIndividual ind = individualGenerator.generateIndividual(cls);
				Set<OWLIndividualAxiom> instanceData = aboxGenerator.generateInstanceData(ind, supExpr);
				result.put(ind, instanceData);
//				System.out.println(instanceData);
			}
		}
		
		return result;
	}
	
	public Map<OWLIndividual, Set<OWLIndividualAxiom>> generateNegativeExamples(OWLClass cls, int nrOfExamples) {
		// generate minimal ABox data to be a positive example
		Map<OWLIndividual, Set<OWLIndividualAxiom>> positiveExamples = generatePositiveExamples(cls, nrOfExamples);
		
		// modify the instance data for each individual such that it
		// does not satisfy the description of the class
		for (Entry<OWLIndividual, Set<OWLIndividualAxiom>> entry : positiveExamples.entrySet()) {
			OWLIndividual ind = entry.getKey();
			Set<OWLIndividualAxiom> instanceData = entry.getValue();
			
			// easiest way: remove at least 1 and at most n-1 axioms randomly with probability p
			double probability = 0.5;
			Random rnd = new Random(123);
			for (Iterator<OWLIndividualAxiom> iterator = instanceData.iterator(); iterator.hasNext();) {
				OWLIndividualAxiom axiom = iterator.next();
				if(rnd.nextDouble() < probability) {
					iterator.remove();
				}
			}
		}
		
		return positiveExamples;
	}
	
	public static void main(String[] args) throws Exception {
		ToStringRenderer.getInstance().setRenderer(new DLSyntaxObjectRenderer());
		String ontologyURL = "http://protege.cim3.net/file/pub/ontologies/koala/koala.owl";
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = man.loadOntology(IRI.create(ontologyURL));
		
		ExamplesGenerator examplesGenerator = new ExamplesGenerator(ontology);
		
		for (OWLClass cls : ontology.getClassesInSignature()) {
			System.out.println("########### " + cls + " #############");
			
			Map<OWLIndividual, Set<OWLIndividualAxiom>> positiveExamples = examplesGenerator.generatePositiveExamples(cls, 3);
			for (Entry<OWLIndividual, Set<OWLIndividualAxiom>> entry : positiveExamples.entrySet()) {
				OWLIndividual ind = entry.getKey();
				Set<OWLIndividualAxiom> instanceData = entry.getValue();
				System.out.println("+(" + ind + ")::" + instanceData);
			}
			
			Map<OWLIndividual, Set<OWLIndividualAxiom>> negativeExamples = examplesGenerator.generateNegativeExamples(cls, 3);
			for (Entry<OWLIndividual, Set<OWLIndividualAxiom>> entry : negativeExamples.entrySet()) {
				OWLIndividual ind = entry.getKey();
				Set<OWLIndividualAxiom> instanceData = entry.getValue();
				System.out.println("-(" + ind + "::" + instanceData);
			}
		}
		
	}
}
