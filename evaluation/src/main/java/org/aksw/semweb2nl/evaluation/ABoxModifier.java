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
package org.aksw.semweb2nl.evaluation;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLIndividualAxiom;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.OWLAxiomVisitorAdapter;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;

import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

public class ABoxModifier extends OWLAxiomVisitorAdapter {
	private OWLDataFactory df = new OWLDataFactoryImpl();
	private Set<OWLIndividualAxiom> axioms = new HashSet<OWLIndividualAxiom>();
	// the stack of individuals which are the current subject we are talking about
	private Deque<OWLIndividual> individuals = new ArrayDeque<OWLIndividual>();
	
	private Random rnd = new Random(123);
	private OWLOntology ontology;
	private IndividualGenerator individualGenerator;
	private OWLReasoner reasoner;

	public ABoxModifier(OWLOntology ontology, IndividualGenerator individualGenerator) {
		this.ontology = ontology;
		this.individualGenerator = individualGenerator;
		
		OWLReasonerFactory reasonerFactory = PelletReasonerFactory.getInstance();
		reasoner = reasonerFactory.createNonBufferingReasoner(ontology);
	}

	/**
	 * Modify the given instance data.
	 * Returns a new set of axioms.
	 */
	public Set<OWLIndividualAxiom> modifyInstanceData(Set<OWLIndividualAxiom> axioms) {
		reset();

		for (OWLIndividualAxiom axiom : axioms) {
			if(rnd.nextDouble() <= 0.5) { // first coin: modify or not
				if(rnd.nextDouble() <= 0.5) { // second coin: remove or modify
					axiom.accept(this);
				} else {
					// nothing to do here
				}
			}
		}

		return axioms;
	}

	private void reset() {
		axioms = new HashSet<OWLIndividualAxiom>();
		individuals.clear();
	}
	
	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.util.OWLAxiomVisitorAdapter#visit(org.semanticweb.owlapi.model.OWLClassAssertionAxiom)
	 */
	@Override
	public void visit(OWLClassAssertionAxiom axiom) {
		// A(a) -> add B(a) such that B is not a subclass of A
		OWLClassExpression ce = axiom.getClassExpression();
		
		if(!ce.isAnonymous() && !ce.isOWLThing()){
			// pick random class that is not a subclass of A
			Set<OWLClass> classes = ontology.getClassesInSignature();
			classes.remove(reasoner.getSubClasses(ce.asOWLClass(), false));
			
			OWLClass newClass = classes.iterator().next();
			
			axioms.add(df.getOWLClassAssertionAxiom(newClass, axiom.getIndividual()));
		}
		
	}
	
	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.util.OWLAxiomVisitorAdapter#visit(org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom)
	 */
	@Override
	public void visit(OWLDataPropertyAssertionAxiom axiom) {
		super.visit(axiom);
	}
	
	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.util.OWLAxiomVisitorAdapter#visit(org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom)
	 */
	@Override
	public void visit(OWLObjectPropertyAssertionAxiom axiom) {
		// A(a) -> add B(a) such that B is not a subclass of A

		OWLObjectPropertyExpression pe = axiom.getProperty();

		if (!pe.isAnonymous() && !pe.isOWLTopObjectProperty()) {
			// pick random class that is not a subclass of A
			Set<OWLObjectProperty> properties = ontology.getObjectPropertiesInSignature();
			properties.remove(reasoner.getSubObjectProperties(pe, false));

			OWLObjectProperty newProperty = properties.iterator().next();

			axioms.add(df.getOWLObjectPropertyAssertionAxiom(newProperty, axiom.getSubject(), axiom.getObject()));
		}
	}

	
}